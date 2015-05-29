package edu.tamu.tcat.dex.importer.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CharacterDTO
{
   public String id;

   public final List<String> names = new ArrayList<>();
   public final Set<String> playIds = new HashSet<>();
}
