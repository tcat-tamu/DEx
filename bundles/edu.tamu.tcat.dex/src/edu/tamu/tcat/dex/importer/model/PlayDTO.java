package edu.tamu.tcat.dex.importer.model;

import java.util.HashSet;
import java.util.Set;


public class PlayDTO
{
   public static class PlaywrightReferenceDTO
   {
      public String playwrightId;
      public String displayName;
   }

   public String id;

   public final Set<String> titles = new HashSet<>();
   public final Set<String> editions = new HashSet<>();
   public final Set<String> editors = new HashSet<>();
   public final Set<String> publishers = new HashSet<>();
   public String date;

   public final Set<PlaywrightReferenceDTO> playwrightRefs = new HashSet<>();
}
