package edu.tamu.tcat.dex.importer.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ExtractDTO
{
   private final String id;

   private String playId;
   private String xmlContent;

   private final Set<String> speakers = new HashSet<>();

   public ExtractDTO(String id)
   {
      this.id = id;
   }

   public String getId()
   {
      return id;
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

   public String getXMLContent()
   {
      return xmlContent;
   }

   public void setXMLContent(String xmlContent)
   {
      this.xmlContent = xmlContent;
   }
}