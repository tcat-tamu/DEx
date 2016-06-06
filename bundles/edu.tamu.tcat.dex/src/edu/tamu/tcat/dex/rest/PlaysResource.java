package edu.tamu.tcat.dex.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import edu.tamu.tcat.dex.TrcBiblioType;
import edu.tamu.tcat.dex.rest.v1.RepoAdapter;
import edu.tamu.tcat.dex.rest.v1.RestApiV1;
import edu.tamu.tcat.dex.rest.v1.RestApiV1.PlayBibEntry;
import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.biblio.Work;
import edu.tamu.tcat.trc.entries.types.biblio.repo.WorkRepository;

@Path("/plays")
public class PlaysResource
{
   private static final Logger logger = Logger.getLogger(PlaysResource.class.getName());

   private WorkRepository repo;

   public void setRepo(WorkRepository repo)
   {
      this.repo = repo;
   }

   public void activate()
   {
      try 
      {
         Objects.requireNonNull(repo, "No repository provided");
      }
      catch (Exception ex)
      {
         logger.log(Level.SEVERE, "Failed to start /plays REST resource.", ex);
         throw ex;
      }
   }

   public void dispose()
   {
      repo = null;
   }

   @GET
   @Produces(MediaType.APPLICATION_JSON)
   public List<RestApiV1.PlayBibEntry> bibliography()
   {
      List<RestApiV1.PlayBibEntry> bibEntries = new ArrayList<>();

      Iterable<Work> works = () -> this.repo.getAllWorks();
      for (Work work : works)
      {
         if (!TrcBiblioType.Play.toString().equals(work.getType())) {
            continue;
         }

         PlayBibEntry dto = RepoAdapter.toPlayBibEntryDTO(work);
         bibEntries.add(dto);
      }

      return bibEntries;
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
