package edu.tamu.tcat.dex.importer;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.tamu.tcat.dex.importer.model.CharacterDTO;
import edu.tamu.tcat.dex.importer.model.PlayDTO;
import edu.tamu.tcat.dex.importer.model.PlaywrightDTO;


public class PeopleAndPlaysImporter
{
   public static class ImportResult
   {
      private final Map<String, PlayDTO> plays;
      private final Map<String, PlaywrightDTO> playwrights;
      private final Map<String, CharacterDTO> characters;


      private ImportResult(Map<String, PlayDTO> plays, Map<String, PlaywrightDTO> playwrights, Map<String, CharacterDTO> characters)
      {
         this.plays = plays;
         this.playwrights = playwrights;
         this.characters = characters;
      }

      public Map<String, PlayDTO> getPlays()
      {
         return plays;
      }

      public Map<String, PlaywrightDTO> getPlaywrights()
      {
         return playwrights;
      }
      public Map<String, CharacterDTO> getCharacters()
      {
         return characters;
      }
   }


   private SAXParserFactory parserFactory = SAXParserFactory.newInstance();
   private final SAXParser parser;
   private final XMLReader reader;
   private final PeopleAndPlaysHandler handler = new PeopleAndPlaysHandler();


   /**
    * Singleton class; static load() method is primary interface
    */
   private PeopleAndPlaysImporter()
   {
      parserFactory.setNamespaceAware(true);

      try
      {
         parser = parserFactory.newSAXParser();
         reader = parser.getXMLReader();
         reader.setContentHandler(handler);
      }
      catch (Exception e)
      {
         throw new IllegalStateException("Could not create static instance of SAX XML reader", e);
      }
   }

   /**
    * Load People and Plays data from TEI XML source
    *
    * @param xmlSource
    * @throws XmlParseException
    * @throws IOException
    */
   public ImportResult load(Reader xmlSource) throws XmlParseException, IOException
   {
      InputSource inputSource = new InputSource(xmlSource);

      try
      {
         reader.parse(inputSource);
      }
      catch (SAXException e)
      {
         throw new XmlParseException("malformed XML input", e);
      }

      Map<String, PlayDTO> plays = handler.getPlays();
      Map<String, PlaywrightDTO> playwrights = handler.getPlaywrights();
      Map<String, CharacterDTO> characters = handler.getCharacters();

      return new ImportResult(plays, playwrights, characters);
   }

   public static void main(String[] args)
   {
      PeopleAndPlaysImporter importer = new PeopleAndPlaysImporter();

      String inputFilePath = "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/peopleandplays.xml";

      ImportResult result;
      try (FileReader fileReader = new FileReader(inputFilePath))
      {
         result = importer.load(fileReader);
      }
      catch (FileNotFoundException e)
      {
         System.err.println("Unable to find input file: " + inputFilePath);
         return;
      }
      catch (IOException e)
      {
         System.err.println("Could not read input file");
         e.printStackTrace();
         return;
      }
      catch (XmlParseException e)
      {
         System.err.println("Malformed input file");
         e.printStackTrace();
         return;
      }

      System.out.println(String.format("Parsed %d play(s), %d playwright(s), and %d character(s).", result.getPlays().size(), result.getPlaywrights().size(), result.getCharacters().size()));

      ObjectMapper mapper = new ObjectMapper();
      try
      {
         mapper.writeValue(System.out, result);
      }
      catch (JsonProcessingException e)
      {
         System.err.println("Problem processing JSON output");
         e.printStackTrace();
      }
      catch (IOException e) {
         System.err.println("Unable to output result");
         e.printStackTrace();
      }


   }
}
