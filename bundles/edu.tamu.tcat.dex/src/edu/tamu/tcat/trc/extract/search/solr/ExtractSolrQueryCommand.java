package edu.tamu.tcat.trc.extract.search.solr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import edu.tamu.tcat.trc.entries.search.SearchException;
import edu.tamu.tcat.trc.entries.search.solr.impl.TrcQueryBuilder;
import edu.tamu.tcat.trc.extract.search.ExtractQueryCommand;
import edu.tamu.tcat.trc.extract.search.SearchExtractResult;

public class ExtractSolrQueryCommand implements ExtractQueryCommand
{
   private static final int DEFAULT_MAX_RESULTS = 25;

   private final SolrServer solrServer;
   private final TrcQueryBuilder queryBuilder;

   private Collection<String> manuscriptIds = new ArrayList<>();
   private Collection<String> playwrightIds = new ArrayList<>();
   private Collection<String> playIds = new ArrayList<>();
   private Collection<String> speakerIds = new ArrayList<>();


   public ExtractSolrQueryCommand(SolrServer solrServer, TrcQueryBuilder queryBuilder)
   {
      this.solrServer = solrServer;
      this.queryBuilder = queryBuilder;
      queryBuilder.max(DEFAULT_MAX_RESULTS);
   }


   @Override
   public SearchExtractResult execute() throws SearchException
   {
      try
      {
         if (!manuscriptIds.isEmpty())
         {
            queryBuilder.filterMulti(ExtractSolrConfig.MANUSCRIPT_ID, manuscriptIds);
         }

         if (!playwrightIds.isEmpty())
         {
            queryBuilder.filterMulti(ExtractSolrConfig.PLAYWRIGHT_ID, playwrightIds);
         }

         if (!playIds.isEmpty())
         {
            queryBuilder.filterMulti(ExtractSolrConfig.PLAY_ID, playIds);
         }

         if (!speakerIds.isEmpty())
         {
            queryBuilder.filterMulti(ExtractSolrConfig.SPEAKER_ID, speakerIds);
         }

         QueryResponse response = solrServer.query(queryBuilder.get());
         SolrDocumentList results = response.getResults();

         long totalFound = results.getNumFound();
         List<ExtractSearchProxy> extracts = queryBuilder.unpack(results, ExtractSolrConfig.SEARCH_PROXY);
         return new SolrExtractsResults(this, extracts, totalFound);
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
   public void queryManuscript(String manuscriptQuery) throws SearchException
   {
      queryBuilder.query(ExtractSolrConfig.MANUSCRIPT_TITLE_SEARCHABLE, manuscriptQuery);
   }

   @Override
   public void filterManuscript(Collection<String> manuscriptIds) throws SearchException
   {
      this.manuscriptIds.addAll(manuscriptIds);
   }

   @Override
   public void queryPlaywright(String playwrightQuery) throws SearchException
   {
      queryBuilder.query(ExtractSolrConfig.PLAYWRIGHT_NAME_SEARCHABLE, playwrightQuery);
   }

   @Override
   public void filterPlaywright(Collection<String> playwrightIds) throws SearchException
   {
      this.playwrightIds.addAll(playwrightIds);
   }

   @Override
   public void queryPlay(String playQuery) throws SearchException
   {
      queryBuilder.query(ExtractSolrConfig.PLAY_TITLE_SEARCHABLE, playQuery);
   }

   @Override
   public void filterPlay(Collection<String> playIds) throws SearchException
   {
      this.playIds.addAll(playIds);
   }

   @Override
   public void querySpeaker(String speakerQuery) throws SearchException
   {
      queryBuilder.query(ExtractSolrConfig.SPEAKER_NAME_SEARCHABLE, speakerQuery);
   }

   @Override
   public void filterSpeaker(Collection<String> speakerIds) throws SearchException
   {
      this.speakerIds.addAll(speakerIds);
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

}
