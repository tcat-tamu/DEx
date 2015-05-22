package edu.tamu.tcat.dex.importer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

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
   private final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
   private final SAXParser saxParser;
   private final XMLReader reader;
   private final ManuscriptHandler handler = new ManuscriptHandler();

   private ManuscriptImporter()
   {
      saxParserFactory.setNamespaceAware(true);

      try
      {
         saxParser = saxParserFactory.newSAXParser();
         reader = saxParser.getXMLReader();
      }
      catch (ParserConfigurationException | SAXException e)
      {
         throw new IllegalStateException("Could not create instance of SAX XML reader", e);
      }

      reader.setContentHandler(handler);
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
      ManuscriptImporter importer = new ManuscriptImporter();

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