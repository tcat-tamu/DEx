package edu.tamu.tcat.dex.trc.extract.dto;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
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

import edu.tamu.tcat.dex.trc.extract.DramaticExtract;
import edu.tamu.tcat.dex.trc.extract.ManuscriptRef;
import edu.tamu.tcat.dex.trc.extract.PlaywrightRef;
import edu.tamu.tcat.dex.trc.extract.SourceRef;
import edu.tamu.tcat.dex.trc.extract.SpeakerRef;

public class ExtractDTO
{
   private static final Logger logger = Logger.getLogger(ExtractDTO.class.getName());

   public String id;
   public int msIndex;
   public String folioIdent;
   public String author;
   public ReferenceDTO manuscript = new ReferenceDTO();
   public ReferenceDTO source = new ReferenceDTO();
   public String sourceRef;
   public Set<ReferenceDTO> speakers = new HashSet<>();
   public Set<ReferenceDTO> playwrights = new HashSet<>();
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
      extract.msIndex = dto.msIndex;
      extract.folioIdent = dto.folioIdent;
      extract.author = dto.author;

      if (dto.manuscript != null)
      {
         extract.manuscript = new ManuscriptRef()
         {
            @Override
            public String getId()
            {
               return dto.manuscript.id;
            }

            @Override
            public String getDisplayTitle()
            {
               return dto.manuscript.title;
            }
         };
      }

      extract.source = new SourceRef()
      {
         @Override
         public String getId()
         {
            return dto.source.id;
         }

         @Override
         public String getDisplayTitle()
         {
            return dto.source.title;
         }

         @Override
         public String getLineReference()
         {
            return dto.sourceRef;
         }
      };

      extract.speakers = dto.speakers.parallelStream()
            .map(refDTO ->
            {
               SpeakerRef ref = new SpeakerRef()
               {

                  @Override
                  public String getId()
                  {
                     return refDTO.id;
                  }

                  @Override
                  public String getDisplayName()
                  {
                     return refDTO.title;
                  }
               };

               return ref;
            })
            .collect(Collectors.toSet());

      extract.playwrights = dto.playwrights.parallelStream()
            .map(ExtractDTO::adaptPlaywrightReference)
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

   private static PlaywrightRef adaptPlaywrightReference(ReferenceDTO refDTO)
   {
      return new PlaywrightRef()
      {
         @Override
         public String getId()
         {
            return refDTO.id;
         }

         @Override
         public String getDisplayName()
         {
            return refDTO.title;
         }
      };
   }

   public static ExtractDTO create(DramaticExtract extract)
   {
      ExtractDTO dto = new ExtractDTO();

      dto.id = extract.getId();
      dto.msIndex = extract.getManuscriptIndex();
      dto.folioIdent = extract.getFolioIdent();
      dto.author = extract.getAuthor();

      ManuscriptRef mRef = extract.getManuscriptRef();
      dto.manuscript = ReferenceDTO.create(mRef.getId(), mRef.getDisplayTitle());

      SourceRef sourceRef = extract.getSource();
      dto.source = sourceRef == null ? null : ReferenceDTO.create(sourceRef.getId(), sourceRef.getDisplayTitle());
      dto.sourceRef = sourceRef == null ? null : sourceRef.getLineReference();

      dto.speakers = extract.getSpeakerRefs().parallelStream()
            .map(ref -> ReferenceDTO.create(ref.getId(), ref.getDisplayName()))
            .collect(Collectors.toSet());

      dto.playwrights = extract.getPlaywrightRefs().parallelStream()
            .map(ref -> ReferenceDTO.create(ref.getId(), ref.getDisplayName()))
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
      private String id;
      private int msIndex;
      private String folioIdent;
      private String author;
      private ManuscriptRef manuscript;
      private SourceRef source;
      private Set<SpeakerRef> speakers;
      private Set<PlaywrightRef> playwrights;
      private Document teiDoc;


      @Override
      public String getId()
      {
         return id;
      }

      @Override
      public int getManuscriptIndex()
      {
         return msIndex;
      }

      @Override
      public String getFolioIdent()
      {
         return folioIdent;
      }

      @Override
      public String getAuthor()
      {
         return author;
      }

      @Override
      public ManuscriptRef getManuscriptRef()
      {
         return manuscript;
      }

      @Override
      public SourceRef getSource()
      {
         return source;
      }

      @Override
      public Set<SpeakerRef> getSpeakerRefs()
      {
         return speakers;
      }

      @Override
      public Set<PlaywrightRef> getPlaywrightRefs()
      {
         return playwrights;
      }

      @Override
      public Document getTEIContent()
      {
         return teiDoc;
      }
   }
}
