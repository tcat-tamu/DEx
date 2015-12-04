package edu.tamu.tcat.dex.importer.model;

import java.util.HashSet;
import java.util.Set;

import edu.tamu.tcat.dex.trc.extract.dto.ExtractDTO;

public class ExtractImportDTO extends ExtractDTO
{
   /**
    * Speaker IDs must be resolved prior to saving
    */
   public Set<String> speakerIds = new HashSet<>();

   /**
    * Source ID must be resolved prior to saving
    */
   public String sourceId;
}