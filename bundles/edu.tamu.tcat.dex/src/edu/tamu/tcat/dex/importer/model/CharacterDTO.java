package edu.tamu.tcat.dex.importer.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CharacterDTO
{
   private final String id;

   private final Set<String> names = new HashSet<>();
   private final Set<String> playIds = new HashSet<>();

   public CharacterDTO(String id)
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

   public Set<String> getPlayIds()
   {
      return Collections.unmodifiableSet(playIds);
   }

   public void addPlayId(String playId)
   {
      playIds.add(playId);
   }
}
