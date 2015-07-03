package edu.tamu.tcat.trc.extract.search.solr;

import java.util.Arrays;
import java.util.Collection;

import org.apache.solr.client.solrj.SolrQuery;

import edu.tamu.tcat.trc.entries.search.SearchException;
import edu.tamu.tcat.trc.entries.search.solr.SolrIndexConfig;
import edu.tamu.tcat.trc.entries.search.solr.SolrIndexField;
import edu.tamu.tcat.trc.entries.search.solr.impl.BasicFields;

public class ExtractSolrConfig implements SolrIndexConfig
{
   public static final SolrIndexField<String> ID = new BasicFields.BasicString("id");
   public static final BasicFields.SearchProxyField<ExtractSearchProxy> SEARCH_PROXY = new BasicFields.SearchProxyField<ExtractSearchProxy>("extract_proxy", ExtractSearchProxy.class);
   public static final SolrIndexField<String> MANUSCRIPT_ID = new BasicFields.BasicString("mss_shelfmark");
   public static final SolrIndexField<String> MANUSCRIPT_FACET = new BasicFields.BasicString("mss_facet_value");
   public static final SolrIndexField<String> MANUSCRIPT_TITLE = new BasicFields.BasicString("mss_title");
   public static final SolrIndexField<String> MANUSCRIPT_TITLE_SEARCHABLE = new BasicFields.BasicString("mss_title_searchable");
   public static final SolrIndexField<String> NORMALIZED = new BasicFields.BasicString("normalized");
   public static final SolrIndexField<String> ORIGINAL = new BasicFields.BasicString("original");
   public static final SolrIndexField<String> PLAYWRIGHT_ID = new BasicFields.BasicString("playwright_id");
   public static final SolrIndexField<String> PLAYWRIGHT_FACET = new BasicFields.BasicString("playwright_facet_value");
   public static final SolrIndexField<String> PLAYWRIGHT_NAME = new BasicFields.BasicString("playwright_name");
   public static final SolrIndexField<String> PLAYWRIGHT_NAME_SEARCHABLE = new BasicFields.BasicString("playwright_name_searchable");
   public static final SolrIndexField<String> PLAY_ID = new BasicFields.BasicString("play_id");
   public static final SolrIndexField<String> PLAY_FACET = new BasicFields.BasicString("play_facet_value");
   public static final SolrIndexField<String> PLAY_TITLE = new BasicFields.BasicString("play_title");
   public static final SolrIndexField<String> PLAY_TITLE_SEARCHABLE = new BasicFields.BasicString("play_title_searchable");
   public static final SolrIndexField<String> SPEAKER_ID = new BasicFields.BasicString("speaker_id");
   public static final SolrIndexField<String> SPEAKER_FACET = new BasicFields.BasicString("speaker_facet_value");
   public static final SolrIndexField<String> SPEAKER_NAME = new BasicFields.BasicString("speaker_name");
   public static final SolrIndexField<String> SPEAKER_NAME_SEARCHABLE = new BasicFields.BasicString("speaker_name_searchable");

   public static final String FACET_EXCLUDE_TAG_MANUSCRIPT = "exManuscript";
   public static final String FACET_EXCLUDE_TAG_PLAYWRIGHT = "exPlaywright";
   public static final String FACET_EXCLUDE_TAG_PLAY = "exPlay";
   public static final String FACET_EXCLUDE_TAG_SPEAKER = "exSpeaker";

   @Override
   public void initialConfiguration(SolrQuery params) throws SearchException
   {
      /*
       * Using eDisMax seemed like a more adventagous way of doing the query. This will allow
       * additional solr Paramaters to be set in order to 'fine tune' the query.
       */
      params.set("defType", "edismax");

      // enable faceting
      // HACK Faceting should be done by ID rather than by their display values.
      //      IDs should be resolved server-side by an in-memory cache.
      params.setFacet(true);
      params.setFacetLimit(10);
      params.setFacetMinCount(1);
      addFacetField(params, MANUSCRIPT_FACET.getName(), FACET_EXCLUDE_TAG_MANUSCRIPT);
      addFacetField(params, PLAYWRIGHT_FACET.getName(), FACET_EXCLUDE_TAG_PLAYWRIGHT);
      addFacetField(params, PLAY_FACET.getName(), FACET_EXCLUDE_TAG_PLAY);
      addFacetField(params, SPEAKER_FACET.getName(), FACET_EXCLUDE_TAG_SPEAKER);
   }

   private static void addFacetField(SolrQuery params, String solrFieldName, String excludeTag)
   {
      params.add("facet.field", (excludeTag == null || excludeTag.isEmpty() ? "" : "{!ex=" + excludeTag + "}") + solrFieldName);
   }

   @Override
   public void configureBasic(String q, SolrQuery params) throws SearchException
   {
      // HACK: if no query is specified, the default will be to return all documents
      if (q == null || q.trim().isEmpty())
      {
         q = "*:*";
      }

      StringBuilder qBuilder = new StringBuilder(q);

      params.set("q", qBuilder.toString());

      // TODO give precedence to normalized and original text
      params.set("qf", "normalized original mss_title_searchable playwright_name_searchable play_title_searchable speaker_name_searchable");
   }

   @Override
   public Class<ExtractSearchProxy> getSearchProxyType()
   {
      return ExtractSearchProxy.class;
   }

   @Override
   public Class<ExtractDocument> getIndexDocumentType()
   {
      return ExtractDocument.class;
   }

   @Override
   public Collection<? extends SolrIndexField<?>> getIndexedFields()
   {
      return Arrays.asList(ID,
            MANUSCRIPT_ID,
            MANUSCRIPT_FACET,
            MANUSCRIPT_TITLE,
            NORMALIZED,
            ORIGINAL,
            PLAYWRIGHT_ID,
            PLAYWRIGHT_FACET,
            PLAYWRIGHT_NAME,
            PLAY_ID,
            PLAY_FACET,
            PLAY_TITLE,
            SPEAKER_ID,
            SPEAKER_FACET,
            SPEAKER_NAME);
   }

   @Override
   public Collection<? extends SolrIndexField<?>> getStoredFields()
   {
      return Arrays.asList(ID,
            SEARCH_PROXY,
            NORMALIZED,
            ORIGINAL);
   }

   @Override
   public Collection<? extends SolrIndexField<?>> getMultiValuedFields()
   {
      return Arrays.asList(PLAYWRIGHT_ID,
            PLAYWRIGHT_FACET,
            PLAYWRIGHT_NAME,
            PLAY_ID,
            PLAY_FACET,
            PLAY_TITLE,
            SPEAKER_ID,
            SPEAKER_FACET,
            SPEAKER_NAME);
   }

}
