package edu.tamu.tcat.trc.extract.search.solr;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.solr.common.SolrInputDocument;

import edu.tamu.tcat.dex.trc.entry.DramaticExtract;
import edu.tamu.tcat.dex.trc.entry.PlaywrightRef;
import edu.tamu.tcat.dex.trc.entry.SourceRef;
import edu.tamu.tcat.dex.trc.entry.SpeakerRef;
import edu.tamu.tcat.dex.trc.entry.tei.transform.ExtractManipulationException;
import edu.tamu.tcat.dex.trc.entry.tei.transform.ExtractManipulationUtil;
import edu.tamu.tcat.trc.search.SearchException;
import edu.tamu.tcat.trc.search.solr.impl.TrcDocument;

public class ExtractDocument
{
   private static final Logger logger = Logger.getLogger(ExtractDocument.class.getName());

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
      doc.document.set(ExtractSolrConfig.MANUSCRIPT_TITLE, extract.getManuscriptRef().getDisplayTitle());

      try
      {
         doc.document.set(ExtractSolrConfig.MANUSCRIPT_FACET, facetValueManipulationUtil.getWorkFacetValue(manuscriptId));
      }
      catch (FacetValueException e)
      {
         // TODO: this message needs to be sent to the user
         logger.log(Level.WARNING, "Unable to resolve facet value for manuscript [" + manuscriptId +"]. Faceting by this manuscript will not be available for extract [" + extract.getId() + "].", e);
      }

      try
      {
         doc.document.set(ExtractSolrConfig.NORMALIZED, extractManipulationUtil.toNormalized(extract.getTEIContent()));
      }
      catch (ExtractManipulationException e)
      {
         // TODO: this message needs to be sent to the user
         logger.log(Level.WARNING, "Unable to get normalized text for extract [" + extract.getId() + "].", e);
      }
      try
      {
         doc.document.set(ExtractSolrConfig.ORIGINAL, extractManipulationUtil.toOriginal(extract.getTEIContent()));
      }
      catch (ExtractManipulationException e)
      {
         // TODO: this message needs to be sent to the user
         logger.log(Level.WARNING, "Unable to get original text for extract [" + extract.getId() + "].", e);
      }

      for (PlaywrightRef ref : extract.getPlaywrightRefs())
      {
         String playwrightId = ref.getId();
         doc.document.set(ExtractSolrConfig.PLAYWRIGHT_ID, playwrightId);
         doc.document.set(ExtractSolrConfig.PLAYWRIGHT_NAME, ref.getDisplayName());
         try
         {
            doc.document.set(ExtractSolrConfig.PLAYWRIGHT_FACET, facetValueManipulationUtil.getPersonFacetValue(playwrightId));
         }
         catch (FacetValueException e)
         {
            // TODO: this message needs to be sent to the user
            logger.log(Level.WARNING, "Unable to resolve facet value for playwright [" + playwrightId + "]. Faceting by this playwright wil not be available for extract [" + extract.getId() + "].", e);
         }
      }

      SourceRef playSource = extract.getSource();
      String playId = playSource.getId();
      doc.document.set(ExtractSolrConfig.PLAY_ID, playId);
      doc.document.set(ExtractSolrConfig.PLAY_TITLE, playSource.getDisplayTitle());
      try
      {
         doc.document.set(ExtractSolrConfig.PLAY_FACET, facetValueManipulationUtil.getWorkFacetValue(playId));
      }
      catch (FacetValueException e)
      {
         // TODO: this message needs to be sent to the user
         logger.log(Level.WARNING, "Unable to resolve facet value for play [" + playId + "]. Faceting by this play will not be available for extract [" + extract.getId() + "].", e);
      }

      for (SpeakerRef ref : extract.getSpeakerRefs())
      {
         String speakerId = ref.getId();
         doc.document.set(ExtractSolrConfig.SPEAKER_ID, speakerId);
         doc.document.set(ExtractSolrConfig.SPEAKER_NAME, ref.getDisplayName());
         try
         {
            doc.document.set(ExtractSolrConfig.SPEAKER_FACET, facetValueManipulationUtil.getPersonFacetValue(speakerId));
         }
         catch (FacetValueException e)
         {
            // TODO: this message needs to be sent to the user
            logger.log(Level.WARNING, "Unable to resolve facet value for speaker [" + speakerId + "]. Faceting by this speaker will not be available for extract [" + extract.getId() + "].", e);
         }
      }

      doc.document.set(ExtractSolrConfig.SEARCH_PROXY, ExtractSearchProxy.create(extract, extractManipulationUtil));

      return doc;
   }

   public SolrInputDocument getDocument()
   {
      return document.getSolrDocument();
   }
}
