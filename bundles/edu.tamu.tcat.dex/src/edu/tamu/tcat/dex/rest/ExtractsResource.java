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

import edu.tamu.tcat.dex.rest.v1.RestApiV1;
import edu.tamu.tcat.dex.rest.v1.SearchAdapter;
import edu.tamu.tcat.dex.trc.entry.DramaticExtract;
import edu.tamu.tcat.dex.trc.entry.DramaticExtractException;
import edu.tamu.tcat.dex.trc.entry.ExtractNotAvailableException;
import edu.tamu.tcat.dex.trc.entry.ExtractRepository;
import edu.tamu.tcat.trc.entries.search.SearchException;
import edu.tamu.tcat.trc.extract.dto.ExtractDTO;
import edu.tamu.tcat.trc.extract.search.ExtractQueryCommand;
import edu.tamu.tcat.trc.extract.search.ExtractSearchService;
import edu.tamu.tcat.trc.extract.search.SearchExtractResult;

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

//   @GET
//   @Path("/")
//   @Produces(MediaType.APPLICATION_JSON)
//   public RestApiV1.ResultList browseAll(@DefaultValue("1") @QueryParam("page") int page,
//                         @DefaultValue("-1") @QueryParam("numResults") int numResultsPerPage)
//   {
//      try {
//         ExtractQueryCommand queryCommand = searchService.createQueryCommand();
//         queryCommand.queryAll();
//         queryCommand.setOffset(numResultsPerPage * (page-1));
//         queryCommand.setMaxResults(numResultsPerPage);
//         SearchExtractResult results = queryCommand.execute();
//         return SearchAdapter.toDto(results, page, numResultsPerPage);
//      }
//      catch (SearchException e) {
//         throw new ServerErrorException("Unable to execute fetch-all", Status.INTERNAL_SERVER_ERROR, e);
//      }
//   }

   // TODO: faceting
   @GET
   @Path("/search")
   @Produces(MediaType.APPLICATION_JSON)
   public RestApiV1.ResultList search(@QueryParam("q") String query,
                               @DefaultValue("") @QueryParam("a") String advancedQuery,
                               @DefaultValue("") @QueryParam("ms") String manuscriptQuery,
                               @DefaultValue("") @QueryParam("pw") String playwrightQuery,
                               @DefaultValue("") @QueryParam("pl") String playQuery,
                               @DefaultValue("") @QueryParam("sp") String speakerQuery,
                               @DefaultValue("1") @QueryParam("p") int page,
                               @DefaultValue("-1") @QueryParam("n") int numResultsPerPage)
   {
      try {
         ExtractQueryCommand queryCommand = searchService.createQueryCommand();

         if (query != null)
         {
            queryCommand.query(query);
         }
         else
         {
            if (!advancedQuery.isEmpty())
            {
               queryCommand.advancedQuery(advancedQuery);
            }

            if (!manuscriptQuery.isEmpty())
            {
               queryCommand.queryManuscript(manuscriptQuery);
            }

            if (!playwrightQuery.isEmpty())
            {
               queryCommand.queryPlaywright(playwrightQuery);
            }

            if (!playQuery.isEmpty())
            {
               queryCommand.queryPlay(playQuery);
            }

            if (!speakerQuery.isEmpty())
            {
               queryCommand.querySpeaker(speakerQuery);
            }
         }

         queryCommand.setOffset(numResultsPerPage * (page-1));
         queryCommand.setMaxResults(numResultsPerPage);
         SearchExtractResult results = queryCommand.execute();
         return SearchAdapter.toDTO(results, page, numResultsPerPage);
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
