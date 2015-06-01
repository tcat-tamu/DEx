package edu.tamu.tcat.dex.rest;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.bib.Work;
import edu.tamu.tcat.trc.entries.types.bib.repo.WorkRepository;
import edu.tamu.tcat.trc.extract.dto.ManuscriptDTO;

@Path("/mss")
public class ManuscriptsResource
{
   private WorkRepository repo;

   public void setRepo(WorkRepository repo)
   {
      this.repo = repo;
   }

   @GET
   @Path("/")
   @Produces(MediaType.APPLICATION_JSON)
   public void browseAll(@DefaultValue("1") @QueryParam("page") int page,
                         @DefaultValue("-1") @QueryParam("numResults") int numResultsPerPage)
   {
      throw new UnsupportedOperationException("not yet implemented");
   }

   // TODO: faceting
   @GET
   @Path("/search")
   @Produces(MediaType.APPLICATION_JSON)
   public void search(@QueryParam("q") String query,
                      @DefaultValue("1") @QueryParam("page") int page,
                      @DefaultValue("-1") @QueryParam("numResults") int numResultsPerPage)
   {
      throw new UnsupportedOperationException("not yet implemented");
   }

   @GET
   @Path("/{id}")
   @Produces(MediaType.APPLICATION_JSON)
   public ManuscriptDTO get(@PathParam("id") String id)
   {
         try
         {
            Work work = repo.getWork(id);
            return ManuscriptDTO.create(work);
         }
         catch (NoSuchCatalogRecordException e)
         {
            throw new NotFoundException("Unable to find manuscript [" + id + "]");
         }
   }

}
