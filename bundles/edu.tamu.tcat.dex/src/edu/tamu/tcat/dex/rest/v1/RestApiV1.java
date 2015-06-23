package edu.tamu.tcat.dex.rest.v1;

import java.util.List;

import edu.tamu.tcat.trc.extract.search.solr.ExtractSearchProxy;

public class RestApiV1
{
   public static class ResultListDTO
   {
      public int numResultsPerPage;
      public int page;
      public long numFound;
      public List<ExtractSearchProxy> results;

      // TODO: faceting: active facets, available facets
   }
}