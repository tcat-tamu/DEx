package edu.tamu.tcat.trc.extract.dto;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import edu.tamu.tcat.dex.trc.entry.DramaticExtract;

public class ExtractDTO
{
   private static final Logger logger = Logger.getLogger(ExtractDTO.class.getName());

   public URI id;
   public String author;
   public String manuscript;
   public String source;
   public String sourceRef;
   public Set<String> speakers;
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
      extract.manuscript = URI.create(dto.manuscript);
      extract.source = URI.create(dto.source);
      extract.sourceRef = dto.sourceRef;

      extract.speakers = dto.speakers.stream()
            .map(URI::create)
            .collect(Collectors.toSet());

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

      URI manuscriptUri = extract.getManuscript();
      dto.manuscript = manuscriptUri == null ? null : manuscriptUri.toString();

      URI sourceUri = extract.getSource();
      dto.source = sourceUri == null ? null : sourceUri.toString();

      dto.sourceRef = extract.getSourceRef();

      dto.speakers = extract.getSpeakers().stream()
            .map(URI::toString)
            .collect(Collectors.toSet());

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
      private URI id;
      private String author;
      private URI manuscript;
      private URI source;
      private String sourceRef;
      private Set<URI> speakers;
      private Document teiDoc;


      @Override
      public URI getId()
      {
         return id;
      }

      @Override
      public String getAuthor()
      {
         return author;
      }

      @Override
      public URI getManuscript()
      {
         return manuscript;
      }

      @Override
      public URI getSource()
      {
         return source;
      }

      @Override
      public String getSourceRef()
      {
         return sourceRef;
      }

      @Override
      public Set<URI> getSpeakers()
      {
         return speakers;
      }

      @Override
      public Document getTEIContent()
      {
         return teiDoc;
      }

      @Override
      public String getOriginalContent()
      {
         throw new UnsupportedOperationException();
      }

      @Override
      public String getNormalizedContent()
      {
         throw new UnsupportedOperationException();
      }

   }
}
