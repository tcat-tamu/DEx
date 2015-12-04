package edu.tamu.tcat.dex.trc.extract.search.solr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import edu.tamu.tcat.dex.trc.extract.search.ExtractQueryCommand;
import edu.tamu.tcat.dex.trc.extract.search.FacetItemList;
import edu.tamu.tcat.dex.trc.extract.search.SearchExtractResult;
import edu.tamu.tcat.dex.trc.extract.search.FacetItemList.FacetItem;
import edu.tamu.tcat.dex.trc.extract.search.solr.FacetValueManipulationUtil.FacetValue;
import edu.tamu.tcat.trc.search.SearchException;
import edu.tamu.tcat.trc.search.solr.impl.TrcQueryBuilder;


public class ExtractSolrQueryCommand implements ExtractQueryCommand
{
   private static final int DEFAULT_MAX_RESULTS = 25;

   private static final Logger logger = Logger.getLogger(ExtractSolrQueryCommand.class.getName());

   private final SolrServer solrServer;
   private final TrcQueryBuilder queryBuilder;

   private Collection<String> manuscriptIds = new ArrayList<>();
   private Collection<String> playwrightIds = new ArrayList<>();
   private Collection<String> playIds = new ArrayList<>();
   private Collection<String> speakerIds = new ArrayList<>();

   private final FacetValueManipulationUtil facetValueManipulationUtil;


   public ExtractSolrQueryCommand(SolrServer solrServer,
                                  TrcQueryBuilder queryBuilder,
                                  FacetValueManipulationUtil facetValueManipulationUtil)
   {
      this.solrServer = solrServer;
      this.queryBuilder = queryBuilder;
      this.facetValueManipulationUtil = facetValueManipulationUtil;
      queryBuilder.max(DEFAULT_MAX_RESULTS);
   }


   @Override
   public SearchExtractResult execute() throws SearchException
   {
      try
      {
         if (!manuscriptIds.isEmpty())
         {
            queryBuilder.filterMulti(ExtractSolrConfig.MANUSCRIPT_FACET, manuscriptIds, ExtractSolrConfig.FACET_EXCLUDE_TAG_MANUSCRIPT);
         }

         if (!playwrightIds.isEmpty())
         {
            queryBuilder.filterMulti(ExtractSolrConfig.PLAYWRIGHT_FACET, playwrightIds, ExtractSolrConfig.FACET_EXCLUDE_TAG_PLAYWRIGHT);
         }

         if (!playIds.isEmpty())
         {
            queryBuilder.filterMulti(ExtractSolrConfig.PLAY_FACET, playIds, ExtractSolrConfig.FACET_EXCLUDE_TAG_PLAY);
         }

         if (!speakerIds.isEmpty())
         {
            queryBuilder.filterMulti(ExtractSolrConfig.SPEAKER_FACET, speakerIds, ExtractSolrConfig.FACET_EXCLUDE_TAG_SPEAKER);
         }

         SolrQuery solrParams = (SolrQuery)queryBuilder.get();

         // for a single MS, sort results by the order they appear in the manuscript
         if (manuscriptIds.size() == 1) {
            solrParams.addSort(ExtractSolrConfig.MANUSCRIPT_INDEX.getName(), ORDER.asc);
         }

         QueryResponse response = solrServer.query(solrParams);
         SolrDocumentList results = response.getResults();

         Map<String, Collection<String>> selectedFacets = new HashMap<>();
         selectedFacets.put(ExtractSolrConfig.MANUSCRIPT_FACET.getName(), manuscriptIds);
         selectedFacets.put(ExtractSolrConfig.PLAYWRIGHT_FACET.getName(), playwrightIds);
         selectedFacets.put(ExtractSolrConfig.PLAY_FACET.getName(), playIds);
         selectedFacets.put(ExtractSolrConfig.SPEAKER_FACET.getName(), speakerIds);

         Collection<FacetItemList> facets = response.getFacetFields().parallelStream()
               .map(solrField -> FacetItemListImpl.fromSolr(solrField, selectedFacets.getOrDefault(solrField.getName(), Collections.emptyList())))
               .collect(Collectors.toList());

         long totalFound = results.getNumFound();
         List<ExtractSearchProxy> extracts = queryBuilder.unpack(results, ExtractSolrConfig.SEARCH_PROXY);
         return new SolrExtractsResults(this, extracts, totalFound, facets);
      }
      catch (SolrServerException e)
      {
         throw new SearchException("An error occurred while querying the works core.", e);
      }
   }

   @Override
   public void query(String basicQueryString) throws SearchException
   {
      Objects.requireNonNull(basicQueryString, "Null query string provided");
      if (basicQueryString.isEmpty())
      {
         basicQueryString = "*:*";
      }

      queryBuilder.basic(basicQueryString);
   }

   @Override
   public void advancedQuery(String advancedQueryString) throws SearchException
   {
      queryBuilder.query(ExtractSolrConfig.ORIGINAL, advancedQueryString);
      queryBuilder.query(ExtractSolrConfig.NORMALIZED, advancedQueryString);
   }

   @Override
   public void queryManuscript(String manuscriptQuery) throws SearchException
   {
      queryBuilder.query(ExtractSolrConfig.MANUSCRIPT_TITLE_SEARCHABLE, manuscriptQuery);
   }

