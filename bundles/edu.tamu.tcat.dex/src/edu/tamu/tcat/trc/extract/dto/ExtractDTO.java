package edu.tamu.tcat.trc.extract.dto;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import edu.tamu.tcat.dex.trc.entry.DramaticExtract;
import edu.tamu.tcat.dex.trc.entry.SourceRef;

public class ExtractDTO
{
   private static final Logger logger = Logger.getLogger(ExtractDTO.class.getName());

   public String id;
   public String author;
   public String manuscriptId;
   public String source;
   public String sourceRef;
   public Set<String> speakerIds = new HashSet<>();
   public String teiContent;

   /**
    * Creates a new domain model object given a data transfer object
    *
    * @param dto Data transfer object representing dramatic extract to create
    * @return Created dramatic extract domain model
    */
   public static DramaticExtract instantiate(ExtractDTO dto)
   {
      DramaticExtractImpl extract = new DramaticExtractImpl();
      extract.id = dto.id;
      extract.author = dto.author;
      extract.manuscriptId = dto.manuscriptId;
      extract.source = new SourceRef()
      {
         @Override
         public String getId()
         {
            return dto.id;
         }

         @Override
         public String getLineReference()
         {
            return dto.sourceRef;
         }
      };

      extract.speakers = Collections.unmodifiableSet(dto.speakerIds);

      try
      {
         StringReader reader = new StringReader(dto.teiContent);
         DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
         DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
         extract.teiDoc = docBuilder.parse(new InputSource(reader));
      }
      catch (Exception e)
      {
         logger.log(Level.WARNING, "Unable to parse TEI DOM", e);
      }

      return extract;
   }

   public static ExtractDTO create(DramaticExtract extract)
   {
      ExtractDTO dto = new ExtractDTO();

      dto.id = extract.getId();
      dto.author = extract.getAuthor();
      dto.manuscriptId = extract.getManuscriptId();

      SourceRef sourceRef = extract.getSource();
      dto.source = sourceRef == null ? null : sourceRef.getId();
      dto.sourceRef = sourceRef == null ? null : sourceRef.getLineReference();

      dto.speakerIds = Collections.unmodifiableSet(extract.getSpeakerIds());

      try
      {
         Document teiContent = extract.getTEIContent();
         StringWriter writer = new StringWriter();
         TransformerFactory transformerFactory = TransformerFactory.newInstance();
         Transformer transformer = transformerFactory.newTransformer();
         transformer.transform(new DOMSource(teiContent), new StreamResult(writer));
         dto.teiContent = writer.toString();
      }
      catch (Exception e)
      {
         logger.log(Level.WARNING, "Unable to serialize TEI DOM", e);
      }

      return dto;
   }


   public static class DramaticExtractImpl implements DramaticExtract
   {
      private String id;
      private String author;
      private String manuscriptId;
      private SourceRef source;
      private Set<String> speakers;
      private Document teiDoc;


      @Override
      public String getId()
      {
         return id;
      }

      @Override
      public String getAuthor()
      {
         return author;
      }

      @Override
      public String getManuscriptId()
      {
         return manuscriptId;
      }

      @Override
      public SourceRef getSource()
      {
         return source;
      }

      @Override
      public Set<String> getSpeakerIds()
      {
         return speakers;
      }

      @Override
      public Document getTEIContent()
      {
         return teiDoc;
      }

   }
}
