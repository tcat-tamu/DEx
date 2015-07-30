package edu.tamu.tcat.dex.psql.test;

import static org.junit.Assert.fail;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;

import edu.tamu.tcat.dex.importer.DexImportException;
import edu.tamu.tcat.dex.importer.DexImportService;

public class TestDexImportService
{
   private static final String TEI_BASE_PATH = "/home/CITD/matthew.barry/Dropbox/Shared/DEx MSS for Matthew";

   private static final String PEOPLE_PLAYS_TEI_PATH = TEI_BASE_PATH + "/peopleandplays_13April.xml";

   private static final String[] MANUSCRIPT_TEI_PATHS = {
      TEI_BASE_PATH + "/BLMSAdd10309.xml",
      TEI_BASE_PATH + "/BLMSAdd22608.xml",
      TEI_BASE_PATH + "/BLMSAdd64078.xml",
      TEI_BASE_PATH + "/BLMSLansdowne1185.xml",
      TEI_BASE_PATH + "/BodleianMSSancroft29.xml",
      TEI_BASE_PATH + "/FolgerMSVa87_22Apr.xml",
      TEI_BASE_PATH + "/Harvard MS Fr. 487.xml",
      TEI_BASE_PATH + "/UChicago_MS824.xml"
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

         Path p = Paths.get(filePath);

         try (InputStream tei = Files.newInputStream(p))
         {
            importService.importManuscriptTEI(manuscriptId, tei);

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
