package edu.tamu.tcat.dex.importer.model;

import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Document;

public class ExtractImportDTO
{
   // TODO: member variables should be public


   public String id;

   /**
    * Author typically inherited from manuscript, but cases exist where extract author
    * could differ from manuscript author
    */
   public String author;

   public String sourceRef;
   public String source;
   public String teiContent;

   public final Set<String> speakerIds = new HashSet<>();
}