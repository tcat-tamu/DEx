package edu.tamu.tcat.dex.rest.v1;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import edu.tamu.tcat.trc.entries.search.solr.SolrIndexField;
import edu.tamu.tcat.trc.extract.search.FacetItemList.FacetItem;
import edu.tamu.tcat.trc.extract.search.SearchExtractResult;
import edu.tamu.tcat.trc.extract.search.solr.ExtractSolrConfig;

public class SearchAdapter
{
   public static final String REST_FIELD_MANUSCRIPT = "manuscript";
   public static final String REST_FIELD_PLAY = "play";
   public static final String REST_FIELD_PLAYWRIGHT = "playwright";
   public static final String REST_FIELD_SPEAKER = "speaker";

   private static final Map<String, SolrIndexField<?>> REST_SOLR_FIELD_NAME_MAP = new HashMap<>();
   private static final Map<String, String> SOLR_REST_FIELD_NAME_MAP = new HashMap<>();

   static
   {
      REST_SOLR_FIELD_NAME_MAP.put(REST_FIELD_MANUSCRIPT, ExtractSolrConfig.MANUSCRIPT_TITLE);
      REST_SOLR_FIELD_NAME_MAP.put(REST_FIELD_PLAY, ExtractSolrConfig.PLAY_TITLE);
      REST_SOLR_FIELD_NAME_MAP.put(REST_FIELD_PLAYWRIGHT, ExtractSolrConfig.PLAYWRIGHT_NAME);
      REST_SOLR_FIELD_NAME_MAP.put(REST_FIELD_SPEAKER, ExtractSolrConfig.SPEAKER_NAME);

      // NOTE THIS IS NOT THREAD-SAFE!!!!
      REST_SOLR_FIELD_NAME_MAP.entrySet().stream()
         .forEach(e -> SOLR_REST_FIELD_NAME_MAP.put(e.getValue().getName(), e.getKey()));
   }


   public static RestApiV1.ResultList toDTO(SearchExtractResult results, int page, int numResultsPerPage)
   {
      RestApiV1.ResultList dto = new RestApiV1.ResultList();

      dto.page = page;
      dto.numResultsPerPage = numResultsPerPage;
      dto.numFound = results.getNumFound();
      dto.results = results.get();

      dto.facets = results.getFacets().parallelStream()
            .collect(Collectors.toMap(
                  f ->  SOLR_REST_FIELD_NAME_MAP.get(f.getFieldName()),
                  f -> f.getValues().stream()
                        .map(SearchAdapter::toDTO)
                        .collect(Collectors.toList())
            ));

      return dto;
   }

   public static RestApiV1.FacetItem toDTO(FacetItem item)
   {
      RestApiV1.FacetItem dto = new RestApiV1.FacetItem();

      dto.label = item.getLabel();
      dto.count = item.getCount();
      dto.selected = item.isSelected();

      return dto;
   }
}