   @Override
   public void filterManuscript(Collection<String> manuscriptIds) throws SearchException
   {
      Collection<String> facetValues = manuscriptIds.stream()
         .map(this::getWorkFacetValue)
         .filter(Objects::nonNull)
         .collect(Collectors.toList());

      this.manuscriptIds.addAll(facetValues);
   }

   @Override
   public void queryPlaywright(String playwrightQuery) throws SearchException
   {
      queryBuilder.query(ExtractSolrConfig.PLAYWRIGHT_NAME_SEARCHABLE, playwrightQuery);
   }

   @Override
   public void filterPlaywright(Collection<String> playwrightIds) throws SearchException
   {
      Collection<String> facetValues = playwrightIds.stream()
            .map(this::getPersonFacetValue)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

      this.playwrightIds.addAll(facetValues);
   }

   @Override
   public void queryPlay(String playQuery) throws SearchException
   {
      queryBuilder.query(ExtractSolrConfig.PLAY_TITLE_SEARCHABLE, playQuery);
   }

   @Override
   public void filterPlay(Collection<String> playIds) throws SearchException
   {
      Collection<String> facetValues = playIds.stream()
            .map(this::getWorkFacetValue)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

      this.playIds.addAll(facetValues);
   }

   @Override
   public void querySpeaker(String speakerQuery) throws SearchException
   {
      queryBuilder.query(ExtractSolrConfig.SPEAKER_NAME_SEARCHABLE, speakerQuery);
   }

   @Override
   public void filterSpeaker(Collection<String> speakerIds) throws SearchException
   {
      Collection<String> facetValues = speakerIds.stream()
         .map(this::getPersonFacetValue)
         .filter(Objects::nonNull)
         .collect(Collectors.toList());

      this.speakerIds.addAll(facetValues);
   }

   @Override
   public void setOffset(int offset)
   {
      if (offset < 0)
      {
         throw new IllegalArgumentException("Offest cannot be negative.");
      }

      queryBuilder.offset(offset);
   }

   @Override
   public void setMaxResults(int count)
   {
      if (count < 0)
      {
         count = Integer.MAX_VALUE;
      }

      queryBuilder.max(count);
   }

   @Override
   public void setMaxFacets(int count)
   {
      if (count < 0)
      {
         count = Integer.MAX_VALUE;
      }

      queryBuilder.facetLimit(count);
   }

   /**
    * Delegates to {@link FacetValueManipulationUtil}; returns null and logs error if thrown
    *
    * @param id
    * @return
    */
   private String getWorkFacetValue(String id)
   {
      try
      {
         return facetValueManipulationUtil.getWorkFacetValue(id);
      }
      catch (FacetValueException e)
      {
         logger.log(Level.WARNING, "unable to look up facet value for work [" + id + "].", e);
         return null;
      }
   }

   /**
    * Delegates to {@link FacetValueManipulationUtil}; returns null and logs error if thrown
    *
    * @param id
    * @return
    */
   private String getPersonFacetValue(String id)
   {
      try
      {
         return facetValueManipulationUtil.getPersonFacetValue(id);
      }
      catch (FacetValueException e)
      {
         logger.log(Level.WARNING, "unable to look up facet value for person [" + id + "].", e);
         return null;
      }
   }

   private static class FacetItemListImpl implements FacetItemList
   {
      private final String fieldName;
      private final Supplier<List<FacetItem>> values;

      public static FacetItemListImpl fromSolr(FacetField solrField, Collection<String> selectedItems)
      {
         Supplier<List<FacetItem>> valuesSupplier = () ->
            {
               // add items returned by Solr
               List<FacetItem> values = solrField.getValues().stream()
                     .map(solrItem -> FacetItemImpl.fromSolr(solrItem, selectedItems.remove(solrItem.getName())))
                     .collect(Collectors.toList());

               // add remaining selected items
               selectedItems.stream()
                     .map(selectedItem -> FacetItemImpl.fromFacetValue(selectedItem, 0, true))
                     .forEach(values::add);

               return values;
            };

         return new FacetItemListImpl(solrField.getName(), valuesSupplier);
      }

      private FacetItemListImpl(String fieldName, Supplier<List<FacetItem>> values)
      {
         this.fieldName = fieldName;
         this.values = values;
      }

      @Override
      public String getFieldName()
      {
         return fieldName;
      }

      @Override
      public List<FacetItem> getValues()
      {
         return values.get();
      }
   }

   private static class FacetItemImpl implements FacetItem
   {
      private final String id;
      private final String label;
      private final long count;
      private final boolean selected;

      public static FacetItemImpl fromSolr(Count solrCount, boolean selected)
      {
         return fromFacetValue(solrCount.getName(), solrCount.getCount(), selected);
      }

      public static FacetItemImpl fromFacetValue(String facetValue, long count, boolean selected)
      {
         FacetValue splitValue = FacetValueManipulationUtil.splitFacetValue(facetValue);
         return new FacetItemImpl(splitValue.id, splitValue.label, count, selected);
      }

      private FacetItemImpl(String id, String label, long count, boolean selected)
      {
         this.id = id;
         this.label = label;
         this.count = count;
         this.selected = selected;
      }

      @Override
      public String getId()
      {
         return id;
      }

      @Override
      public String getLabel()
      {
         return label;
      }

      @Override
      public long getCount()
      {
         return count;
      }

      @Override
      public boolean isSelected()
      {
         return selected;
      }
   }
}
