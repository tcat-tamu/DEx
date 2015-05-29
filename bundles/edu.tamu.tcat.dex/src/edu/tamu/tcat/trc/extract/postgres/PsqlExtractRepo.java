package edu.tamu.tcat.trc.extract.postgres;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
import edu.tamu.tcat.dex.trc.entry.ExtractsChangeEvent.ChangeType;
import edu.tamu.tcat.trc.entries.notification.UpdateListener;
import edu.tamu.tcat.trc.extract.dto.ExtractDTO;

public class PsqlExtractRepo implements ExtractRepository
{
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

   // TODO: should this be passed in as a service?
   private final ObjectMapper mapper = new ObjectMapper();


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
   }

   public void dispose()
   {
   }


   @Override
   public DramaticExtract get(String id) throws ExtractNotAvailableException, DramaticExtractException
   {
      Future<ExtractDTO> extractFuture = executor.submit(makeSelectTask(id));
      try {
         ExtractDTO dto = extractFuture.get();
         return ExtractDTO.instantiate(dto);
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
      command.setCommitHook(dto -> updateExtract(dto, ChangeType.CREATED));
      return command;
   }

   @Override
   public EditExtractCommand edit(String id) throws ExtractNotAvailableException, DramaticExtractException
   {

      Future<ExtractDTO> extractFuture = executor.submit(makeSelectTask(id));
      try {
         ExtractDTO extractDTO = extractFuture.get();
         EditExtractCommandImpl command = new EditExtractCommandImpl(extractDTO);
         command.setCommitHook(dto -> updateExtract(dto, ChangeType.MODIFIED));
         return command;
      }
      catch (InterruptedException | ExecutionException e) {
         throw new DramaticExtractException("Unable to get extract [" + id + "]", e);
      }
   }

   @Override
   public void remove(String id) throws DramaticExtractException
   {
      ExecutorTask<String> task = makeDeleteTask(id);
      executor.submit(task);
   }

   @Override
   public AutoCloseable register(UpdateListener<DramaticExtract> ears)
   {
      throw new UnsupportedOperationException();
   }


   /**
    * Creates or updates the dramatic extract in the database.
    *
    * @param dto The extract to persist
    * @param changeType The type of change to perform
    * @return A future that resolves to the ID of the saved extract
    * @throws DramaticExtractException
    */
   private Future<String> updateExtract(ExtractDTO dto, ChangeType changeType)
   {
      String sql = getUpdateSql(changeType);

      String json;
      try {
         json = mapper.writeValueAsString(dto);
      }
      catch (JsonProcessingException e) {
         throw new IllegalStateException("Unable to serialize extract into JSON string.", e);
      }

      ExecutorTask<String> task = makeUpdateTask(dto.id, json, sql);
      return executor.submit(task);
   }

   /**
    * @param changeType Type of change to perform.
    * @return A parameterized SQL statement (as a string) to create or update an extract.
    */
   private String getUpdateSql(ChangeType changeType)
   {
      switch (changeType)
      {
         case MODIFIED:
            return UPDATE_EXTRACT_SQL;
         case CREATED:
            return CREATE_EXTRACT_SQL;
         default:
            throw new IllegalArgumentException("Expected change type to be 'created' or 'modified', but received [" + changeType + "].");
      }
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
               return parseJson(pgo.toString(), mapper);
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
   private ExecutorTask<String> makeUpdateTask(String id, String json, String sql)
   {
      return (conn) ->
      {
         try (PreparedStatement ps = conn.prepareStatement(sql))
         {
            ps.setString(SQL_UPDATE_PARAM_ID, id);

            PGobject jsonObject = new PGobject();
            jsonObject.setType("json");
            jsonObject.setValue(json);
            ps.setObject(SQL_UPDATE_PARAM_EXTRACT, jsonObject);

            int ct = ps.executeUpdate();
            if (ct != 1)
            {
               throw new IllegalStateException("Failed to update extract. Unexpected number of rows updated [" + ct + "]");
            }

            return id;
         }
         catch (SQLException e)
         {
            throw new IllegalStateException("Failed to update extract: [" + id + "]\n" + json, e);
         }
      };
   }

   /**
    * Creates an executor task to delete an extract
    *
    * @param id The ID of the extract to delete
    * @return An executor task that resolves to the ID of the extract when removed.
    */
   private ExecutorTask<String> makeDeleteTask(String id)
   {
      return (conn) ->
      {
         try (PreparedStatement ps = conn.prepareCall(DELETE_EXTRACT_SQL))
         {
            ps.setString(SQL_DELETE_PARAM_ID, id);

            int ct = ps.executeUpdate();
            if (ct != 1)
            {
               throw new IllegalStateException("Failed to deactivate extract [" + id + "]. Unexpected number of rows updated [" + ct + "]");
            }

            return id;
         }
         catch (SQLException e)
         {
            throw new IllegalStateException("Failed to deactivate extract [" + id + "]", e);
         }
      };
   }

   /**
    * Deserializes a {@link DramaticExtract} from a JSON blob.
    *
    * @param json JSON Blob representing an {@link ExtractDTO}
    * @param mapper ObjectMapper to use for deserialization
    * @return
    */
   private static ExtractDTO parseJson(String json, ObjectMapper mapper)
   {
      try
      {
         return mapper.readValue(json, ExtractDTO.class);
      }
      catch (IOException je)
      {
         // NOTE: possible data leak. If this exception is propagated to someone who isn't authorized to see this record...
         throw new IllegalStateException("Cannot parse person from JSON:\n" + json, je);
      }
   }

}
