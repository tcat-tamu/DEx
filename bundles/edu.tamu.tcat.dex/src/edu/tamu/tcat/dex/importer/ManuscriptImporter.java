package edu.tamu.tcat.dex.importer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.tamu.tcat.dex.importer.model.ManuscriptDTO;

public class ManuscriptImporter
{
   private static final Logger logger = Logger.getLogger(ManuscriptImporter.class.getName());

   private SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
   private SAXParser saxParser;
   private XMLReader reader;
   private ManuscriptHandler handler;

   private ManuscriptImporter()
   {
   }

   public void activate()
   {
      Objects.requireNonNull(handler);

      saxParserFactory.setNamespaceAware(true);

      try
      {
         saxParser = saxParserFactory.newSAXParser();
         reader = saxParser.getXMLReader();
      }
      catch (Exception e)
      {
         logger.log(Level.SEVERE, "Could not create instance of SAX XML reader", e);
      }

      reader.setContentHandler(this.handler);
   }

   public void setManuscriptHandler(ManuscriptHandler handler)
   {
      this.handler = handler;
   }

   public ManuscriptDTO load(Reader xmlSource) throws XmlParseException, IOException
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

      return handler.getManuscript();
   }

   public static void main(String[] args)
   {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder;

      try
      {
         documentBuilder = documentBuilderFactory.newDocumentBuilder();
      }
      catch (ParserConfigurationException e)
      {
         logger.log(Level.SEVERE, "Could not create instance of document builder", e);
         return;
      }

      ManuscriptHandler handler = new ManuscriptHandler();
      handler.setDocumentBuilder(documentBuilder);
      handler.activate();

      ManuscriptImporter importer = new ManuscriptImporter();
      importer.setManuscriptHandler(handler);
      importer.activate();

      String[] files = {
         "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/BLMSAdd10309.xml",
         "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/BLMSAdd64078.xml",
         "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/BLMSLansdowne1185.new.xml",
         "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/BodleianMSSancroft29.xml",
         "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/DEx_Sample_BLAddMS22608.xml",
         "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/FolgerMSVa87_22Apr.xml",
         "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/Harvard MS Fr. 487.xml",
         "/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/UChicago_MS824.xml"
      };

      Map<String, ManuscriptDTO> manuscripts = new HashMap<>();

      for (String filePath : files)
      {
         System.out.println("Processing File: " + filePath);

         File file = new File(filePath);
         String basename = file.getName();

         try (Reader fileReader = new FileReader(file))
         {
            ManuscriptDTO manuscript = importer.load(fileReader);
            manuscripts.put(basename, manuscript);
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }

      ObjectMapper mapper = new ObjectMapper();

      File outputFile = new File("/home/CITD/matt.barry/Documents/Projects/dex/manuscripts.json");
      try
      {
         mapper.writeValue(outputFile, manuscripts);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
}