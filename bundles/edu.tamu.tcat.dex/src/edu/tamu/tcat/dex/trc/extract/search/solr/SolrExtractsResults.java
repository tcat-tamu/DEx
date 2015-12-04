package edu.tamu.tcat.dex.trc.extract.search.solr;

import java.util.Collection;
import java.util.List;

import edu.tamu.tcat.dex.trc.extract.search.ExtractQueryCommand;
import edu.tamu.tcat.dex.trc.extract.search.FacetItemList;
import edu.tamu.tcat.dex.trc.extract.search.SearchExtractResult;

public class SolrExtractsResults implements SearchExtractResult
{
   private final ExtractQueryCommand command;
   private final List<ExtractSearchProxy> extracts;
   private final long numFound;
   private final Collection<FacetItemList> facets;


   public SolrExtractsResults(ExtractQueryCommand command, List<ExtractSearchProxy> extracts, long numFound, Collection<FacetItemList> facets)
   {
      this.command = command;
      this.extracts = extracts;
      this.numFound = numFound;
      this.facets = facets;
   }

   @Override
   public ExtractQueryCommand getCommand()
   {
      return command;
   }

   @Override
   public List<ExtractSearchProxy> get()
   {
      return extracts;
   }

   @Override
   public long getNumFound()
   {
      return numFound;
   }

   @Override
   public Collection<FacetItemList> getFacets()
   {
      return facets;
   }
}
