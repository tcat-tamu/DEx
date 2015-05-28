package edu.tamu.tcat.dex.rest;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/mss")
public class ManuscriptsResource
{

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
   public void get(@PathParam("id") String id)
   {
      throw new UnsupportedOperationException("not yet implemented");
   }

}
