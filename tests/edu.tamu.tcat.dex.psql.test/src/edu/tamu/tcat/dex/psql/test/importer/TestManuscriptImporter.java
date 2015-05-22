package edu.tamu.tcat.dex.psql.test.importer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.tamu.tcat.dex.importer.ManuscriptImporter;
import edu.tamu.tcat.dex.importer.XmlParseException;
import edu.tamu.tcat.dex.importer.model.ManuscriptDTO;

public class TestManuscriptImporter
{
   private static final String[] files = {
      "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/BLMSAdd10309.xml",
      "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/BLMSAdd64078.xml",
      "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/BLMSLansdowne1185.new.xml",
      "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/BodleianMSSancroft29.xml",
      "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/DEx_Sample_BLAddMS22608.xml",
      "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/FolgerMSVa87_22Apr.xml",
      "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/Harvard MS Fr. 487.xml",
      "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/UChicago_MS824.xml"
   };

   private static final ManuscriptImporter importer = new ManuscriptImporter();


   @Test
   public void testLoad() throws XmlParseException, IOException
   {
      Map<String, ManuscriptDTO> manuscripts = new HashMap<>();

      for (String filePath : files)
      {
         System.out.println("Processing File: " + filePath);

         File file = new File(filePath);
         String basename = file.getName();

         try (Reader fileReader = new FileReader(file))
         {
            ManuscriptDTO manuscript = importer.load(fileReader);
            manuscripts.put(basename, manuscript);
         }
      }

      ObjectMapper mapper = new ObjectMapper();
      File outputFile = new File("/home/CITD/matt.barry/Documents/Projects/dex/manuscripts.json");
      mapper.writeValue(outputFile, manuscripts);
   }
}
