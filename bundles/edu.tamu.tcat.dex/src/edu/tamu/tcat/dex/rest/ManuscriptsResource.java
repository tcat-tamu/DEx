package edu.tamu.tcat.dex.rest;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import edu.tamu.tcat.dex.TrcBiblioType;
import edu.tamu.tcat.dex.importer.DexImportService;
import edu.tamu.tcat.dex.rest.v1.RepoAdapter;
import edu.tamu.tcat.dex.rest.v1.RestApiV1;
import edu.tamu.tcat.dex.trc.extract.ExtractRepository;
import edu.tamu.tcat.dex.trc.extract.search.ExtractSearchService;
import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.biblio.Work;
import edu.tamu.tcat.trc.entries.types.biblio.repo.WorkRepository;

@Path("/mss")
public class ManuscriptsResource
{
   private static final Logger logger = Logger.getLogger(ManuscriptsResource.class.getName());

   private WorkRepository repo;
   private DexImportService importService;
   
   // required for deletion
   private ExtractRepository extracts;

   public void setRepo(WorkRepository repo)
   {
      this.repo = repo;
   }
   
   public void setExtracts(ExtractRepository repo)
   {
      this.extracts = repo;
   }

   public void setImportService(DexImportService importService)
   {
      this.importService = importService;
   }

   public void activate()
   {
      try 
      {
         Objects.requireNonNull(repo, "No works repository provided.");
         Objects.requireNonNull(extracts, "No extracts repository provided.");
         Objects.requireNonNull(importService, "No import service provided.");
      }
      catch (Exception ex)
      {
         logger.log(Level.SEVERE, "Failed to start /mss REST resource.", ex);
         throw ex;
      }
   }

   @DELETE
   @Path("/{id}")
   @Produces(MediaType.APPLICATION_JSON)
   public Response remove(@PathParam("id") String id)
   {
      try 
      {
         // TODO should verify that this is in fact a MS
         try {
            repo.deleteWork(id);
         } catch (IllegalArgumentException ex) {
            // HACK: catch and suppress spurious exception. Thrown because no index service is configured for works.
         }
         extracts.removeByManuscriptId(id);
         return Response.noContent().build();
      }
      catch (Exception ex)
      {
         logger.log(Level.SEVERE, "Failed to delete manuscript " + id, ex);
         throw new IllegalStateException("Failed to delete manuscript " + id, ex);
      }
   }
   
   
   @GET
   @Path("/{id}")
   @Produces(MediaType.APPLICATION_JSON)
   public RestApiV1.Manuscript get(@PathParam("id") String id)
   {
      String manuscriptType = TrcBiblioType.Manuscript.toString();
      try
      {
         Work work = repo.getWork(id);
         String workType = work.getType();
         if (!manuscriptType.equals(workType))
         {
            throw new NoSuchCatalogRecordException("Wrong bibliographic type: expected [" + manuscriptType + "] but received [" + workType + "].");
         }
         return RepoAdapter.toManuscriptDTO(work);
      }
      catch (NoSuchCatalogRecordException e)
      {
         throw new NotFoundException("Unable to find manuscript [" + id + "]");
      }
   }

   @GET
   @Path("/{id}/tei")
   @Produces(MediaType.APPLICATION_XML)
   public StreamingOutput getTei(@PathParam("id") String id)
   {
      return (out) -> importService.exportManuscriptTEI(id, out);
   }

}
