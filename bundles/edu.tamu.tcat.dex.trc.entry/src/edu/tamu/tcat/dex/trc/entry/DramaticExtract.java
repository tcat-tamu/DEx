package edu.tamu.tcat.dex.trc.entry;

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
    *       On occasion, the author of the manuscript will collect extracts written by friends or others.
    *       See, for example, birthday books, autograph books.
    */
   String getAuthor();

   /**
    * @return The ID of the bibliographic entry for the manuscript this extract was
    *       compiled in.
    */
   String getManuscriptId();

   /**
    * @return The URI of the bibliographic entry for the play or other source this
    *       extract references.
    */
   SourceRef getSource();

   /**
    * @return A set of IDs for the biographical entries corresponding to the characters
    *       who spoke the lines recorded in this extract.
    */
   Set<String> getSpeakerIds();

   /**
    * @return The TEI encoded transcription of this extract as defined by the project
    *       guidelines.
    */
   Document getTEIContent();    // TODO add link to description of guidelines
}
