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

import edu.tamu.tcat.dex.rest.v1.RestApiV1;
import edu.tamu.tcat.dex.rest.v1.SearchAdapter;
import edu.tamu.tcat.dex.trc.extract.DramaticExtract;
import edu.tamu.tcat.dex.trc.extract.DramaticExtractException;
import edu.tamu.tcat.dex.trc.extract.ExtractNotAvailableException;
import edu.tamu.tcat.dex.trc.extract.ExtractRepository;
import edu.tamu.tcat.dex.trc.extract.dto.ExtractDTO;
import edu.tamu.tcat.dex.trc.extract.search.ExtractQueryCommand;
import edu.tamu.tcat.dex.trc.extract.search.ExtractSearchService;
import edu.tamu.tcat.dex.trc.extract.search.SearchExtractResult;

@Path("/extracts")
@Produces(MediaType.APPLICATION_JSON)
public class ExtractsResource
{
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

   // TODO: faceting
   @GET
   @Path("/search")
   @Produces(MediaType.APPLICATION_JSON)
   public RestApiV1.ResultList search(@QueryParam("q") String query,
                               @QueryParam("a") String advancedQuery,
                               @QueryParam("ms") String manuscriptQuery,
                               @QueryParam("f.ms[]") List<String> manuscriptFilters,
                               @QueryParam("pw") String playwrightQuery,
                               @QueryParam("f.pw[]") List<String> playwrightFilters,
                               @QueryParam("pl") String playQuery,
                               @QueryParam("f.pl[]") List<String> playFilters,
                               @QueryParam("sp") String speakerQuery,
                               @QueryParam("f.sp[]") List<String> speakerFilters,
                               @QueryParam("sort") String sortBy,
                               @DefaultValue("1") @QueryParam("p") int page,
                               @DefaultValue("-1") @QueryParam("n") int numResultsPerPage,
                               @DefaultValue("10") @QueryParam("f.n") int numFacets)
   {
      try {
         ExtractQueryCommand queryCommand = searchService.createQueryCommand();

         boolean basicQuery = true;

         // advanced query
         if (advancedQuery != null && !advancedQuery.isEmpty())
         {
            queryCommand.advancedQuery(advancedQuery);
            basicQuery = false;
         }

         if (manuscriptQuery != null && !manuscriptQuery.isEmpty())
         {
            queryCommand.queryManuscript(manuscriptQuery);
            basicQuery = false;
         }

         if (playwrightQuery != null && !playwrightQuery.isEmpty())
         {
            queryCommand.queryPlaywright(playwrightQuery);
            basicQuery = false;
         }

         if (playQuery != null && !playQuery.isEmpty())
         {
            queryCommand.queryPlay(playQuery);
            basicQuery = false;
         }

         if (speakerQuery != null && !speakerQuery.isEmpty())
         {
            queryCommand.querySpeaker(speakerQuery);
            basicQuery = false;
         }

         if (basicQuery) {
            queryCommand.query(query == null ? "" : query);
         }

         // search refinement applies to both the basic and advanced query variants
         if (manuscriptFilters != null && !manuscriptFilters.isEmpty())
         {
            queryCommand.filterManuscript(manuscriptFilters);
         }

         if (playwrightFilters != null && !playwrightFilters.isEmpty())
         {
            queryCommand.filterPlaywright(playwrightFilters);
         }

         if (playFilters != null && !playFilters.isEmpty())
         {
            queryCommand.filterPlay(playFilters);
         }

         if (speakerFilters != null && !speakerFilters.isEmpty())
         {
            queryCommand.filterSpeaker(speakerFilters);
         }

         queryCommand.setOffset(numResultsPerPage * (page-1));
         queryCommand.setMaxResults(numResultsPerPage);
         queryCommand.setMaxFacets(numFacets);
         SearchExtractResult results = queryCommand.execute();
         return SearchAdapter.toDTO(results, page, numResultsPerPage);
      }
      catch (Exception e) {
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
