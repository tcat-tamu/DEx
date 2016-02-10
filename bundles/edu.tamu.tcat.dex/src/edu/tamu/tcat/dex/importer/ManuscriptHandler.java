package edu.tamu.tcat.dex.importer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Stack;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringEscapeUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.tamu.tcat.dex.importer.model.ExtractImportDTO;
import edu.tamu.tcat.dex.importer.model.ManuscriptImportDTO;

class ManuscriptHandler extends DefaultHandler
{
   private static final Logger logger = Logger.getLogger(PeopleAndPlaysHandler.class.getName());

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


   private Stack<String> elementStack;
   private Stack<Object> objectStack;

   private boolean rawMode;
   private int extractNumber;
   private String currentFolioIdent;

   private ManuscriptImportDTO manuscript;

   /**
    * Set to {@code false} when the current {@code <div>} element does not represent a dramatic
    * extract, but, for example, a heading.
    */
   private boolean inExtractDiv;    // FIXME does not get set to false when no-longer in extract div


   public ManuscriptImportDTO getManuscript()
   {
      return manuscript;
   }

   @Override
   public void startDocument() throws SAXException
   {
      elementStack = new Stack<>();
      objectStack = new Stack<>();

      manuscript = new ManuscriptImportDTO();

      currentFolioIdent = null;
      extractNumber = 0;
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
         inExtractDiv = "extract".equals(attributes.getValue("type"));
         if (inExtractDiv)
            handleExtractDiv(qName, attributes);
      }
      else if (qName.equals("sp") && isParentElement("div") && inExtractDiv)
      {
         handleSpeakerStartElement(attributes);
      }
      else if (qName.equals("pb"))
      {
         currentFolioIdent = attributes.getValue("n");
      }
      else if (qName.equals("graphic") && isParentElement("facsimile"))
      {
         try
         {
            // append facsimile link to list of links
            URI target = new URI(attributes.getValue("url"));

            // HACK HACK HACK comma-separated list of HTML links‽‽‽
            if (!manuscript.links.isEmpty())
            {
               manuscript.links += ", ";
            }
            manuscript.links += "<a href=\"" + target.toString() + "\">Facsimile</a>";
         }
         catch (URISyntaxException e) {
            logger.log(Level.WARNING, "malformed URI in link attribute", e);
         }
      }
      else if (qName.equals("bibl") && isParentElement("listBibl"))
      {
         // push link title
         objectStack.push(attributes.getValue("type"));
      }
      else if (qName.equals("link") && isParentElement("bibl"))
      {
         // push link target
         URI target = null;
         try
         {
            target = new URI(attributes.getValue("target"));
         }
         catch (URISyntaxException e)
         {
            logger.log(Level.WARNING, "malformed URI in link attribute", e);
         }
         objectStack.push(target);
      }
   }

   private void handleSpeakerStartElement(Attributes attributes)
   {
      String who = attributes.getValue("who");
      if (who != null)
      {
         String[] speakerRefs = who.trim().split("\\s+");
         for (String ref : speakerRefs)
         {
            String speakerId = ref.substring(ref.indexOf('#') + 1);

            ExtractImportDTO extract = (ExtractImportDTO)objectStack.get(objectStack.size() - 2);
            extract.speakerIds.add(speakerId);
         }
      }
   }

   private void handleExtractDiv(String qName, Attributes attributes)
   {
      ExtractImportDTO extract = new ExtractImportDTO();
      extract.id = UUID.randomUUID().toString();
      extract.msIndex = extractNumber++;

      if (currentFolioIdent != null) {
         extract.folioIdent = currentFolioIdent;
      }

      // inherit manuscript author by default
      // TODO: parse per-extract authors
      extract.author = manuscript.author;

      extract.sourceRef = attributes.getValue("n");

      manuscript.extracts.add(extract);

      String corresp = attributes.getValue("corresp");
      if (corresp != null)
      {
         extract.sourceId = corresp.substring(corresp.indexOf('#') + 1);
      }

      objectStack.push(extract);

      XmlStringBuilder xsb = new XmlStringBuilder();
      xsb.startTag(qName, attributes);
      rawMode = true;
      objectStack.push(xsb);
   }

   @Override
   public void endElement(String uri, String localName, String qName) throws SAXException
   {
      // just finished dramatic extract definition
      if (qName.equals("div") && inExtractDiv)
      {
         XmlStringBuilder xsb = (XmlStringBuilder)objectStack.pop();
         xsb.endTag("div");

         ExtractImportDTO extract = (ExtractImportDTO)objectStack.pop();

         // parse TEI XML string into W3C DOM
         extract.teiContent = xsb.toString().trim();
         inExtractDiv = false;
         rawMode = false;
      }
      else if (qName.equals("bibl") && isParentElement("listBibl"))
      {
         // append link to manuscript list of links
         URI target = (URI)objectStack.pop();
         String title = (String)objectStack.pop();

         if (target != null && title != null && !title.isEmpty())
         {
            // HACK HACK HACK comma-separated list of HTML links‽‽‽
            if (manuscript.links == null)
            {
               manuscript.links = "";
            }
            else if (!manuscript.links.isEmpty())
            {
               manuscript.links += ", ";
            }
            manuscript.links += "<a href=\"" + target.toString() + "\">" + title + "</a>";
         }
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
      }
      else
      {
         if (isCurrentElement("title") && isParentElement("titleStmt"))
         {
            manuscript.title = value.trim();
         }
         else if (isCurrentElement("persName") && isParentElement("author"))
         {
            manuscript.author = value.trim();
         }
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
}
