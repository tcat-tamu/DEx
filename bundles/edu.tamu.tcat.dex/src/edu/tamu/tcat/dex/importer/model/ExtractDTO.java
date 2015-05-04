package edu.tamu.tcat.dex.importer.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Document;

public class ExtractDTO
{
   private final String id;

   /**
    * Author typically inherited from manuscript, but cases exist where extract author
    * could differ from manuscript author
    */
   private String author;

   private String lineRef;
   private String playId;
   private Document teiContent;

   private final Set<String> speakers = new HashSet<>();

   public ExtractDTO(String id)
   {
      this.id = id;
   }

   public String getId()
   {
      return id;
   }

   public String getAuthor()
   {
      return author;
   }

   public void setAuthor(String author)
   {
      this.author = author;
   }

   public String getPlayId()
   {
      return playId;
   }

   public void setPlayId(String playId)
   {
      this.playId = playId;
   }

   public void addSpeaker(String speakerId)
   {
      speakers.add(speakerId);
   }

   public Set<String> getSpeakers()
   {
      return Collections.unmodifiableSet(speakers);
   }

   public Document getTEIContent()
   {
      return teiContent;
   }

   public void setTEIContent(Document teiContent)
   {
      this.teiContent = teiContent;
   }

   public String getLineRef()
   {
      return lineRef;
   }

   public void setLineRef(String lineRef)
   {
      this.lineRef = lineRef;
   }
}