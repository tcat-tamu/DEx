package edu.tamu.tcat.trc.extract.search.solr;

import java.util.List;

import edu.tamu.tcat.trc.extract.search.ExtractQueryCommand;
import edu.tamu.tcat.trc.extract.search.SearchExtractResult;

public class SolrExtractsResults implements SearchExtractResult
{
   private final ExtractQueryCommand command;
   private final List<ExtractSearchProxy> extracts;
   private final long numFound;


   public SolrExtractsResults(ExtractQueryCommand command, List<ExtractSearchProxy> extracts, long numFound)
   {
      this.command = command;
      this.extracts = extracts;
      this.numFound = numFound;
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

}
