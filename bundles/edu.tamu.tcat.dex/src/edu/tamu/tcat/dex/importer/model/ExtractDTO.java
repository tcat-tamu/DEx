package edu.tamu.tcat.dex.importer.model;

import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Document;

public class ExtractDTO
{
   // TODO: member variables should be public


   public String id;

   /**
    * Author typically inherited from manuscript, but cases exist where extract author
    * could differ from manuscript author
    */
   public String author;

   public String lineRef;
   public String playId;
   public Document teiContent;

   public final Set<String> speakers = new HashSet<>();
}