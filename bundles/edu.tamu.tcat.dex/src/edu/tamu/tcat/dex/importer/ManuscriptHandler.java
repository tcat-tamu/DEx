package edu.tamu.tcat.dex.importer;

import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.tamu.tcat.dex.importer.model.ExtractDTO;
import edu.tamu.tcat.dex.importer.model.ManuscriptDTO;

class ManuscriptHandler extends DefaultHandler
{
   private static final Logger logger = Logger.getLogger(ManuscriptHandler.class.getName());

   private Stack<String> elementStack;
   private Stack<Object> objectStack;

   private boolean rawMode;

   private ManuscriptDTO manuscript;


   public ManuscriptDTO getManuscript()
   {
      return manuscript;
   }

   @Override
   public void startDocument() throws SAXException
   {
      elementStack = new Stack<>();
      objectStack = new Stack<>();

      manuscript = new ManuscriptDTO();

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
         StringBuilder sb = (StringBuilder)objectStack.peek();

         // write tag as string
         sb.append('<')
            .append(qName);

         int numAttributes = attributes.getLength();
         for (int i = 0; i < numAttributes; i++)
         {
            String attrName = attributes.getQName(i);
            String attrValue = attributes.getValue(i);

            sb.append(' ')
               .append(attrName)
               .append("=\"")
               .append(attrValue)
               .append('"');
         }

         sb.append('>');
      }

      elementStack.push(qName);

      if (qName.equals("div") && isParentElement("body"))
      {
         String extractId = attributes.getValue("n");
         if (extractId == null)
         {
            logger.log(Level.WARNING, "Encountered extract with null ID: Skipping.");
            return;
         }

         ExtractDTO extract = new ExtractDTO(extractId);
         manuscript.addExtract(extract);

         String corresp = attributes.getValue("corresp");
         if (corresp != null)
         {
            String playId = corresp.substring(corresp.indexOf('#') + 1);
            extract.setPlayId(playId);
         }

         objectStack.push(extract);

         StringBuilder sb = new StringBuilder();
         rawMode = true;
         objectStack.push(sb);
      }
      else if (qName.equals("sp") && isParentElement("div"))
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
      if (qName.equals("div"))
      {
         StringBuilder sb = (StringBuilder)objectStack.pop();

         ExtractDTO extract = (ExtractDTO)objectStack.pop();
         extract.setXMLContent(sb.toString().trim());

         rawMode = false;
      }

      elementStack.pop();

      if (rawMode)
      {
         StringBuilder sb = (StringBuilder)objectStack.peek();
         sb.append("</")
            .append(qName)
            .append('>');
      }
   }

   @Override
   public void characters(char[] ch, int start, int length) throws SAXException
   {
      String value = new String(ch, start, length).replaceAll("\\s+", " ");

      if (rawMode)
      {
         StringBuilder sb = (StringBuilder)objectStack.peek();
         sb.append(value);
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

}
