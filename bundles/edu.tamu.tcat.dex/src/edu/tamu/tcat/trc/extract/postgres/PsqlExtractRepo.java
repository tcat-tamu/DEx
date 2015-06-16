package edu.tamu.tcat.trc.extract.postgres;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.postgresql.util.PGobject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.tamu.tcat.db.exec.sql.SqlExecutor;
import edu.tamu.tcat.db.exec.sql.SqlExecutor.ExecutorTask;
import edu.tamu.tcat.dex.trc.entry.DramaticExtract;
import edu.tamu.tcat.dex.trc.entry.DramaticExtractException;
import edu.tamu.tcat.dex.trc.entry.EditExtractCommand;
import edu.tamu.tcat.dex.trc.entry.ExtractNotAvailableException;
import edu.tamu.tcat.dex.trc.entry.ExtractRepository;
import edu.tamu.tcat.trc.entries.notification.DataUpdateObserver;
import edu.tamu.tcat.trc.entries.notification.DataUpdateObserverAdapter;
import edu.tamu.tcat.trc.entries.notification.EntryUpdateHelper;
import edu.tamu.tcat.trc.entries.notification.ObservableTaskWrapper;
import edu.tamu.tcat.trc.entries.notification.UpdateEvent;
import edu.tamu.tcat.trc.entries.notification.UpdateListener;
import edu.tamu.tcat.trc.extract.dto.ExtractDTO;

public class PsqlExtractRepo implements ExtractRepository
{
   private static final Logger logger = Logger.getLogger(PsqlExtractRepo.class.getName());

   private static final String SELECT_EXTRACT_SQL = "SELECT extract FROM extracts WHERE id = ?";
   private static final String UPDATE_EXTRACT_SQL = "UPDATE extracts SET extract = ? WHERE id = ?";
   private static final String CREATE_EXTRACT_SQL = "INSERT INTO extracts (extract, id) VALUES (?, ?)";
   private static final String DELETE_EXTRACT_SQL = "UPDATE extracts SET active = FALSE WHERE id = ?";

   private static final String SQL_SELECT_COLUMN_EXTRACT = "extract";

   private static final int SQL_SELECT_PARAM_ID = 1;
   private static final int SQL_UPDATE_PARAM_EXTRACT = 1;
   private static final int SQL_UPDATE_PARAM_ID = 2;
   private static final int SQL_DELETE_PARAM_ID = 1;

   private SqlExecutor executor;
   private EntryUpdateHelper<UpdateEvent> listeners;
   private ObjectMapper mapper;
   private BaseUpdateEventFactory eventFactory;



   /**
    * DI method called by owning object -- most likely OSGi declarative service layer
    *
    * @param executor the SQL executor service
    */
   public void setSqlExecutor(SqlExecutor executor)
   {
      this.executor = executor;
   }

   public void activate()
   {
      Objects.requireNonNull(executor, "No SQL Executor provided");
      eventFactory = new BaseUpdateEventFactory();
      listeners = new EntryUpdateHelper<>();
      mapper = new ObjectMapper();
   }

   public void dispose()
   {
      if (listeners != null)
      {
         listeners.close();
      }
      eventFactory = null;
      executor = null;
      mapper = null;
      listeners = null;
   }


   @Override
   public DramaticExtract get(String id) throws ExtractNotAvailableException, DramaticExtractException
   {
      ExtractDTO dto = getDTO(id);
      return ExtractDTO.instantiate(dto);
   }

   private ExtractDTO getDTO(String id) throws DramaticExtractException
   {
      Future<ExtractDTO> extractFuture = executor.submit(makeSelectTask(id));
      try
      {
         return extractFuture.get();
      }
      catch (InterruptedException | ExecutionException e) {
         throw new DramaticExtractException("Unable to get extract [" + id + "]", e);
      }
   }

   @Override
   public EditExtractCommand create(String id) throws DramaticExtractException
   {
      ExtractDTO extractDTO = new ExtractDTO();
      extractDTO.id = id;

      EditExtractCommandImpl command = new EditExtractCommandImpl(extractDTO);
      command.setCommitHook(dto ->
      {
         // TODO: supply actor UUID to makeCreateEvent
         UpdateEvent evt = eventFactory.makeCreateEvent(id, null);
         ExecutorTask<String> task = makeUpdateTask(dto, CREATE_EXTRACT_SQL);
         DataUpdateObserver<String> observer = new DataUpdateObserverAdapter<String>()
         {
            @Override
            protected void onFinish(String id)
            {
               if (id != null)
               {
                  listeners.after(evt);
               }
            }
         };
         ObservableTaskWrapper<String> observableTask = new ObservableTaskWrapper<>(task, observer);
         return executor.submit(observableTask);
      });
      return command;
   }

