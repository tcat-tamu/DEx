package edu.tamu.tcat.dex.importer;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.tamu.tcat.dex.importer.model.CharacterDTO;
import edu.tamu.tcat.dex.importer.model.PlayDTO;
import edu.tamu.tcat.dex.importer.model.PlayDTO.PlaywrightReferenceDTO;
import edu.tamu.tcat.dex.importer.model.PlaywrightDTO;

class PeopleAndPlaysHandler extends DefaultHandler
{
   /**
    * data transfer objects instantiated during the parse
    */
   private Map<String, PlayDTO> plays;
   private Map<String, CharacterDTO> characters;
   private Map<String, PlaywrightDTO> playwrights;

   /**
    * keep track of context while traversing XML tree
    */
   private Stack<String> elementStack;
   private Stack<Object> objectStack;

   /**
    * @return plays from last successful parse
    */
   public Map<String, PlayDTO> getPlays()
   {
      return new HashMap<>(plays);
   }

   /**
    * @return characters from last successful parse
    */
   public Map<String, CharacterDTO> getCharacters()
   {
      return new HashMap<>(characters);
   }

   /**
    * @return playwrights from last successful parse
    */
   public Map<String, PlaywrightDTO> getPlaywrights()
   {
      return new HashMap<>(playwrights);
   }

   /**
    * Called at the start of an XML document
    */
   @Override
   public void startDocument() throws SAXException
   {
      plays = new HashMap<>();
      characters = new HashMap<>();
      playwrights = new HashMap<>();

      elementStack = new Stack<>();
      objectStack = new Stack<>();
   }

   /**
    * Called when the end of the XML document is reached
    */
   @Override
   public void endDocument() throws SAXException
   {
      assert elementStack.empty() : "Leftover elements in element stack";
      assert objectStack.empty() : "Leftover objects in object stack";
   }

   /**
    * Called when an opening XML tag is encountered
    *
    * @param uri Element namespace URI (e.g. "http://www.w3.org/XML/1998/namespace")
    * @param localName Element name without the namespace prefix (e.g. "element")
    * @param qName Element name with namespace prefix (e.g. "xml:element")
    * @param attributes
    */
   @Override
   public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
   {
      elementStack.push(qName);

      if (qName.equals("bibl"))
      {
         // each bibl tag corresponds to a single play entity

         String id = attributes.getValue("xml:id");
         PlayDTO play = new PlayDTO(id);
         plays.put(id, play);
         objectStack.push(play);
      }
      else if (qName.equals("author") && getParentElement() != null && getParentElement().equals("bibl"))
      {
         // bibl/author forms a reference to a playright:
         //    bibl/author/text() is the name as it appears on the work
         //    bibl/author/@corresp is the ID of the referenced playwright (if known)

         String corresp = attributes.getValue("corresp");

         // strip leading '#' from reference
         String playwrightId = corresp == null ? null : corresp.substring(1);

         PlaywrightReferenceDTO playwrightRef = new PlaywrightReferenceDTO(playwrightId);

         PlayDTO play = (PlayDTO)objectStack.peek();
         play.addPlaywright(playwrightRef);

         objectStack.push(playwrightRef);
      }
      else if (qName.equals("person"))
      {
         // person tag corresponds to either a playwright or a character in a play

         if (attributes.getValue("role").equals("playwright"))
         {
            // person[@role="playwright"] corresponds to a single playwright entity

            String id = attributes.getValue("xml:id");
            PlaywrightDTO playwright = new PlaywrightDTO(id);
            playwrights.put(id, playwright);
            objectStack.push(playwright);
         }
         else if (attributes.getValue("role").equals("character"))
         {
            // person[@role="character"] corresponds to a single play character entity

            String id = attributes.getValue("xml:id");
            CharacterDTO character = new CharacterDTO(id);
            characters.put(id, character);
            objectStack.push(character);
         }
      }
      else if (qName.equals("ptr"))
      {
         // person[@role="character"]/notes/ptr/@target contains the ID of the play that the character appears in

         if (objectStack.peek() instanceof CharacterDTO)
         {
            String target = attributes.getValue("target");

            // strip leading '#' from reference if defined
            String playId = target.substring(1);

            CharacterDTO character = (CharacterDTO)objectStack.peek();
            character.addPlayId(playId);
         }
      }
   }

   /**
    * Called when a closing XML tag is encountered
    *
    * @param uri Element namespace URI (e.g. "http://www.w3.org/XML/1998/namespace")
    * @param localName Element name without the namespace prefix (e.g. "element")
    * @param qName Element name with namespace prefix (e.g. "xml:element")
    */
   @Override
   public void endElement(String uri, String localName, String qName) throws SAXException
   {
      elementStack.pop();

      if (qName.equals("bibl") || qName.equals("person"))
      {
         objectStack.pop();
      }
      else if (getCurrentElement() != null && getCurrentElement().equals("bibl"))
      {
         if (qName.equals("author"))
         {
            PlaywrightReferenceDTO playwrightRef = (PlaywrightReferenceDTO)objectStack.pop();

            PlayDTO play = (PlayDTO)objectStack.peek();
            play.addPlaywright(playwrightRef);
         }
      }
   }

   /**
    * Handles text nodes between element tags
    *
    * @param ch
    * @param start
    * @param length
    */
   @Override
   public void characters(char[] ch, int start, int length) throws SAXException
   {
      String value = new String(ch, start, length).trim();

      if (value.length() == 0)
      {
         // ignore white space
         return;
      }

      if (getParentElement() == null)
      {
         // we only care about nested elements (i.e. beneath bibl and person elements) from this point on
         return;
      }

      if (getParentElement().equals("bibl"))
      {
         if (getCurrentElement().equals("author"))
         {
            PlaywrightReferenceDTO playwrightRef = (PlaywrightReferenceDTO)objectStack.peek();
            playwrightRef.setDisplayName(value);
         }
         else if (getCurrentElement().equals("title"))
         {
            PlayDTO play = (PlayDTO)objectStack.peek();
            play.addTitle(value);
         }
         else if (getCurrentElement().equals("edition"))
         {
            PlayDTO play = (PlayDTO)objectStack.peek();
            play.addEdition(value);
         }
         else if (getCurrentElement().equals("publisher"))
         {
            PlayDTO play = (PlayDTO)objectStack.peek();
            play.addPublisher(value);
         }
         else if (getCurrentElement().equals("editor"))
         {
            PlayDTO play = (PlayDTO)objectStack.peek();
            play.addEditor(value);
         }
         else if (getCurrentElement().equals("date"))
         {
            PlayDTO play = (PlayDTO)objectStack.peek();
            play.setDate(value);
         }
      }
      else if (getParentElement().equals("person"))
      {
         if (getCurrentElement().equals("persName"))
         {
            if (objectStack.peek() instanceof PlaywrightDTO)
            {
               PlaywrightDTO playwright = (PlaywrightDTO)objectStack.peek();
               playwright.addName(value);
            }
            else if(objectStack.peek() instanceof CharacterDTO)
            {
               CharacterDTO character = (CharacterDTO)objectStack.peek();
               character.addName(value);
            }
         }
      }
   }

   /**
    * @return name of the current element or null if at root of XML document.
    */
   private String getCurrentElement()
   {
      return elementStack.empty() ? null : elementStack.peek();
   }

   /**
    * @return name of parent element or null if current depth < 1
    */
   private String getParentElement()
   {
      return elementStack.size() < 2 ? null : elementStack.get(elementStack.size() - 2);
   }
}