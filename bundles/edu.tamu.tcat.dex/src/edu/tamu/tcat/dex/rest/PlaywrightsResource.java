package edu.tamu.tcat.dex.rest;

import java.util.Objects;

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
   private PeopleRepository repo;

   public void setRepo(PeopleRepository repo)
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
