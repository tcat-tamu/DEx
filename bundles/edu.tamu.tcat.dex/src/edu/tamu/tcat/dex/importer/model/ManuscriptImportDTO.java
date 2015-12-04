package edu.tamu.tcat.dex.importer.model;

import java.util.ArrayList;
import java.util.List;

import edu.tamu.tcat.dex.trc.extract.dto.ManuscriptDTO;

public class ManuscriptImportDTO extends ManuscriptDTO
{
   public final List<ExtractImportDTO> extracts = new ArrayList<>();
}
