package edu.tamu.tcat.dex.internal.trc.entry.postgres;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Future;

import org.postgresql.util.PGobject;

import edu.tamu.tcat.db.exec.sql.SqlExecutor;
import edu.tamu.tcat.db.exec.sql.SqlExecutor.ExecutorTask;
import edu.tamu.tcat.dex.internal.trc.entry.dto.ExtractDTO;
import edu.tamu.tcat.dex.trc.entry.DramaticExtract;
import edu.tamu.tcat.dex.trc.entry.DramaticExtractException;
import edu.tamu.tcat.dex.trc.entry.EditExtractCommand;
import edu.tamu.tcat.dex.trc.entry.ExtractNotAvailableException;
import edu.tamu.tcat.dex.trc.entry.ExtractRepository;
import edu.tamu.tcat.dex.trc.entry.ExtractsChangeEvent.ChangeType;
import edu.tamu.tcat.trc.entries.notification.UpdateListener;

public class PsqlExtractRepo implements ExtractRepository
{
   private static final String UPDATE_EXTRACT_SQL = "UPDATE extracts SET extract = ? WHERE id = ?";
   private static final String CREATE_EXTRACT_SQL = "INSERT INTO extracts (extract, id) VALUES (?, ?)";
   private static final String DELETE_EXTRACT_SQL = "UPDATE extracts SET active = FALSE WHERE id = ?";

   private static final int SQL_UPDATE_PARAM_EXTRACT = 1;
   private static final int SQL_UPDATE_PARAM_ID = 2;
   private static final int SQL_DELETE_PARAM_ID = 1;

   private SqlExecutor executor;

   @Override
   public DramaticExtract get(URI id) throws ExtractNotAvailableException, DramaticExtractException
   {
      // TODO Auto-generated method stub
      return null;
   }

   /**
    * DI method called by Equinox DS
    *
    * @param executor the SQL executor service
    */
   public void setSqlExecutor(SqlExecutor executor)
   {
      this.executor = executor;
   }

   @Override
   public EditExtractCommand create() throws DramaticExtractException
   {
      ExtractDTO extractDTO = new ExtractDTO();
      EditExtractCommandImpl command = new EditExtractCommandImpl(extractDTO);
      command.setCommitHook(dto -> updateExtract(dto, ChangeType.CREATED));
      return command;
   }

   @Override
   public EditExtractCommand edit(URI id) throws ExtractNotAvailableException
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void remove(URI id) throws DramaticExtractException
   {
      // TODO Auto-generated method stub

   }

   @Override
   public AutoCloseable register(UpdateListener<DramaticExtract> ears)
   {
      // TODO Auto-generated method stub
      return null;
   }

   /**
    * Creates or updates the dramatic extract in the database.
    *
    * @param dto The extract to persist
    * @param changeType The type of change to perform
    * @return A future that resolves to the ID of the saved extract
    */
   private Future<String> updateExtract(ExtractDTO dto, ChangeType changeType)
   {
      String sql = getUpdateSql(changeType);

      // TODO: json-serialize extract
      String json = "";

      ExecutorTask<String> task = makeUpdateTask(dto.id, json, sql);
      return executor.submit(task);
   }

   /**
    * Deletes the dramatic extract from the database.
    *
    * @param id The ID of the extract to delete
    */
   public void delete(String id)
   {
      ExecutorTask<String> task = makeDeleteTask(id);
      executor.submit(task);
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
            throw new IllegalStateException("Failed to update extract: [" + id + "]\n" + json);
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
            throw new IllegalStateException("Failed to deactivate extract [" + id + "]");
         }
      };
   }

}
