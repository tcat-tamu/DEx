package edu.tamu.tcat.dex.importer.model;

import java.util.ArrayList;
import java.util.List;

public class ManuscriptDTO
{
   public String id;
   public String title;
   public String author;

   public final List<ExtractDTO> extracts = new ArrayList<>();
}
