package edu.tamu.tcat.dex.importer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ManuscriptDTO
{
   private String title;
   private String author;

   private final List<ExtractDTO> extracts = new ArrayList<>();

   public ManuscriptDTO()
   {
   }

   public String getTitle()
   {
      return title;
   }

   public void setTitle(String title)
   {
      this.title = title;
   }

   public String getAuthor()
   {
      return author;
   }

   public void setAuthor(String author)
   {
      this.author = author;
   }

   public void addExtract(ExtractDTO extract)
   {
      extracts.add(extract);
   }

   public List<ExtractDTO> getExtracts()
   {
      return Collections.unmodifiableList(extracts);
   }
}
