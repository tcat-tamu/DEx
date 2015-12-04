package edu.tamu.tcat.dex.trc.extract.search.solr;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.solr.common.SolrInputDocument;
import org.w3c.dom.Document;

import edu.tamu.tcat.dex.trc.entry.DramaticExtract;
import edu.tamu.tcat.dex.trc.entry.PlaywrightRef;
import edu.tamu.tcat.dex.trc.entry.SourceRef;
import edu.tamu.tcat.dex.trc.entry.SpeakerRef;
import edu.tamu.tcat.dex.trc.entry.tei.transform.ExtractManipulationException;
import edu.tamu.tcat.dex.trc.entry.tei.transform.ExtractManipulationUtil;
import edu.tamu.tcat.trc.search.SearchException;
import edu.tamu.tcat.trc.search.solr.impl.TrcDocument;

/**
 * Generates a searchable representation of a {@link DramaticExtract} for indexing using Solr.
 */
public class SearchableDocumentFactory
{
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
      return create(extract, warnings -> {});
   }

   public SolrInputDocument create(DramaticExtract extract, Consumer<IndexCreationProblems> errorMonitor)
   {
      SolrDocumentBuilder builder = new SolrDocumentBuilder(extract);

      SolrInputDocument doc = builder.build();
      if (!builder.problems.isEmpty())
      {
         errorMonitor.accept(builder.problems);
      }

      return doc;
   }

   public static class IndexCreationProblems
   {
      private final String extractId;

      private Map<String, String> warnings = new HashMap<>();

      IndexCreationProblems(String extractId) {
         this.extractId = extractId;
      }

      public boolean isEmpty()
      {
         return warnings.isEmpty();
      }

      public void addMsFacetWarning(String manuscriptId)
      {
         String facetErrorMsg = "Unable to resolve facet value for manuscript [{0}]. Faceting by this manuscript will not be available for extract [{1}].";
         warnings.put(manuscriptId, MessageFormat.format(facetErrorMsg, manuscriptId, extractId));
      }

      public void addPlayFacetWarning(String playId)
      {
         String facetErrMsg = "Unable to resolve facet value for play [{0}]. Faceting by this play will not be available for extract [{1}].";
         String msg = MessageFormat.format(facetErrMsg, playId, extractId);
         warnings.put(playId, msg);
      }

      public void addSpeakerFacetWarning(String speakerId)
      {
         String facetErrMsg = "Unable to resolve facet value for speaker [{0}]. Faceting by this speaker will not be available for extract [{1}].";
         String msg = MessageFormat.format(facetErrMsg, speakerId, extractId);
         warnings.put(speakerId, msg);
      }

      public void addPlaywrightFacetWarning(String playwrightId)
      {
         String facetErrMsg = "Unable to resolve facet value for playwright [{0}]. Faceting by this playwright wil not be available for extract [{1}].";
         String msg = MessageFormat.format(facetErrMsg, playwrightId, extractId);
         warnings.put(playwrightId, msg);
      }

      public void addParseError(String content)
      {
         String parseErrorMsg= "Unable to parse extract text for [{0}].\n\n{1}";
         warnings.put(extractId, MessageFormat.format(parseErrorMsg, extractId, content));
      }
   }
   private class SolrDocumentBuilder
   {
      // TODO add error report monitor
      private final TrcDocument document;
      private final DramaticExtract extract;

      private final IndexCreationProblems problems;

      public SolrDocumentBuilder(DramaticExtract extract)
      {
         this.extract = extract;
         this.document = new TrcDocument(new ExtractSolrConfig());

         this.problems = new IndexCreationProblems(extract.getId());
      }

      public SolrInputDocument build() throws IllegalStateException
      {
         // TODO expose warnings
         try
         {
            document.set(ExtractSolrConfig.ID, extract.getId());
            addManuscriptDetail(extract);
            addTransformedContent(extract);
            addPlaywrights(extract);
            addPlayDetails(extract);
            addSpeakerReferences(extract);
            addProxy();

            return document.getSolrDocument();
         }
         catch (SearchException ex)
         {
            String searchErrMsg = "Internal Error. Unable to create search index record for extract [{0}].";
            throw new IllegalStateException(MessageFormat.format(searchErrMsg, extract.getId()), ex);
         }
      }

      private void addProxy() throws SearchException
      {
         ExtractSearchProxy proxy = ExtractSearchProxy.create(extract, parser);
         document.set(ExtractSolrConfig.SEARCH_PROXY, proxy);
      }

      private void addManuscriptDetail(DramaticExtract extract) throws SearchException
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

      private void addTransformedContent(DramaticExtract extract) throws SearchException
      {
         Document content = extract.getTEIContent();
         try
         {
            String normalized = parser.toNormalized(content);
            String original = parser.toOriginal(content);

            document.set(ExtractSolrConfig.NORMALIZED, normalized);
            document.set(ExtractSolrConfig.ORIGINAL, original);
         }
         catch (ExtractManipulationException e)
         {
            problems.addParseError(content.toString());
         }
      }

      private void addPlaywrights(DramaticExtract extract) throws SearchException
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

      private void addPlayDetails(DramaticExtract extract) throws SearchException
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

      public void addSpeakerReferences(DramaticExtract extract) throws SearchException
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
}
