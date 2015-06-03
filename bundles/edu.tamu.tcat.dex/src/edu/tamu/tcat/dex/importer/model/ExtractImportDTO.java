package edu.tamu.tcat.dex.importer.model;

import java.util.Set;

import edu.tamu.tcat.trc.extract.dto.ExtractDTO;

public class ExtractImportDTO extends ExtractDTO
{
   /**
    * Speaker IDs must be resolved prior to saving
    */
   public Set<String> speakerIds;
}