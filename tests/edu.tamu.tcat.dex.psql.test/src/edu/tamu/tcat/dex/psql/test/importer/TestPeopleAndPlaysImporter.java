package edu.tamu.tcat.dex.psql.test.importer;

import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.tamu.tcat.dex.importer.PeopleAndPlaysImporter;
import edu.tamu.tcat.dex.importer.PeopleAndPlaysImporter.ImportResult;
import edu.tamu.tcat.dex.importer.DexImportException;

public class TestPeopleAndPlaysImporter
{

   private static PeopleAndPlaysImporter importer = new PeopleAndPlaysImporter();

   @Test
   public void testLoad() throws DexImportException, IOException
   {
      String inputFilePath = "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/peopleandplays.xml";

      ImportResult result;
      try (FileReader fileReader = new FileReader(inputFilePath))
      {
         result = importer.load(fileReader);
      }

      System.out.println(String.format("Parsed %d play(s), %d playwright(s), and %d character(s).", result.getPlays().size(), result.getPlaywrights().size(), result.getCharacters().size()));

      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(System.out, result);
   }

}
