package edu.tamu.tcat.dex.rest.v1;

import java.util.List;
import java.util.Map;

import edu.tamu.tcat.trc.extract.search.solr.ExtractSearchProxy;

public class RestApiV1
{
   public static class ResultList
   {
      public int numResultsPerPage;
      public int page;
      public long numFound;
      public List<ExtractSearchProxy> results;

      public Map<String, List<FacetItem>> facets;
   }

   public static class FacetItem
   {
      public String label;
      public long count;
   }
}