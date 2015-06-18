package edu.tamu.tcat.dex.psql.test;

import static org.junit.Assert.fail;

import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;

import edu.tamu.tcat.dex.importer.DexImportException;
import edu.tamu.tcat.dex.importer.DexImportService;

public class TestDexImportService
{
   private static final String PEOPLE_PLAYS_TEI_PATH = "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/peopleandplays.xml";
   private static final String[] MANUSCRIPT_TEI_PATHS = {
      "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/BLMSAdd10309.xml",
      "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/DEx_Sample_BLAddMS22608.xml",
      "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/BLMSAdd64078.xml",
      "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/BLMSLansdowne1185.new.xml",
      "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/BodleianMSSancroft29.xml",
      "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/FolgerMSVa87_22Apr.xml",
      "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/Harvard MS Fr. 487.xml",
      "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/UChicago_MS824.xml"
   };

   @Test
   public void testImport() throws DexImportException
   {
      // attaches listener to repository
      ServiceHelper.getSearchService();

      boolean resultPeopleAndPlays = testImportPeopleAndPlays();
      if (!resultPeopleAndPlays)
      {
         System.out.println("Skipping manuscript import test.");
      }
      testImportManuscript();
   }

   public boolean testImportPeopleAndPlays() throws DexImportException
   {
      DexImportService importService = ServiceHelper.getImportService();

      try (FileReader teiReader = new FileReader(PEOPLE_PLAYS_TEI_PATH))
      {
         importService.importPeopleAndPlaysTEI(teiReader);
      }
      catch (IOException e)
      {
         fail("Unable to locate people and plays TEI XML file [" + PEOPLE_PLAYS_TEI_PATH + "].");
         return false;
      }


      return true;
   }

   public boolean testImportManuscript() throws DexImportException
   {
      for (String filePath : MANUSCRIPT_TEI_PATHS)
      {
         String manuscriptId = FilenameUtils.getBaseName(filePath);
         DexImportService importService = ServiceHelper.getImportService();

         try (FileReader teiReader = new FileReader(filePath))
         {
            importService.importManuscriptTEI(manuscriptId, teiReader);
         }
         catch (IOException e)
         {
            fail("Unable to locate manuscript TEI XML file [" + filePath + "].");
            return false;
         }
      }

      return true;
   }
}
