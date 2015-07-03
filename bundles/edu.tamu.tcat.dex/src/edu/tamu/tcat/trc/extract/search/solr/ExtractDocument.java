package edu.tamu.tcat.trc.extract.search.solr;

import org.apache.solr.common.SolrInputDocument;

import edu.tamu.tcat.dex.trc.entry.DramaticExtract;
import edu.tamu.tcat.dex.trc.entry.PlaywrightRef;
import edu.tamu.tcat.dex.trc.entry.SourceRef;
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


   public static ExtractDocument create(DramaticExtract extract, ExtractManipulationUtil extractManipulationUtil, FacetValueManipulationUtil facetValueManipulationUtil) throws SearchException
   {
      ExtractDocument doc = new ExtractDocument();

      doc.document.set(ExtractSolrConfig.ID, extract.getId());

      String manuscriptId = extract.getManuscriptRef().getId();
      doc.document.set(ExtractSolrConfig.MANUSCRIPT_ID, manuscriptId);
      doc.document.set(ExtractSolrConfig.MANUSCRIPT_FACET, facetValueManipulationUtil.getWorkFacetValue(manuscriptId));
      doc.document.set(ExtractSolrConfig.MANUSCRIPT_TITLE, extract.getManuscriptRef().getDisplayTitle());

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
         String playwrightId = ref.getId();
         doc.document.set(ExtractSolrConfig.PLAYWRIGHT_ID, playwrightId);
         doc.document.set(ExtractSolrConfig.PLAYWRIGHT_FACET, facetValueManipulationUtil.getPersonFacetValue(playwrightId));
         doc.document.set(ExtractSolrConfig.PLAYWRIGHT_NAME, ref.getDisplayName());
      }

      SourceRef playSource = extract.getSource();
      String playId = playSource.getId();
      doc.document.set(ExtractSolrConfig.PLAY_ID, playId);
      doc.document.set(ExtractSolrConfig.PLAY_FACET, facetValueManipulationUtil.getWorkFacetValue(playId));
      doc.document.set(ExtractSolrConfig.PLAY_TITLE, playSource.getDisplayTitle());

      for (SpeakerRef ref : extract.getSpeakerRefs())
      {
         String speakerId = ref.getId();
         doc.document.set(ExtractSolrConfig.SPEAKER_ID, speakerId);
         doc.document.set(ExtractSolrConfig.SPEAKER_FACET, facetValueManipulationUtil.getPersonFacetValue(speakerId));
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
