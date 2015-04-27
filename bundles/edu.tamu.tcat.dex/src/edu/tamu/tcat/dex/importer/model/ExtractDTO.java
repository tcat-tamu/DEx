package edu.tamu.tcat.dex.importer.model;

public class ExtractDTO
{
   private final String id;

   private String playId;
   private String xmlContent;

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

   public String getXMLContent()
   {
      return xmlContent;
   }

   public void setXMLContent(String xmlContent)
   {
      this.xmlContent = xmlContent;
   }
}