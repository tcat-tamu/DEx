package edu.tamu.tcat.dex.rest;

import java.util.Objects;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import edu.tamu.tcat.dex.TrcBiblioType;
import edu.tamu.tcat.dex.rest.v1.RepoAdapter;
import edu.tamu.tcat.dex.rest.v1.RestApiV1;
import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.biblio.Work;
import edu.tamu.tcat.trc.entries.types.biblio.repo.WorkRepository;

@Path("/plays")
public class PlaysResource
{
   private WorkRepository repo;

   public void setRepo(WorkRepository repo)
   {
      this.repo = repo;
   }

   public void activate()
   {
      Objects.requireNonNull(repo, "No repository provided");
   }

   public void dispose()
   {
      repo = null;
   }


   @GET
   @Path("/{id}")
   @Produces(MediaType.APPLICATION_JSON)
   public RestApiV1.Play get(@PathParam("id") String id)
   {
      String playType = TrcBiblioType.Play.toString();
      try
      {
         Work work = repo.getWork(id);
         String workType = work.getType();
         if (!playType.equals(workType))
         {
            throw new NoSuchCatalogRecordException("Wrong bibliographic type: expected [" + playType + "] but recieved [" + workType + "].");
         }
         return RepoAdapter.toPlayDTO(work);
      }
      catch (NoSuchCatalogRecordException e)
      {
         throw new NotFoundException("unable to find play [" + id + "]");
      }
   }
}
