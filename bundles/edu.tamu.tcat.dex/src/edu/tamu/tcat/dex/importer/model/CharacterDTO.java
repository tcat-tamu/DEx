package edu.tamu.tcat.dex.importer.model;

import java.util.HashSet;
import java.util.Set;

public class CharacterDTO
{
   public String id;

   public final Set<String> names = new HashSet<>();
   public final Set<String> playIds = new HashSet<>();
}
