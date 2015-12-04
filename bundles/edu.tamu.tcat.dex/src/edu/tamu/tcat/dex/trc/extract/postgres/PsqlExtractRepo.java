package edu.tamu.tcat.dex.trc.extract.postgres;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
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
import edu.tamu.tcat.dex.trc.extract.dto.ExtractDTO;
import edu.tamu.tcat.trc.entries.notification.BaseUpdateEvent;
import edu.tamu.tcat.trc.entries.notification.DataUpdateObserver;
import edu.tamu.tcat.trc.entries.notification.DataUpdateObserverAdapter;
import edu.tamu.tcat.trc.entries.notification.ObservableTaskWrapper;
import edu.tamu.tcat.trc.entries.notification.UpdateEvent;
import edu.tamu.tcat.trc.entries.notification.UpdateEvent.UpdateAction;
import edu.tamu.tcat.trc.entries.notification.UpdateListener;

public class PsqlExtractRepo implements ExtractRepository
{
   private static final Logger logger = Logger.getLogger(PsqlExtractRepo.class.getName());

   private static final String SELECT_EXTRACT_SQL = "SELECT extract FROM extracts WHERE id = ? AND active = TRUE";

   // HACK: not sure of the best way to do this
   private static final String SELECT_EXTRACT_BY_MANUSCRIPT_SQL = "SELECT id FROM extracts WHERE extract->'manuscript'->>'id' = ? AND active = TRUE";

   private static final String UPDATE_EXTRACT_SQL = "UPDATE extracts SET extract = ? WHERE id = ?";
   private static final String CREATE_EXTRACT_SQL = "INSERT INTO extracts (extract, id) VALUES (?, ?)";
   private static final String DELETE_EXTRACT_SQL = "UPDATE extracts SET active = FALSE WHERE id = ?";

   private static final String SQL_SELECT_COLUMN_EXTRACT = "extract";
   private static final String SQL_SELECT_COLUMN_ID = "id";

   private static final int SQL_SELECT_PARAM_ID = 1;
   private static final int SQL_SELECT_BY_MANUSCRIPT_PARAM_ID = 1;
   private static final int SQL_UPDATE_PARAM_EXTRACT = 1;
   private static final int SQL_UPDATE_PARAM_ID = 2;
   private static final int SQL_DELETE_PARAM_ID = 1;

   // HACK: hard-coded actor UUID for Extract Repository
   private static final UUID EXTRACT_REPO_ACTOR_ID = UUID.fromString("deadbeef-cafe-f00d-c0de-decaffc0ffee");

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
      eventFactory = new BaseUpdateEventFactory(EXTRACT_REPO_ACTOR_ID);
      listeners = new EntryUpdateHelper<>(4);
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

