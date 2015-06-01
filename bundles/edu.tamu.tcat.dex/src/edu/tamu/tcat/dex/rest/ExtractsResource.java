package edu.tamu.tcat.dex.rest;

import java.util.Objects;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import edu.tamu.tcat.dex.trc.entry.DramaticExtract;
import edu.tamu.tcat.dex.trc.entry.DramaticExtractException;
import edu.tamu.tcat.dex.trc.entry.ExtractNotAvailableException;
import edu.tamu.tcat.dex.trc.entry.ExtractRepository;
import edu.tamu.tcat.trc.extract.dto.ExtractDTO;

@Path("/extracts")
public class ExtractsResource
{
   private ExtractRepository repo;

   public void setRepo(ExtractRepository repo)
   {
      this.repo = repo;
   }

   public void activate()
   {
      Objects.requireNonNull(repo, "No repository specified");
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
   public ExtractDTO get(@PathParam("id") String id)
   {
      try
      {
         DramaticExtract extract = repo.get(id);
         return ExtractDTO.create(extract);
      }
      catch (ExtractNotAvailableException e)
      {
         throw new NotFoundException("No such extract [" + id + "]", e);
      }
      catch (DramaticExtractException e)
      {
         throw new ServerErrorException("Unable to retrieve extract [" + id + "]", Status.INTERNAL_SERVER_ERROR, e);
      }

   }
}
