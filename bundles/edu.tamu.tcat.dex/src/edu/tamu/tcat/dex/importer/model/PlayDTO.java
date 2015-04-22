package edu.tamu.tcat.dex.importer.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class PlayDTO
{
   public static class PlaywrightReferenceDTO
   {
      private String displayName;
      private final String playwrightId;

      public PlaywrightReferenceDTO(String playwrightId)
      {
         this.playwrightId = playwrightId;
      }

      public String getDisplayName()
      {
         return displayName;
      }

      public void setDisplayName(String displayName)
      {
         this.displayName = displayName;
      }

      public String getPlaywrightId()
      {
         return playwrightId;
      }
   }

   private final String id;

   private final Set<String> titles = new HashSet<>();
   private final Set<String> editions = new HashSet<>();
   private final Set<String> editors = new HashSet<>();
   private final Set<String> publishers = new HashSet<>();
   private String date;

   private final Set<PlaywrightReferenceDTO> playwrights = new HashSet<>();

   public PlayDTO(String id)
   {
      this.id = id;
   }

   public String getId()
   {
      return id;
   }

   public String getDate()
   {
      return date;
   }

   public void setDate(String date)
   {
      this.date = date;
   }

   public Set<String> getTitles()
   {
      return Collections.unmodifiableSet(titles);
   }

   public void addTitle(String title)
   {
      titles.add(title);
   }

   public Set<String> getEditions()
   {
      return Collections.unmodifiableSet(editions);
   }

   public void addEdition(String edition)
   {
      editions.add(edition);
   }

   public Set<String> getEditors()
   {
      return Collections.unmodifiableSet(editors);
   }

   public void addEditor(String editor)
   {
      editors.add(editor);
   }

   public Set<PlaywrightReferenceDTO> getPlaywrights()
   {
      return Collections.unmodifiableSet(playwrights);
   }

   public void addPlaywright(PlaywrightReferenceDTO playwright)
   {
      playwrights.add(playwright);
   }

   public void addPublisher(String publisher)
   {
      publishers.add(publisher);
   }
}
