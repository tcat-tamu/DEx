package edu.tamu.tcat.dex.importer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.tamu.tcat.dex.importer.model.CharacterImportDTO;
import edu.tamu.tcat.dex.importer.model.PlayImportDTO;
import edu.tamu.tcat.dex.importer.model.PlayImportDTO.EditionDTO;
import edu.tamu.tcat.dex.importer.model.PlayImportDTO.PlaywrightReferenceDTO;
import edu.tamu.tcat.dex.importer.model.PlaywrightImportDTO;

class PeopleAndPlaysHandler extends DefaultHandler
{
   private static final Logger logger = Logger.getLogger(PeopleAndPlaysHandler.class.getName());

   /**
    * data transfer objects instantiated during the parse
    */
   private Map<String, PlayImportDTO> plays;
   private Map<String, CharacterImportDTO> characters;
   private Map<String, PlaywrightImportDTO> playwrights;

   /**
    * keep track of context while traversing XML tree
    */
   private Stack<String> elementStack;
   private Stack<Object> objectStack;

   /**
    * @return plays from last successful parse
    */
   public Map<String, PlayImportDTO> getPlays()
   {
      return new HashMap<>(plays);
   }

   /**
    * @return characters from last successful parse
    */
   public Map<String, CharacterImportDTO> getCharacters()
   {
      return new HashMap<>(characters);
   }

   /**
    * @return playwrightRefs from last successful parse
    */
   public Map<String, PlaywrightImportDTO> getPlaywrights()
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

         PlayImportDTO play = new PlayImportDTO();
         play.id = attributes.getValue("xml:id");

         plays.put(play.id, play);
         objectStack.push(play);
      }
      else if (qName.equals("author") && getParentElement() != null && getParentElement().equals("bibl"))
      {
         // bibl/author forms a reference to a playright:
         //    bibl/author/text() is the name as it appears on the work
         //    bibl/author/@corresp is the ID of the referenced playwright (if known)

         PlaywrightReferenceDTO playwrightRef = new PlaywrightReferenceDTO();

         // strip leading '#' from reference if it exists
         String corresp = attributes.getValue("corresp");
         playwrightRef.playwrightId = corresp == null ? null : corresp.substring(corresp.indexOf('#') + 1);

         PlayImportDTO play = (PlayImportDTO)objectStack.peek();
         play.playwrightRefs.add(playwrightRef);

         objectStack.push(playwrightRef);
      }
      else if (qName.equals("edition") && "bibl".equals(getParentElement()))
      {
         if (objectStack.peek() instanceof EditionDTO) {
            objectStack.pop();
         }

         EditionDTO edition = new EditionDTO();

         PlayImportDTO play = (PlayImportDTO)objectStack.peek();
         play.editions.add(edition);

         objectStack.push(edition);
      }
      else if (qName.equals("link") && objectStack.peek() instanceof EditionDTO)
      {
         EditionDTO edition = (EditionDTO)objectStack.peek();
         try {
            edition.link = new URI(attributes.getValue("target"));
         }
         catch (URISyntaxException e) {
            logger.log(Level.WARNING, "malformed URI in link attribute", e);
         }
      }
      else if (qName.equals("person"))
      {
         // person tag corresponds to either a playwright or a character in a play

         if (attributes.getValue("role").equals("playwright"))
         {
            // person[@role="playwright"] corresponds to a single playwright entity

            PlaywrightImportDTO playwright = new PlaywrightImportDTO();
            playwright.id = attributes.getValue("xml:id");

            playwrights.put(playwright.id, playwright);
            objectStack.push(playwright);
         }
         else if (attributes.getValue("role").equals("character"))
         {
            // person[@role="character"] corresponds to a single play character entity

            String id = attributes.getValue("xml:id");
            CharacterImportDTO character = new CharacterImportDTO();
            character.id = id;
            characters.put(id, character);
            objectStack.push(character);
         }
      }
      else if (qName.equals("persName") && "person".equals(getParentElement()))
      {
         StringBuilder nameBuilder = new StringBuilder();
         objectStack.push(nameBuilder);
      }
      else if (qName.equals("ptr"))
      {
         // person[@role="character"]/notes/ptr/@target contains the ID of the play that the character appears in

         if (objectStack.peek() instanceof CharacterImportDTO)
         {
            String target = attributes.getValue("target");

            // strip leading '#' from reference if defined
            String playId = target.substring(1);

            CharacterImportDTO character = (CharacterImportDTO)objectStack.peek();
            character.playIds.add(playId);
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

      if (qName.equals("bibl"))
      {
         if (objectStack.peek() instanceof EditionDTO) {
            objectStack.pop();
         }

         objectStack.pop();
      }
      else if (qName.equals("person"))
      {
         objectStack.pop();
      }
      else if (qName.equals("persName") && getCurrentElement().equals("person"))
      {
         String name = objectStack.pop().toString();

         if (objectStack.peek() instanceof PlaywrightImportDTO)
         {
            PlaywrightImportDTO playwright = (PlaywrightImportDTO)objectStack.peek();
            playwright.names.add(name);
         }
         else if(objectStack.peek() instanceof CharacterImportDTO)
         {
            CharacterImportDTO character = (CharacterImportDTO)objectStack.peek();
            character.names.add(name);
         }
      }
      else if (getCurrentElement() != null && getCurrentElement().equals("bibl"))
      {
         if (qName.equals("author"))
         {
            PlaywrightReferenceDTO playwrightRef = (PlaywrightReferenceDTO)objectStack.pop();

            PlayImportDTO play = (PlayImportDTO)objectStack.peek();
            play.playwrightRefs.add(playwrightRef);
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
            playwrightRef.displayName = value;
         }
         else if (getCurrentElement().equals("title"))
         {
            PlayImportDTO play = (PlayImportDTO)objectStack.peek();
            play.titles.add(value);
         }
         else if (getCurrentElement().equals("edition"))
         {
            EditionDTO edition = (EditionDTO)objectStack.peek();
            edition.title = value;
         }
         else if (getCurrentElement().equals("publisher"))
         {
            EditionDTO edition = (EditionDTO)objectStack.peek();
            edition.publisher = value;
         }
         else if (getCurrentElement().equals("editor"))
         {
            EditionDTO edition;

            if (objectStack.peek() instanceof EditionDTO)
            {
               // check if we had a preceding edition tag
               edition = (EditionDTO)objectStack.peek();
            }
            else
            {
               // add an edition on the fly
               PlayImportDTO play = (PlayImportDTO)objectStack.peek();
               edition = new EditionDTO();
               edition.title = play.titles.iterator().next();
               play.editions.add(edition);
               objectStack.push(edition);
            }

            edition.editors.add(value);
         }
         else if (getCurrentElement().equals("date"))
         {
            EditionDTO edition = (EditionDTO)objectStack.peek();
            edition.date = value;
         }
      }
      else if (getParentElement().equals("person"))
      {
         if (getCurrentElement().equals("persName"))
         {
            StringBuilder nameBuilder = (StringBuilder)objectStack.peek();
            nameBuilder.append(value);
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