package edu.tamu.tcat.dex.importer;

import java.io.IOException;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import edu.tamu.tcat.dex.importer.model.ManuscriptDTO;

public class ManuscriptImporter
{
   private final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
   private final SAXParser saxParser;
   private final XMLReader reader;
   private final ManuscriptHandler handler = new ManuscriptHandler();

   public ManuscriptImporter()
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
}