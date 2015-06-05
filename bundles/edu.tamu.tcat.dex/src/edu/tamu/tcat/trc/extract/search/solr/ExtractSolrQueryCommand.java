package edu.tamu.tcat.trc.extract.search.solr;

import java.util.List;

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
         // TODO: add lazy-evaluated parameters to query builder

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
      queryBuilder.basic(basicQueryString);
   }

   @Override
   public void queryAll() throws SearchException
   {
      queryBuilder.basic("*:*");
   }

   // TODO: add methods to tweak parameters on the query builder

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
