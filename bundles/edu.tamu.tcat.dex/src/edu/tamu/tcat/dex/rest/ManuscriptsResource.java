package edu.tamu.tcat.dex.rest;

import java.util.Objects;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import edu.tamu.tcat.dex.rest.v1.RepoAdapter;
import edu.tamu.tcat.dex.rest.v1.RestApiV1;
import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.bib.Work;
import edu.tamu.tcat.trc.entries.types.bib.repo.WorkRepository;

@Path("/mss")
public class ManuscriptsResource
{
   private WorkRepository repo;

   public void setRepo(WorkRepository repo)
   {
      this.repo = repo;
   }

   public void activate()
   {
      Objects.requireNonNull(repo, "No works repository provided.");
   }

   @GET
   @Path("/")
   @Produces(MediaType.APPLICATION_JSON)
   public void browseAll(@DefaultValue("1") @QueryParam("page") int page,
                         @DefaultValue("-1") @QueryParam("numResultsPerPage") int numResultsPerPage)
   {
      throw new UnsupportedOperationException("not yet implemented");
      // use faceting to get all manuscripts from the Solr Server:
      //    resp = GET {solr-endpoint}/select?rows=0&wt=json&facet=true&facet.field=mss_title
      //    return resp.facet_counts.facet_fields.mss_title[::2]
   }

   // TODO: faceting
   @GET
   @Path("/search")
   @Produces(MediaType.APPLICATION_JSON)
   public void search(@QueryParam("q") String query,
                      @DefaultValue("1") @QueryParam("page") int page,
                      @DefaultValue("-1") @QueryParam("numResultsPerPage") int numResultsPerPage)
   {
      throw new UnsupportedOperationException("not yet implemented");
   }

   @GET
   @Path("/{id}")
   @Produces(MediaType.APPLICATION_JSON)
   public RestApiV1.Manuscript get(@PathParam("id") String id)
   {
         try
         {
            Work work = repo.getWork(id);
            return RepoAdapter.toDTO(work);
         }
         catch (NoSuchCatalogRecordException e)
         {
            throw new NotFoundException("Unable to find manuscript [" + id + "]");
         }
   }

}
