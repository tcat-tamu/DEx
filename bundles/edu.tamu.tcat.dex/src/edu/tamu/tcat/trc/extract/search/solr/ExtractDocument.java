package edu.tamu.tcat.trc.extract.search.solr;

import org.apache.solr.common.SolrInputDocument;

import edu.tamu.tcat.dex.trc.entry.DramaticExtract;
import edu.tamu.tcat.dex.trc.entry.PlaywrightRef;
import edu.tamu.tcat.dex.trc.entry.SpeakerRef;
import edu.tamu.tcat.dex.trc.entry.tei.transform.ExtractManipulationException;
import edu.tamu.tcat.dex.trc.entry.tei.transform.ExtractManipulationUtil;
import edu.tamu.tcat.trc.entries.search.SearchException;
import edu.tamu.tcat.trc.entries.search.solr.impl.TrcDocument;

public class ExtractDocument
{
   private TrcDocument document;

   protected ExtractDocument()
   {
      document = new TrcDocument(new ExtractSolrConfig());
   }


   public static ExtractDocument create(DramaticExtract extract, ExtractManipulationUtil extractManipulationUtil) throws SearchException
   {
      ExtractDocument doc = new ExtractDocument();

      doc.document.set(ExtractSolrConfig.ID, extract.getId());

      doc.document.set(ExtractSolrConfig.MS_SHELFMARK, extract.getManuscriptRef().getId());
      doc.document.set(ExtractSolrConfig.MS_TITLE, extract.getManuscriptRef().getDisplayTitle());

      try
      {
         doc.document.set(ExtractSolrConfig.NORMALIZED, extractManipulationUtil.toNormalized(extract.getTEIContent()));
         doc.document.set(ExtractSolrConfig.ORIGINAL, extractManipulationUtil.toOriginal(extract.getTEIContent()));
      }
      catch (ExtractManipulationException e)
      {
         throw new IllegalStateException("Unable to get normalized or original text for Solr proxy", e);
      }

      for (PlaywrightRef ref : extract.getPlaywrightRefs())
      {
         doc.document.set(ExtractSolrConfig.PLAYWRIGHT_ID, ref.getId());
         doc.document.set(ExtractSolrConfig.PLAYWRIGHT_NAME, ref.getDisplayName());
      }

      for (SpeakerRef ref : extract.getSpeakerRefs())
      {
         doc.document.set(ExtractSolrConfig.SPEAKER_ID, ref.getId());
         doc.document.set(ExtractSolrConfig.SPEAKER_NAME, ref.getDisplayName());
      }

      doc.document.set(ExtractSolrConfig.SEARCH_PROXY, ExtractSearchProxy.create(extract, extractManipulationUtil));

      return doc;
   }

   public SolrInputDocument getDocument()
   {
      return document.getSolrDocument();
   }
}
