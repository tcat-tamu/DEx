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
   private static final String TEI_BASE_PATH = "/home/CITD/matthew.barry/Dropbox/Shared/DEx MSS for Matthew";

   private static final String[] files = {
      TEI_BASE_PATH + "/BLMSAdd10309.xml",
      TEI_BASE_PATH + "/BLMSAdd22608.xml",
      TEI_BASE_PATH + "/BLMSAdd64078.xml",
      TEI_BASE_PATH + "/BLMSLansdowne1185.xml",
      TEI_BASE_PATH + "/BodleianMSSancroft29.xml",
      TEI_BASE_PATH + "/FolgerMSVa87_22Apr.xml",
      TEI_BASE_PATH + "/Harvard MS Fr. 487.xml",
      TEI_BASE_PATH + "/UChicago_MS824.xml"
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
         catch (DexImportException e) {
            System.err.println("Unable to import [" + filePath + "].");
            throw e;
         }
      }

      ObjectMapper mapper = new ObjectMapper();
      File outputFile = new File(TEI_BASE_PATH + "/manuscripts.json");
      mapper.writeValue(outputFile, manuscripts);
   }
}
