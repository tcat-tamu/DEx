package edu.tamu.tcat.dex.importer;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
   private static Logger logger = Logger.getLogger(PeopleAndPlaysImporter.class.getName());

   private static SAXParserFactory parserFactory = SAXParserFactory.newInstance();
   private static PeopleAndPlaysHandler handler = new PeopleAndPlaysHandler();
   private static SAXParser parser;
   private static XMLReader reader;

   static {
      parserFactory.setNamespaceAware(true);

      try
      {
         parser = parserFactory.newSAXParser();
         reader = parser.getXMLReader();
         reader.setContentHandler(handler);
      }
      catch (Exception e)
      {
         logger.log(Level.SEVERE, "Could not create static instance of SAX XML reader", e);
      }
   }

   /**
    * Singleton class; static load() method is primary interface
    */
   private PeopleAndPlaysImporter()
   {
   }

   /**
    * Load People and Plays data from TEI XML source
    *
    * @param xmlSource
    * @throws XmlParseException
    * @throws IOException
    */
   public static void load(Reader xmlSource) throws XmlParseException, IOException
   {
      InputSource inputSource = new InputSource(xmlSource);

      try {
         reader.parse(inputSource);
      }
      catch (SAXException e)
      {
         throw new XmlParseException("malformed XML input", e);
      }

      Map<String, PlayDTO> plays = handler.getPlays();
      Map<String, PlaywrightDTO> playwrights = handler.getPlaywrights();
      Map<String, CharacterDTO> characters = handler.getCharacters();

      System.out.println(String.format("Parsed %d play(s), %d playwright(s), and %d character(s).", plays.size(), playwrights.size(), characters.size()));
   }

   public static void main(String[] args)
   {
      try (FileReader fileReader = new FileReader("/home/CITD/matt.barry/Documents/Projects/dex/WorkPlan/peopleandplays.xml"))
      {
         load(fileReader);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

   }
}
