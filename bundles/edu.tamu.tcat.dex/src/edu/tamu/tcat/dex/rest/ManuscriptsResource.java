package edu.tamu.tcat.dex.rest;

import java.util.Objects;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import edu.tamu.tcat.dex.TrcBiblioType;
import edu.tamu.tcat.dex.importer.DexImportService;
import edu.tamu.tcat.dex.rest.v1.RepoAdapter;
import edu.tamu.tcat.dex.rest.v1.RestApiV1;
import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.biblio.Work;
import edu.tamu.tcat.trc.entries.types.biblio.repo.WorkRepository;

@Path("/mss")
public class ManuscriptsResource
{
   private WorkRepository repo;
   private DexImportService importService;

   public void setRepo(WorkRepository repo)
   {
      this.repo = repo;
   }

   public void setImportService(DexImportService importService)
   {
      this.importService = importService;
   }

   public void activate()
   {
      Objects.requireNonNull(repo, "No works repository provided.");
      Objects.requireNonNull(importService, "No import service provided.");
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
