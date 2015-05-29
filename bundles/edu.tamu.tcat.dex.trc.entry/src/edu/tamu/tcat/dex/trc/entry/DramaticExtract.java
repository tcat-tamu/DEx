package edu.tamu.tcat.dex.trc.entry;

import java.net.URI;
import java.util.Set;

import org.w3c.dom.Document;

/**
 * TODO add description of DEx from project startup documents
 */
public interface DramaticExtract
{
   /**
    * @return A unique, persistent identifier for this extract
    */
   String getId();


   /**
    * @return The name of the extract's author. Usually the author of the manuscript.
    */
   String getAuthor();

   /**
    * @return The URI of the bibliographic entry for the manuscript this extract was
    * 		compiled in.
    */
   URI getManuscript();

   /**
    * @return The URI of the bibliographic entry for the play or other source this
    * 		extract references.
    */
   URI getSource();

   /**
    * @return A string representing the exact location within the source bibliographic entry where
    *       this excerpt may be found.
    */
   String getSourceRef();

   /**
    * @return A set of URIs for the biographical entries corresponding to the characters
    * 		who spoke the lines recorded in this extract.
    */
   Set<URI> getSpeakers();

   /**
    * @return The TEI encoded transcription of this extract as defined by the project
    *       guidelines.
    */
   Document getTEIContent();    // TODO add link to description of guidelines

   /**
    * @return A plain-text representation of the original content. This is extracted
    *       from the structured TEI content to remove spelling normalizations and
    *       expansion of abbreviations. The content will represent the extract as it
    *       originally appeared in the manuscript rather than in the play or other source
    *       material it was taken from.
    */
   String getOriginalContent();     // TODO this seems like it should be used internally, rather than part of the API.

   /**
    * @return A normalized representation of the extract as plain-text. This representation
    *       includes spelling normalizations and expansions of abbreviations to produce a
    *       version of the extract that is easier for modern audiences to read and understand.
    */
   String getNormalizedContent();  // TODO this seems like it should be used internally, rather than part of the API.
}
