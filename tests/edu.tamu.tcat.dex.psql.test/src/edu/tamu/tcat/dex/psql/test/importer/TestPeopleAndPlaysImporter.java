package edu.tamu.tcat.dex.psql.test.importer;

import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.tamu.tcat.dex.importer.DexImportException;
import edu.tamu.tcat.dex.importer.PeopleAndPlaysParser;
import edu.tamu.tcat.dex.importer.PeopleAndPlaysParser.ImportResult;

public class TestPeopleAndPlaysImporter
{
   private static final String TEI_BASE_PATH = "/home/CITD/matthew.barry/Dropbox/Shared/DEx MSS for Matthew";

   @Test
   public void testLoad() throws DexImportException, IOException
   {
      String inputFilePath = TEI_BASE_PATH + "/peopleandplays_13April.xml";

      ImportResult result;
      try (FileReader fileReader = new FileReader(inputFilePath))
      {
         result = PeopleAndPlaysParser.load(fileReader);
      }

      System.out.println(String.format("Parsed %d play(s), %d playwright(s), and %d character(s).", result.plays.size(), result.playwrights.size(), result.characters.size()));

      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(System.out, result);
   }

}
