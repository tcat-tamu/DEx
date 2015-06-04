package edu.tamu.tcat.dex.psql.test;

import static org.junit.Assert.fail;

import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import edu.tamu.tcat.dex.importer.DexImportException;
import edu.tamu.tcat.dex.importer.DexImportService;

public class TestDexImportService
{
   private static final String PEOPLE_PLAYS_TEI_PATH = "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/peopleandplays.xml";
   private static final String MANUSCRIPT_TEI_PATH = "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/BodleianMSSancroft29.xml";

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
      FileReader teiReader;
      try
      {
         teiReader = new FileReader(PEOPLE_PLAYS_TEI_PATH);
      }
      catch (IOException e)
      {
         fail("Unable to locate people and plays TEI XML file.");
         return false;
      }

      DexImportService importService = ServiceHelper.getImportService();
      importService.importPeopleAndPlaysTEI(teiReader);

      return true;
   }

   public boolean testImportManuscript() throws DexImportException
   {
      FileReader teiReader;
      try
      {
         teiReader = new FileReader(MANUSCRIPT_TEI_PATH);
      }
      catch (IOException e)
      {
         fail("Unable to locate people and plays TEI XML file.");
         return false;
      }

      DexImportService importService = ServiceHelper.getImportService();
      importService.importManuscriptTEI("BodleianMSSancroft29", teiReader);

      return true;
   }
}
