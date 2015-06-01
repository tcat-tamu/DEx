package edu.tamu.tcat.dex.importer;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import edu.tamu.tcat.dex.importer.model.CharacterImportDTO;
import edu.tamu.tcat.dex.importer.model.PlayImportDTO;
import edu.tamu.tcat.dex.importer.model.PlaywrightImportDTO;


public class PeopleAndPlaysParser
{
   public static class ImportResult
   {
      public Map<String, PlayImportDTO> plays;
      public Map<String, PlaywrightImportDTO> playwrights;
      public Map<String, CharacterImportDTO> characters;
   }


   /**
    * Singleton class; static load() method is primary interface
    */
   public static XMLReader getReader()
   {
      SAXParserFactory parserFactory = SAXParserFactory.newInstance();
      parserFactory.setNamespaceAware(true);

      try
      {
         SAXParser parser = parserFactory.newSAXParser();
         return parser.getXMLReader();
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
   public static ImportResult load(Reader xmlSource) throws DexImportException, IOException
   {
      InputSource inputSource = new InputSource(xmlSource);

      XMLReader reader = getReader();
      PeopleAndPlaysHandler handler = new PeopleAndPlaysHandler();
      reader.setContentHandler(handler);

      try
      {
         reader.parse(inputSource);
      }
      catch (SAXException e)
      {
         throw new DexImportException("malformed XML input", e);
      }

      ImportResult result = new ImportResult();
      result.plays = handler.getPlays();
      result.playwrights = handler.getPlaywrights();
      result.characters = handler.getCharacters();

      return result;
   }
}
