package edu.tamu.tcat.dex.trc.extract.search.solr;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class IndexCreationProblems
{
   private final String extractId;

   Map<String, String> warnings = new HashMap<>();

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