package edu.tamu.tcat.dex.importer;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import edu.tamu.tcat.dex.importer.model.ManuscriptImportDTO;

public class ManuscriptParser
{

   private static XMLReader getReader()
   {
      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      saxParserFactory.setNamespaceAware(true);

      try
      {
         SAXParser saxParser = saxParserFactory.newSAXParser();
         return saxParser.getXMLReader();
      }
      catch (ParserConfigurationException | SAXException e)
      {
         throw new IllegalStateException("Could not create instance of SAX XML reader", e);
      }

   }

   public static ManuscriptImportDTO load(InputStream xmlSource) throws DexImportException, IOException
   {
      InputSource inputSource = new InputSource(xmlSource);

      ManuscriptHandler handler = new ManuscriptHandler();
      XMLReader reader = getReader();
      reader.setContentHandler(handler);

      try
      {
         reader.parse(inputSource);
      }
      catch (SAXException e)
      {
         throw new DexImportException("malformed XML input", e);
      }

      return handler.getManuscript();
   }
}