   @Override
   public EditExtractCommand edit(String id) throws ExtractNotAvailableException, DramaticExtractException
   {
      ExtractDTO originalDTO = getDTO(id);
      EditExtractCommandImpl command = new EditExtractCommandImpl(originalDTO);
      command.setCommitHook(updatedDTO ->
      {
         // TODO: supply actor UUID to makeUpdateEvent
         UpdateEvent evt = eventFactory.makeUpdateEvent(id, null);
         ExecutorTask<String> task = makeUpdateTask(updatedDTO, UPDATE_EXTRACT_SQL);
         DataUpdateObserver<String> observer = new DataUpdateObserverAdapter<String>()
         {
            @Override
            protected void onFinish(String id)
            {
               if (id != null)
               {
                  listeners.after(evt);
               }
            }
         };
         ObservableTaskWrapper<String> observableTask = new ObservableTaskWrapper<>(task, observer);
         return executor.submit(observableTask);
      });
      return command;
   }

   @Override
   public void remove(String id) throws DramaticExtractException
   {
      // TODO: supply actor UUID to makeDeleteEvent
      UpdateEvent evt = eventFactory.makeDeleteEvent(id, null);
      ExecutorTask<Boolean> task = makeDeleteTask(id);
      DataUpdateObserver<Boolean> observer = new DataUpdateObserverAdapter<Boolean>()
      {
         @Override
         protected void onFinish(Boolean result)
         {
            if (result.booleanValue())
            {
               listeners.after(evt);
            }
         }
      };
      ObservableTaskWrapper<Boolean> observableTask = new ObservableTaskWrapper<Boolean>(task, observer);
      executor.submit(observableTask);
   }

   @Override
   public AutoCloseable register(UpdateListener<UpdateEvent> ears)
   {
      return listeners.register(ears);
   }

   /**
    * Creates an executor task to find an extract by ID
    *
    * @param id The ID of the extract to find
    * @return An executor task that resolves to the extract
    */
   private ExecutorTask<ExtractDTO> makeSelectTask(String id)
   {
      return (conn) ->
      {
         try (PreparedStatement ps = conn.prepareStatement(SELECT_EXTRACT_SQL))
         {
            ps.setString(SQL_SELECT_PARAM_ID, id);

            try (ResultSet rs = ps.executeQuery())
            {
               if (!rs.next())
               {
                  throw new ExtractNotAvailableException("Unable to find record for extract [" + id + "]");
               }

               PGobject pgo = (PGobject)rs.getObject(SQL_SELECT_COLUMN_EXTRACT);
               String json = pgo.toString();

               try
               {
                  return mapper.readValue(json, ExtractDTO.class);
               }
               catch (IOException e)
               {
                  // NOTE: possible data leak. If this exception is propagated to someone who isn't authorized to see this record...
                  throw new IllegalStateException("Cannot parse person from JSON:\n" + json, e);
               }
            }
         }
      };
   }

   /**
    * Creates an executor task to update an extract
    *
    * @param id The ID of the extract to update
    * @param json The JSON-serialized extract
    * @param sql The parameterized SQL create or update statement to execute
    * @return An executor task that resolves to the ID of the extract when saved.
    */
   private ExecutorTask<String> makeUpdateTask(ExtractDTO dto, String sql)
   {
      String json;
      try
      {
         json = mapper.writeValueAsString(dto);
      }
      catch (JsonProcessingException e)
      {
         throw new IllegalStateException("Unable to serialize extract into JSON string.", e);
      }

      return (conn) ->
      {
         try (PreparedStatement ps = conn.prepareStatement(sql))
         {
            ps.setString(SQL_UPDATE_PARAM_ID, dto.id);

            PGobject jsonObject = new PGobject();
            jsonObject.setType("json");
            jsonObject.setValue(json);
            ps.setObject(SQL_UPDATE_PARAM_EXTRACT, jsonObject);

            int ct = ps.executeUpdate();
            if (ct != 1)
            {
               throw new IllegalStateException("Failed to update extract. Unexpected number of rows updated [" + ct + "]");
            }

            return dto.id;
         }
         catch (SQLException e)
         {
            throw new IllegalStateException("Failed to update extract: [" + dto.id + "]\n" + json, e);
         }
      };
   }

   /**
    * Creates an executor task to delete an extract
    *
    * @param id The ID of the extract to delete
    * @return An executor task that resolves to the ID of the extract when removed.
    */
   private ExecutorTask<Boolean> makeDeleteTask(String id)
   {
      return (conn) ->
      {
         try (PreparedStatement ps = conn.prepareCall(DELETE_EXTRACT_SQL))
         {
            ps.setString(SQL_DELETE_PARAM_ID, id);

            int ct = ps.executeUpdate();
            if (ct != 1)
            {
               logger.log(Level.WARNING, "Failed to deactivate extract [" + id + "]. Unexpected number of rows updated [" + ct + "]");
               return Boolean.valueOf(false);
            }

            return Boolean.valueOf(true);
         }
         catch (SQLException e)
         {
            throw new IllegalStateException("Failed to deactivate extract [" + id + "]", e);
         }
      };
   }
}
