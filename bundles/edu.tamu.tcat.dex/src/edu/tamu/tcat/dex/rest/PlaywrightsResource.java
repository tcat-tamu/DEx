package edu.tamu.tcat.dex.rest;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import edu.tamu.tcat.dex.rest.v1.RepoAdapter;
import edu.tamu.tcat.dex.rest.v1.RestApiV1;
import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.bio.Person;
import edu.tamu.tcat.trc.entries.types.bio.repo.PeopleRepository;

@Path("/pws")
public class PlaywrightsResource
{
   private static final Logger logger = Logger.getLogger(PlaywrightsResource.class.getName());

   private PeopleRepository repo;

   public void setRepo(PeopleRepository repo)
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
         logger.log(Level.SEVERE, "Failed to start /pws REST resource.", ex);
         throw ex;
      }
   }

   public void dispose()
   {
      repo = null;
   }


   @GET
   @Path("/{id}")
   @Produces(MediaType.APPLICATION_JSON)
   public RestApiV1.Playwright get(@PathParam("id") String id)
   {
      try
      {
         Person person = repo.get(id);
         return RepoAdapter.toPlaywrightDTO(person);
      }
      catch (NoSuchCatalogRecordException e)
      {
         throw new NotFoundException("unable to find play [" + id + "]");
      }
   }
}
