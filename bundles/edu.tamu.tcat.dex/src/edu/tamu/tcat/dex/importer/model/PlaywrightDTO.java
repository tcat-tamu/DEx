package edu.tamu.tcat.dex.importer.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PlaywrightDTO
{
   private final String id;

   private final Set<String> names = new HashSet<>();

   public PlaywrightDTO(String id)
   {
      this.id = id;
   }

   public String getId()
   {
      return id;
   }

   public Set<String> getNames()
   {
      return Collections.unmodifiableSet(names);
   }

   public void addName(String name)
   {
      names.add(name);
   }
}
