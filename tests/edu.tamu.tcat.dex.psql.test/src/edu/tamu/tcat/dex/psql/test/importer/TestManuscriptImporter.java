package edu.tamu.tcat.dex.psql.test.importer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.tamu.tcat.dex.importer.DexImportException;
import edu.tamu.tcat.dex.importer.ManuscriptParser;
import edu.tamu.tcat.dex.importer.model.ManuscriptImportDTO;

public class TestManuscriptImporter
{
   private static final String TEI_BASE_PATH = "/home/CITD/matt.barry/Documents/Projects/dex";

   private static final String[] files = {
      TEI_BASE_PATH + "/Sample Files/BLMSAdd10309.xml",
      TEI_BASE_PATH + "/Sample Files/BLMSAdd64078.xml",
      TEI_BASE_PATH + "/Sample Files/BLMSLansdowne1185.new.xml",
      TEI_BASE_PATH + "/Sample Files/BodleianMSSancroft29.xml",
      TEI_BASE_PATH + "/Sample Files/DEx_Sample_BLAddMS22608.xml",
      TEI_BASE_PATH + "/Sample Files/FolgerMSVa87_22Apr.xml",
      TEI_BASE_PATH + "/Sample Files/Harvard MS Fr. 487.xml",
      TEI_BASE_PATH + "/Sample Files/UChicago_MS824.xml"
   };

   private static final ManuscriptParser importer = new ManuscriptParser();


   @Test
   public void testLoad() throws DexImportException, IOException
   {
      Map<String, ManuscriptImportDTO> manuscripts = new HashMap<>();

      for (String filePath : files)
      {
         System.out.println("Processing File: " + filePath);

         Path p = Paths.get(filePath);

         try (InputStream tei = Files.newInputStream(p))
         {
            ManuscriptImportDTO manuscript = ManuscriptParser.load(tei);
            manuscripts.put(p.getFileName().toString(), manuscript);

         }
      }

      ObjectMapper mapper = new ObjectMapper();
      File outputFile = new File(TEI_BASE_PATH + "/manuscripts.json");
      mapper.writeValue(outputFile, manuscripts);
   }
}
