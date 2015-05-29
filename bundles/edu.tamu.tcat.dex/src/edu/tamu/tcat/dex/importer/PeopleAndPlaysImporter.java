package edu.tamu.tcat.dex.importer;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

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
   public PeopleAndPlaysImporter()
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
    * @throws DexImportException
    * @throws IOException
    */
   public ImportResult load(Reader xmlSource) throws DexImportException, IOException
   {
      InputSource inputSource = new InputSource(xmlSource);

      try
      {
         reader.parse(inputSource);
      }
      catch (SAXException e)
      {
         throw new DexImportException("malformed XML input", e);
      }

      Map<String, PlayDTO> plays = handler.getPlays();
      Map<String, PlaywrightDTO> playwrights = handler.getPlaywrights();
      Map<String, CharacterDTO> characters = handler.getCharacters();

      return new ImportResult(plays, playwrights, characters);
   }
}
