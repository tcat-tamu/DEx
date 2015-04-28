package edu.tamu.tcat.dex.importer;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import edu.tamu.tcat.dex.importer.model.ExtractDTO;
import edu.tamu.tcat.dex.importer.model.ManuscriptDTO;

public class ManuscriptImporter
{
   private static final Logger logger = Logger.getLogger(ManuscriptImporter.class.getName());

   private static SAXParserFactory parserFactory = SAXParserFactory.newInstance();
   private static ManuscriptHandler handler = new ManuscriptHandler();
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

   private ManuscriptImporter()
   {
   }

   public static void load(Reader xmlSource) throws XmlParseException, IOException
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

      ManuscriptDTO manuscript = handler.getManuscript();

      System.out.println(manuscript.getTitle());
      System.out.println("   by: " + manuscript.getAuthor());

      for (ExtractDTO extract : manuscript.getExtracts())
      {
         System.out.println("extract #" + extract.getId() + " from " + extract.getPlayId() + ":");
         System.out.println("   speakers:");
         for (String speakerId : extract.getSpeakers())
         {
            System.out.println("      " + speakerId);
         }
         System.out.println("   " + extract.getXMLContent());
      }
   }

   public static void main(String[] args)
   {
      try (Reader fileReader = new FileReader("/home/CITD/matt.barry/Documents/Projects/dex/WorkPlan/DEx_Sample_BLAddMS22608.xml"))
      {
         load(fileReader);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
}