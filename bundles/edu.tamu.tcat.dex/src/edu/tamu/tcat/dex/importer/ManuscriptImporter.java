package edu.tamu.tcat.dex.importer;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import edu.tamu.tcat.dex.importer.model.ExtractDTO;
import edu.tamu.tcat.dex.importer.model.ManuscriptDTO;

public class ManuscriptImporter
{
   private static final Logger logger = Logger.getLogger(ManuscriptImporter.class.getName());

   private static SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
   private static SAXParser saxParser;
   private static XMLReader reader;
   private static ManuscriptHandler handler = new ManuscriptHandler();

   private static DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
   private static DocumentBuilder documentBuilder;

   static {
      saxParserFactory.setNamespaceAware(true);

      try
      {
         saxParser = saxParserFactory.newSAXParser();
         reader = saxParser.getXMLReader();
         reader.setContentHandler(handler);
      }
      catch (Exception e)
      {
         logger.log(Level.SEVERE, "Could not create static instance of SAX XML reader", e);
      }

      try
      {
         documentBuilder = documentBuilderFactory.newDocumentBuilder();
      }
      catch (Exception e)
      {
         logger.log(Level.SEVERE, "Could not create static instance of Document XML parser", e);
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
         String tei = extract.getTEIContent();
         System.out.println("   " + tei);

         StringReader sr = new StringReader(tei);
         InputSource is = new InputSource(sr);

         try {
            Document d = documentBuilder.parse(is);
         }
         catch (SAXException e)
         {
            throw new IllegalStateException("Encountered a problem parsing a TEI DOM", e);
         }
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