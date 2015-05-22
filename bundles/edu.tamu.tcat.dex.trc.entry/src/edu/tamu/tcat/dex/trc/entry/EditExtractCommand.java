package edu.tamu.tcat.dex.trc.entry;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.Future;

import org.w3c.dom.Document;

public interface EditExtractCommand
{
   /**
    * @param author Name of this extract's author (could differ from manuscript author)
    */
   void setAuthor(String author);

   /**
    * @param manuscript URI of the manuscript to which this extract belongs
    */
   void setManuscript(URI manuscript);

   /**
    * @param source URI of the bibliographic entry (e.g. play) from which this extract was taken
    */
   void setSource(URI source);

   /**
    * @param sourceRef String representing the exact location within the bibliographic
    *       source from which this extract was taken
    */
   void setSourceRef(String sourceRef);

   /**
    * @param speakers A set of URIs corresponding to the characters who spoke the lines
    *       recorded in this extract
    */
   void setSpeakers(Set<URI> speakers);

   /**
    * @param teiContent An XML Document representing the original TEI source of this extract
    */
   void setTEIContent(Document teiContent);

   /**
    * Persists the modifications made to the underlying DramaticExtract object.
    *
    * @return Future ID of the persisted object once persist actions have finished.
    * @throws DramaticExtractException
    */
   Future<URI> execute() throws DramaticExtractException;
}
