package edu.tamu.tcat.dex.trc.extract.search.solr;

import java.awt.datatransfer.StringSelection;
import java.text.MessageFormat;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.solr.common.SolrInputDocument;
import org.w3c.dom.Document;

import edu.tamu.tcat.dex.trc.extract.DramaticExtract;
import edu.tamu.tcat.dex.trc.extract.PlaywrightRef;
import edu.tamu.tcat.dex.trc.extract.SourceRef;
import edu.tamu.tcat.dex.trc.extract.SpeakerRef;
import edu.tamu.tcat.trc.search.SearchException;
import edu.tamu.tcat.trc.search.solr.impl.TrcDocument;

/**
 * Generates a searchable representation of a {@link DramaticExtract} for indexing using Solr.
 */
public class SearchableDocumentFactory
{
   private static final Logger logger = Logger.getLogger(SearchableDocumentFactory.class.getName());
   
   private final ExtractManipulationUtil parser;
   private final FacetValueManipulationUtil facetUtil;

   /**
    *
    * @param parser Parses the TEI representation of the extract's content into normalized and
    *    original versions.
    * @param facetUtil Used to retrieve structured representations of related resources (e.g,
    *    plays, playwrights, manuscripts) to support facetted searching.
    */
   protected SearchableDocumentFactory(ExtractManipulationUtil parser, FacetValueManipulationUtil facetUtil)
   {
      this.parser = parser;
      this.facetUtil = facetUtil;
   }

   public SolrInputDocument create(DramaticExtract extract)
   {
      return create(extract, new IndexCreationProblems(extract.getId()));
   }

   public SolrInputDocument create(DramaticExtract extract, IndexCreationProblems monitor)
   {

      try
      {
         TrcDocument document = new TrcDocument(new ExtractSolrConfig());
         document.set(ExtractSolrConfig.ID, extract.getId());
         
         addManuscriptDetail(document, extract, monitor);
         addTransformedContent(document, extract, monitor);
         addPlaywrights(document, extract, monitor);
         addPlayDetails(document, extract, monitor);
         addSpeakerReferences(document, extract, monitor);
         addProxy(document, extract, monitor);

         return document.getSolrDocument();
      }
      catch (SearchException ex)
      {
         String searchErrMsg = "Internal Error. Unable to create search index record for extract [{0}].";
         throw new IllegalStateException(MessageFormat.format(searchErrMsg, extract.getId()), ex);
      }
   }

   private void addProxy(TrcDocument document, DramaticExtract extract, IndexCreationProblems problems) throws SearchException
   {
      ExtractSearchProxy proxy = ExtractSearchProxy.create(extract, parser);
      document.set(ExtractSolrConfig.SEARCH_PROXY, proxy);
   }

   private void addManuscriptDetail(TrcDocument document, DramaticExtract extract, IndexCreationProblems problems) throws SearchException
   {
      String manuscriptId = extract.getManuscriptRef().getId();
      try
      {
         document.set(ExtractSolrConfig.MANUSCRIPT_ID, manuscriptId);
         document.set(ExtractSolrConfig.MANUSCRIPT_TITLE, extract.getManuscriptRef().getDisplayTitle());
         document.set(ExtractSolrConfig.MANUSCRIPT_INDEX, extract.getManuscriptIndex());
         document.set(ExtractSolrConfig.MANUSCRIPT_FACET, facetUtil.getWorkFacetValue(manuscriptId));
      }
      catch (FacetValueException e)
      {
         problems.addMsFacetWarning(manuscriptId);

      }
   }

   private void addTransformedContent(TrcDocument document, DramaticExtract extract, IndexCreationProblems problems) throws SearchException
   {
      String normalized = "no content";
      String original = "no content";
      Document content = extract.getTEIContent();
      try
      {
         normalized = parser.toNormalized(content);
         original = parser.toOriginal(content);
         if (original == null) 
            logger.log(Level.INFO, "Failed to parse original content from " + content);

      }
      catch (Exception e)
      {
         logger.log(Level.WARNING, "Failed to parse content" + content);
         problems.addParseError(content.toString());
      }
      
      document.set(ExtractSolrConfig.NORMALIZED, normalized);
      document.set(ExtractSolrConfig.ORIGINAL, original);
   }

   private void addPlaywrights(TrcDocument document, DramaticExtract extract, IndexCreationProblems problems) throws SearchException
   {
      for (PlaywrightRef ref : extract.getPlaywrightRefs())
      {
         String playwrightId = ref.getId();
         try
         {
            document.set(ExtractSolrConfig.PLAYWRIGHT_ID, playwrightId);
            document.set(ExtractSolrConfig.PLAYWRIGHT_NAME, ref.getDisplayName());
            document.set(ExtractSolrConfig.PLAYWRIGHT_FACET, facetUtil.getPersonFacetValue(playwrightId));
         }
         catch (FacetValueException e)
         {
            problems.addPlaywrightFacetWarning(playwrightId);
         }
      }
   }

   private void addPlayDetails(TrcDocument document, DramaticExtract extract, IndexCreationProblems problems) throws SearchException
   {
      SourceRef playSource = extract.getSource();
      String playId = playSource.getId();

      try
      {
         document.set(ExtractSolrConfig.PLAY_ID, playId);
         document.set(ExtractSolrConfig.PLAY_TITLE, playSource.getDisplayTitle());
         if (playId != null)
         {
            String workFacetValue = facetUtil.getWorkFacetValue(playId);
            document.set(ExtractSolrConfig.PLAY_FACET, workFacetValue);
         }

         // TODO check externally to see if play id is null - this is an integrity check on the supplied extract
      }
      catch (FacetValueException e)
      {
         problems.addPlayFacetWarning(playId);
      }
   }

   private void addSpeakerReferences(TrcDocument document, DramaticExtract extract, IndexCreationProblems problems) throws SearchException
   {
      Set<SpeakerRef> speakers = extract.getSpeakerRefs();
      for (SpeakerRef ref : speakers)
      {
         String speakerId = ref.getId();
         try
         {
            document.set(ExtractSolrConfig.SPEAKER_ID, speakerId);
            document.set(ExtractSolrConfig.SPEAKER_NAME, ref.getDisplayName());
            document.set(ExtractSolrConfig.SPEAKER_FACET, facetUtil.getPersonFacetValue(speakerId));
         }
         catch (FacetValueException e)
         {
            problems.addSpeakerFacetWarning(speakerId);
         }
      }
   }
}
