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
   public static final SolrIndexField<String> MANUSCRIPT_TITLE = new BasicFields.BasicString("mss_title");
   public static final SolrIndexField<String> MANUSCRIPT_TITLE_SEARCHABLE = new BasicFields.BasicString("mss_title_searchable");
   public static final SolrIndexField<String> NORMALIZED = new BasicFields.BasicString("normalized");
   public static final SolrIndexField<String> ORIGINAL = new BasicFields.BasicString("original");
   public static final SolrIndexField<String> PLAYWRIGHT_ID = new BasicFields.BasicString("playwright_id");
   public static final SolrIndexField<String> PLAYWRIGHT_NAME = new BasicFields.BasicString("playwright_name");
   public static final SolrIndexField<String> PLAYWRIGHT_NAME_SEARCHABLE = new BasicFields.BasicString("playwright_name_searchable");
   public static final SolrIndexField<String> PLAY_ID = new BasicFields.BasicString("play_id");
   public static final SolrIndexField<String> PLAY_TITLE = new BasicFields.BasicString("play_title");
   public static final SolrIndexField<String> PLAY_TITLE_SEARCHABLE = new BasicFields.BasicString("play_title_searchable");
   public static final SolrIndexField<String> SPEAKER_ID = new BasicFields.BasicString("speaker_id");
   public static final SolrIndexField<String> SPEAKER_NAME = new BasicFields.BasicString("speaker_name");
   public static final SolrIndexField<String> SPEAKER_NAME_SEARCHABLE = new BasicFields.BasicString("speaker_name_searchable");

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
      params.addFacetField(MANUSCRIPT_TITLE.getName());
      params.addFacetField(PLAYWRIGHT_NAME.getName());
      params.addFacetField(PLAY_TITLE.getName());
      params.addFacetField(SPEAKER_NAME.getName());
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
            MANUSCRIPT_TITLE,
            NORMALIZED,
            ORIGINAL,
            PLAYWRIGHT_ID,
            PLAYWRIGHT_NAME,
            PLAY_ID,
            PLAY_TITLE,
            SPEAKER_ID,
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
            PLAYWRIGHT_NAME,
            PLAY_ID,
            PLAY_TITLE,
            SPEAKER_ID,
            SPEAKER_NAME);
   }

}
