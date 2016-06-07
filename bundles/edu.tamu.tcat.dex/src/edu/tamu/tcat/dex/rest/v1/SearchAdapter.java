package edu.tamu.tcat.dex.rest.v1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.tamu.tcat.dex.trc.extract.search.FacetItemList;
import edu.tamu.tcat.dex.trc.extract.search.FacetItemList.FacetItem;
import edu.tamu.tcat.dex.trc.extract.search.SearchExtractResult;
import edu.tamu.tcat.dex.trc.extract.search.solr.ExtractSolrConfig;
import edu.tamu.tcat.trc.search.solr.SolrIndexField;

public class SearchAdapter
{
   public static final String REST_FACET_FIELD_MANUSCRIPT = "manuscript";
   public static final String REST_FACET_FIELD_PLAY = "play";
   public static final String REST_FACET_FIELD_PLAYWRIGHT = "playwright";
   public static final String REST_FACET_FIELD_SPEAKER = "speaker";

   private static final Map<String, FacetFieldAdapter> facetAdapters = new HashMap<>();

   static
   {
      // HACK: Need to introduce support for facetting to trc.search
      addBiDiMapping(ExtractSolrConfig.MANUSCRIPT_FACET, REST_FACET_FIELD_MANUSCRIPT);
      addBiDiMapping(ExtractSolrConfig.PLAY_FACET, REST_FACET_FIELD_PLAY);
      addBiDiMapping(ExtractSolrConfig.PLAYWRIGHT_FACET, REST_FACET_FIELD_PLAYWRIGHT);
      addBiDiMapping(ExtractSolrConfig.SPEAKER_FACET, REST_FACET_FIELD_SPEAKER);
   }
   
   private static void addBiDiMapping(SolrIndexField<String> facetFld, String restFld)
   {
      FacetFieldAdapter strategy = new FacetFieldAdapter(facetFld, restFld);
      facetAdapters.put(facetFld.getName(), strategy);
   }
   
   public static class FacetFieldAdapter
   {
      // HACK: should be moved to a more general location
      public final SolrIndexField<String> indexField;
      public final String restField;
      
      FacetFieldAdapter(SolrIndexField<String> facetFld, String restFld )
      {
         this.indexField = facetFld;
         this.restField = restFld;
      }
      
      public List<RestApiV1.FacetItem> adapt(FacetItemList facet)
      {
         return facet.getValues().stream()
            .map(SearchAdapter::toDTO)
            .collect(Collectors.toList());
      }
   }


   public static RestApiV1.ResultList toDTO(SearchExtractResult results, int page, int numResultsPerPage)
   {
      RestApiV1.ResultList dto = new RestApiV1.ResultList();

      dto.page = page;
      dto.numResultsPerPage = numResultsPerPage;
      dto.numFound = results.getNumFound();
      dto.results = results.get();

      results.getFacets().stream().forEach(facet -> {
         String fld = facet.getFieldName();
         if (facetAdapters.containsKey(fld))
         {
            FacetFieldAdapter adapter = facetAdapters.get(fld);
            dto.facets.put(adapter.restField, adapter.adapt(facet));
         }
      });

      return dto;
   }
   
   public static RestApiV1.FacetItem toDTO(FacetItem item)
   {
      RestApiV1.FacetItem dto = new RestApiV1.FacetItem();

      dto.id = item.getId();
      dto.label = item.getLabel();
      dto.count = item.getCount();
      dto.selected = item.isSelected();

      return dto;
   }
}
