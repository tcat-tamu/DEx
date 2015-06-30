package edu.tamu.tcat.dex.rest.v1;

import java.util.ArrayList;
import java.util.HashMap;
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
      public List<ExtractSearchProxy> results = new ArrayList<>();

      public Map<String, List<FacetItem>> facets = new HashMap<>();
   }

   public static class FacetItem
   {
      public String label;
      public long count;
      public boolean selected;
   }

   public static class Manuscript
   {
      public String id;
      public String title;
      public String author;
   }

   public static class PlayReference
   {
      public String id;
      public String title;
   }

   public static class Play
   {
      public String id;
      public String title;
      public List<PlaywrightReference> playwrights = new ArrayList<>();
   }

   public static class PlaywrightReference
   {
      public String id;
      public String name;
   }

   public static class Playwright
   {
       public String id;
       public List<String> names = new ArrayList<>();
   }

   public static class Character
   {
      public String id;
      public String name;
      public List<PlayReference> plays = new ArrayList<>();
   }
}
