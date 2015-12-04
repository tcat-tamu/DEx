package edu.tamu.tcat.dex.trc.extract;

import java.util.Set;
import java.util.concurrent.Future;

import org.w3c.dom.Document;

public interface EditExtractCommand
{
   /**
    * Set all properties to match those from the provided extract
    *
    * @param extract
    */
   void setAll(DramaticExtract extract);

   /**
    * @param author Name of this extract's author (could differ from manuscript author)
    */
   void setAuthor(String author);

   /**
    * @param manuscriptId ID of the manuscript to which this extract belongs
    */
   void setManuscriptId(String manuscriptId);

   /**
    * @param manuscriptTitle Title of the manuscript to which this extract belongs
    */
   void setManuscriptTitle(String manuscriptTitle);

   /**
    * @param sourceId ID of the bibliographic entry (e.g. play) from which this extract was taken
    */
   void setSourceId(String sourceId);

   /**
    * @param sourceTitle Title of the bibliographic entry (e.g. play) from which this extract was taken
    */
   void setSourceTitle(String sourceTitle);

   /**
    * @param sourceRef String representing the exact location within the bibliographic
    *       source from which this extract was taken
    */
   void setSourceRef(String sourceRef);

   /**
    * @param speakers A set of URIs corresponding to the characters who spoke the lines
    *       recorded in this extract
    */
   void setSpeakers(Set<Pair<String, String>> speakers);

   /**
    * @param playwrights A set of ID/name pairs corresponding to the authors of the source bibliographic
    *       entry referenced by this extract
    */
   void setPlaywrights(Set<Pair<String, String>> playwrights);

   /**
    * @param teiContent An XML Document representing the original TEI source of this extract
    */
   void setTEIContent(Document teiContent);
   void setTEIContent(String teiContent);

   /**
    * Persists the modifications made to the underlying DramaticExtract object.
    *
    * @return Future ID of the persisted object once persist actions have finished.
    * @throws DramaticExtractException
    */
   Future<String> execute() throws DramaticExtractException;

   void setFolioIdentifier(String folio);

   void setMsIndex(int order);
}
