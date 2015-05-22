package edu.tamu.tcat.dex.importer;

import java.io.IOException;
import java.io.StringReader;
import java.util.Stack;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.tamu.tcat.dex.importer.model.ExtractDTO;
import edu.tamu.tcat.dex.importer.model.ManuscriptDTO;

class ManuscriptHandler extends DefaultHandler
{
   private static class XmlStringBuilder
   {
      private StringBuilder sb;

      public XmlStringBuilder()
      {
         sb = new StringBuilder();
         sb.append("<?xml version=\"1.0\"?>");
      }

      public void startTag(String qName, Attributes attributes)
      {
         // write tag as string
         sb.append('<')
            .append(qName);

         int numAttributes = attributes.getLength();
         for (int i = 0; i < numAttributes; i++)
         {
            String attrName = attributes.getQName(i);
            String attrValue = attributes.getValue(i);

            sb.append(' ').append(attrName).append("=\"").append(attrValue).append('"');
         }

         sb.append('>');
      }

      public void endTag(String qName)
      {
         sb.append("</").append(qName).append('>');
      }

      public void text(String value)
      {
         String escapedValue = StringEscapeUtils.escapeXml(value);
         sb.append(escapedValue);
      }

      @Override
      public String toString()
      {
         return sb.toString();
      }
   }

   private static DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
   private DocumentBuilder documentBuilder;

   private Stack<String> elementStack;
   private Stack<Object> objectStack;

   private boolean rawMode;

   private ManuscriptDTO manuscript;

   /**
    * Set to {@code false} when the current {@code <div>} element does not represent a dramatic
    * extract, but, for example, a heading.
    */
   private boolean divIsExtract;


   public ManuscriptHandler()
   {
      try
      {
         documentBuilder = documentBuilderFactory.newDocumentBuilder();
      }
      catch (ParserConfigurationException e)
      {
         throw new IllegalStateException("Could not create instance of document builder", e);
      }
   }


   public ManuscriptDTO getManuscript()
   {
      return manuscript;
   }

   @Override
   public void startDocument() throws SAXException
   {
      elementStack = new Stack<>();
      objectStack = new Stack<>();

      String id = UUID.randomUUID().toString();
      manuscript = new ManuscriptDTO(id);

      rawMode = false;
   }

   @Override
   public void endDocument() throws SAXException
   {
      assert elementStack.empty() : "Leftover elements in element stack";
      assert objectStack.empty() : "Leftover objects in object stack";
   }

   @Override
   public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
   {
      if (rawMode)
      {
         XmlStringBuilder xsb = (XmlStringBuilder)objectStack.peek();
         xsb.startTag(qName, attributes);
      }

      elementStack.push(qName);

      if (qName.equals("div"))
      {
         divIsExtract = "extract".equals(attributes.getValue("type"));

         if (!divIsExtract)
         {
            return;
         }

         String id = UUID.randomUUID().toString();
         ExtractDTO extract = new ExtractDTO(id);

         // inherit manuscript author by default
         // TODO: parse per-extract authors
         extract.setAuthor(manuscript.getAuthor());

         String lineRef = attributes.getValue("n");
         extract.setLineRef(lineRef);

         manuscript.addExtract(extract);

         String corresp = attributes.getValue("corresp");
         if (corresp != null)
         {
            String playId = corresp.substring(corresp.indexOf('#') + 1);
            extract.setPlayId(playId);
         }

         objectStack.push(extract);

         XmlStringBuilder xsb = new XmlStringBuilder();
         xsb.startTag(qName, attributes);
         rawMode = true;
         objectStack.push(xsb);
      }
      else if (qName.equals("sp") && isParentElement("div") && divIsExtract)
      {
         String who = attributes.getValue("who");
         if (who != null)
         {
            String[] speakerRefs = who.trim().split("\\s+");
            for (String ref : speakerRefs)
            {
               String speakerId = ref.substring(ref.indexOf('#') + 1);

               ExtractDTO extract = (ExtractDTO)objectStack.get(objectStack.size() - 2);
               extract.addSpeaker(speakerId);
            }
         }
      }
   }

   @Override
   public void endElement(String uri, String localName, String qName) throws SAXException
   {
      // just finished dramatic extract definition
      if (qName.equals("div") && divIsExtract)
      {
         XmlStringBuilder xsb = (XmlStringBuilder)objectStack.pop();
         xsb.endTag("div");

         ExtractDTO extract = (ExtractDTO)objectStack.pop();

         // parse TEI XML string into W3C DOM
         String tei = xsb.toString().trim();
         StringReader sr = new StringReader(tei);
         InputSource is = new InputSource(sr);
         try {
            Document document = documentBuilder.parse(is);
            extract.setTEIContent(document);
         }
         catch (IOException e)
         {
            throw new IllegalStateException("I/O exception from a string reader while parsing document?", e);
         }

         rawMode = false;
      }

      elementStack.pop();

      if (rawMode)
      {
         XmlStringBuilder xsb = (XmlStringBuilder)objectStack.peek();
         xsb.endTag(qName);
      }
   }

   @Override
   public void characters(char[] ch, int start, int length) throws SAXException
   {
      String value = new String(ch, start, length).replaceAll("\\s+", " ");

      if (rawMode)
      {
         XmlStringBuilder xsb = (XmlStringBuilder)objectStack.peek();
         xsb.text(value);
         return;
      }

      if (isCurrentElement("title"))
      {
         manuscript.setTitle(value);
      }
      else if (isCurrentElement("persName") && isParentElement("author"))
      {
         manuscript.setAuthor(value);
      }
   }


   private boolean isCurrentElement(String qName)
   {
      return !elementStack.empty() && elementStack.peek().equals(qName);
   }

   private boolean isParentElement(String qName)
   {
      return elementStack.size() > 2 && elementStack.get(elementStack.size() - 2).equals(qName);
   }

   private boolean isAncestorElement(String qName)
   {
      if (elementStack.empty())
      {
         return false;
      }

      for (int i = 0; i < elementStack.size(); i++)
      {
         if (elementStack.get(i).equals(qName))
         {
            return true;
         }
      }

      return false;
   }

}