   @Override
   public boolean exists(String id) throws DramaticExtractException
   {
      ExecutorTask<Boolean> task = (conn) ->
      {
         try (PreparedStatement ps = conn.prepareStatement(SELECT_EXTRACT_SQL))
         {
            ps.setString(SQL_SELECT_PARAM_ID, id);

            try (ResultSet rs = ps.executeQuery())
            {
               return Boolean.valueOf(rs.next());
            }
         }
      };

      Future<Boolean> existsFuture = executor.submit(task);

      try {
         return existsFuture.get().booleanValue();
      }
      catch (InterruptedException | ExecutionException e) {
         throw new DramaticExtractException("Encountered error while fetching boolean existence value", e);
      }
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
         UpdateEvent evt = eventFactory.makeCreateEvent(id);
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
         UpdateEvent evt = eventFactory.makeUpdateEvent(id);
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
   public EditExtractCommand createOrEdit(String id) throws DramaticExtractException
   {
      // HACK: not stable since an extract with a duplicate ID could be created and saved between the exists() check and when execute() is called on the command returned by create()
      //
      // This should be addressed at SQL execution with an UPSERT, which also eliminates the need for separate create() and edit() methods.
      //
      // In PostgreSQL 9.4, an UPSERT looks like:
      //
      //    WITH upsert AS (
      //       UPDATE table SET field1=val1, field2=val2, ... WHERE id='dupIdent'               -- UPDATE command
      //       RETURNING *
      //    )
      //    INSERT INTO table (id, field1, field2, ...) SELECT 'dupIdent', val1, val2, ...      -- INSERT command with "VALUES (...)" replaced by "SELECT ..."
      //    WHERE NOT EXISTS (SELECT * FROM upsert);
      //
      //    ref: http://www.the-art-of-web.com/sql/upsert/
      //
      // Native support for INSERT statement will be available in PostgreSQL 9.5:
      //
      //    INSERT INTO table (id, field1, field2, ...) VALUES ('dupIdent', val1, val2, ...)
      //    ON CONFLICT (id) DO UPDATE SET field1=EXCLUDED.val1, field2=EXCLUDED.val2, ...;
      //
      //    ref: http://www.postgresql.org/docs/devel/static/sql-insert.html

      try
      {
         return exists(id) ? edit(id) : create(id);
      }
      catch (ExtractNotAvailableException e)
      {
         throw new IllegalStateException("I was told extract [" + id + "] existed, but was unable to edit it", e);
      }
   }

   @Override
   public void remove(String id) throws DramaticExtractException
   {
      UpdateEvent evt = eventFactory.makeDeleteEvent(id);
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
   public void removebyManuscriptId(String manuscriptId) throws DramaticExtractException
   {
      ExecutorTask<Integer> task = makeDeleteByManuscriptIdTask(manuscriptId);
      executor.submit(task);
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

   private ExecutorTask<Integer> makeDeleteByManuscriptIdTask(String manuscriptId)
   {
      return (conn) ->
      {
         try (PreparedStatement ps = conn.prepareCall(SELECT_EXTRACT_BY_MANUSCRIPT_SQL))
         {
            ps.setString(SQL_SELECT_BY_MANUSCRIPT_PARAM_ID, manuscriptId);

            Integer count = Integer.valueOf(0);
            try (ResultSet rs = ps.executeQuery())
            {
               while (rs.next())
               {
                  String id = rs.getString(SQL_SELECT_COLUMN_ID);
                  try
                  {
                     // delegate to remove() for event handling
                     remove(id);
                     count++;
                  }
                  catch (DramaticExtractException e)
                  {
                     logger.log(Level.SEVERE, "Unable to remove extract [" + id + "] for manuscript [" + manuscriptId + "].", e);
                  }
               }
            }

            return count;
         }
         catch (SQLException e)
         {
            throw new IllegalStateException("Failed to find extracts by manuscript ID [" + manuscriptId + "]", e);
         }
      };
   }

   /**
    * Provides a common utility for creating {@link UpdateEvent}s
    *
    * @todo This class may be refactored into edu.tamu.tcat.trc.entries.notification
    *
    * @param <T> The type of object that will be referenced in the constructed {@link UpdateEvent} instances
    */
   public static class BaseUpdateEventFactory
   {
      private final UUID actor;

      public BaseUpdateEventFactory(UUID actor)
      {
         this.actor = actor;
      }

      /**
       * Creates an update event to notify listeners of an object's creation.
       *
       * @param id
       * @return
       */
      public UpdateEvent makeCreateEvent(String id)
      {
         return new BaseUpdateEvent(id, UpdateAction.CREATE, actor, Instant.now());
      }

      /**
       * Creates an update event to notify listeners of an object's modification.
       *
       * @param id
       * @return
       */
      public UpdateEvent makeUpdateEvent(String id)
      {
         return new BaseUpdateEvent(id, UpdateAction.UPDATE, actor, Instant.now());
      }

      /**
       * Creates an update event to notify listeners of an object's deletion.
       *
       * @param id
       * @return
       */
      public UpdateEvent makeDeleteEvent(String id)
      {
         return new BaseUpdateEvent(id, UpdateAction.DELETE, actor, Instant.now());
      }
   }
}
