package edu.tamu.tcat.dex.importer;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import edu.tamu.tcat.dex.importer.model.ExtractDTO;
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

   public void load(Reader xmlSource) throws XmlParseException, IOException
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

         Document document = extract.getTEIContent();
         DOMSource domSource = new DOMSource(document);

         StringWriter writer = new StringWriter();
         StreamResult result = new StreamResult(writer);

         TransformerFactory tf = TransformerFactory.newInstance();
         try {
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
         }
         catch (TransformerException e) {
            throw new IllegalStateException(e);
         }

         String tei = writer.toString();

         System.out.println("   " + tei);
      }
   }

   public static void main(String[] args)
   {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder;

      try {
         documentBuilder = documentBuilderFactory.newDocumentBuilder();
      }
      catch (ParserConfigurationException e) {
         logger.log(Level.SEVERE, "Could not create instance of document builder", e);
         return;
      }

      ManuscriptHandler handler = new ManuscriptHandler();
      handler.setDocumentBuilder(documentBuilder);
      handler.activate();

      ManuscriptImporter importer = new ManuscriptImporter();
      importer.setManuscriptHandler(handler);
      importer.activate();

      try (Reader fileReader = new FileReader("/home/CITD/matt.barry/Documents/Projects/dex/Sample Files/DEx_Sample_BLAddMS22608.xml"))
      {
         importer.load(fileReader);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
}