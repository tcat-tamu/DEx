package edu.tamu.tcat.dex.importer.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class PlayDTO
{
   public static class PlaywrightReferenceDTO
   {
      public String playwrightId;
      public String displayName;
   }

   public static class EditionDTO
   {
      public String title;
      public final List<String> editors = new ArrayList<>();
      public String publisher;
      public String date;
      public URI link;
   }

   public String id;

   public final List<String> titles = new ArrayList<>();
   public final List<EditionDTO> editions = new ArrayList<>();

   public final Set<PlaywrightReferenceDTO> playwrightRefs = new HashSet<>();
}
