package edu.tamu.tcat.dex.rest;

import java.util.List;
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
import edu.tamu.tcat.trc.entries.search.SearchException;
import edu.tamu.tcat.trc.extract.dto.ExtractDTO;
import edu.tamu.tcat.trc.extract.search.ExtractQueryCommand;
import edu.tamu.tcat.trc.extract.search.ExtractSearchService;
import edu.tamu.tcat.trc.extract.search.SearchExtractResult;
import edu.tamu.tcat.trc.extract.search.solr.ExtractSearchProxy;

@Path("/extracts")
@Produces(MediaType.APPLICATION_JSON)
public class ExtractsResource
{
   public static class ResultListDTO
   {
      public int numResultsPerPage;
      public int page;
      public long numFound;
      public List<ExtractSearchProxy> results;
   }

   private ExtractRepository repo;
   private ExtractSearchService searchService;

   public void setRepo(ExtractRepository repo)
   {
      this.repo = repo;
   }

   public void setSearchService(ExtractSearchService searchService)
   {
      this.searchService = searchService;
   }

   public void activate()
   {
      Objects.requireNonNull(repo, "No repository specified");
      Objects.requireNonNull(searchService, "No search service specified");
   }

   @GET
   @Path("/")
   @Produces(MediaType.APPLICATION_JSON)
   public ResultListDTO browseAll(@DefaultValue("1") @QueryParam("page") int page,
                         @DefaultValue("-1") @QueryParam("numResults") int numResultsPerPage)
   {
      try {
         ExtractQueryCommand queryCommand = searchService.createQueryCommand();
         queryCommand.queryAll();
         queryCommand.setOffset(numResultsPerPage * (page-1));
         queryCommand.setMaxResults(numResultsPerPage);
         SearchExtractResult results = queryCommand.execute();

         ResultListDTO dto = new ResultListDTO();
         dto.page = page;
         dto.numResultsPerPage = numResultsPerPage;
         dto.numFound = results.getNumFound();
         dto.results = results.get();

         return dto;
      }
      catch (SearchException e) {
         throw new ServerErrorException("Unable to execute fetch-all", Status.INTERNAL_SERVER_ERROR, e);
      }
   }

   // TODO: faceting
   @GET
   @Path("/search")
   @Produces(MediaType.APPLICATION_JSON)
   public ResultListDTO search(@QueryParam("q") String query,
                      @DefaultValue("1") @QueryParam("page") int page,
                      @DefaultValue("-1") @QueryParam("numResults") int numResultsPerPage)
   {
      ExtractQueryCommand queryCommand;
      try {
         queryCommand = searchService.createQueryCommand();
         queryCommand.query(query);
         queryCommand.setOffset(numResultsPerPage * (page-1));
         queryCommand.setMaxResults(numResultsPerPage);
         SearchExtractResult results = queryCommand.execute();

         ResultListDTO dto = new ResultListDTO();
         dto.page = page;
         dto.numResultsPerPage = numResultsPerPage;
         dto.numFound = results.getNumFound();
         dto.results = results.get();

         return dto;
      }
      catch (SearchException e) {
         throw new ServerErrorException("Unable to execute search query [" + query + "]", Status.INTERNAL_SERVER_ERROR, e);
      }
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
