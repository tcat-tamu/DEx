package edu.tamu.tcat.dex.trc.extract.dto;

import static java.text.MessageFormat.format;

import java.io.StringReader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import edu.tamu.tcat.dex.trc.extract.DramaticExtract;
import edu.tamu.tcat.dex.trc.extract.ManuscriptRef;
import edu.tamu.tcat.dex.trc.extract.PlaywrightRef;
import edu.tamu.tcat.dex.trc.extract.SourceRef;
import edu.tamu.tcat.dex.trc.extract.SpeakerRef;

public class DramaticExtractImpl implements DramaticExtract
{
   private static final Logger logger = Logger.getLogger(DramaticExtractImpl.class.getName());
   
   private final String id;
   private final int msIndex;
   private final String folioIdent;
   private final String author;
   private final ManuscriptRef manuscript;
   private final SourceRef source;
   private final Set<SpeakerRef> speakers;
   private final Set<PlaywrightRef> playwrights;
   
   private final Document teiDoc;

   public DramaticExtractImpl(ExtractDTO dto) 
   {
      this.id = dto.id;
      this.msIndex = dto.msIndex;
      this.folioIdent = dto.folioIdent;
      this.author = dto.author;

      this.manuscript = dto.manuscript == null ? null : new ManscriptRefImpl(dto.manuscript);
      this.source = new SourceRefImpl(dto.source,  dto.sourceRef);

      this.speakers = dto.speakers.parallelStream()
            .map(SpeakerImpl::new)
            .collect(Collectors.toSet());

      this.playwrights = dto.playwrights.parallelStream()
            .map(PlaywrightRefImpl::new)
            .collect(Collectors.toSet());

      this.teiDoc = parseContent(dto);
   }

   private Document parseContent(ExtractDTO dto)
   {
      try (StringReader reader = new StringReader(dto.teiContent))
      {
         return DocumentBuilderFactory
                     .newInstance()
                     .newDocumentBuilder()
                     .parse(new InputSource(reader));
      }
      catch (Exception e)
      {
         logger.log(Level.WARNING, format("Unable to parse extract TEI {0}", dto.teiContent), e);
         return null;
      }
   }

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
   
   public static class ManscriptRefImpl implements ManuscriptRef
   {
      private final String id; 
      private final String title;
      
      public ManscriptRefImpl(ReferenceDTO dto)
      {
         id = dto.id;
         title = dto.title;
      }

      @Override
      public String getId()
      {
         return id;
      }

      @Override
      public String getDisplayTitle()
      {
         return title;
      }
   }
   
   public static class SourceRefImpl implements SourceRef
   {
      private final String id; 
      private final String title;
      private final String lineReference;
      
      public SourceRefImpl(ReferenceDTO dto, String lineReference)
      {
         this.lineReference = lineReference;
         id = dto.id;
         title = dto.title;
         
      }
      
      @Override
      public String getId()
      {
         return id;
      }

      @Override
      public String getDisplayTitle()
      {
         return title;
      }

      @Override
      public String getLineReference()
      {
         return lineReference;
      }
   }
   
   public static class SpeakerImpl implements SpeakerRef
   {
      private final String id; 
      private final String title;
      
      public SpeakerImpl(ReferenceDTO dto)
      {
         id = dto.id;
         title = dto.title;
      }
      
      @Override
      public String getId()
      {
         return id;
      }

      @Override
      public String getDisplayName()
      {
         return title;
      }
   }
   
   public static class PlaywrightRefImpl implements PlaywrightRef
   {
      private final String id; 
      private final String title;
      
      public PlaywrightRefImpl(ReferenceDTO dto)
      {
         id = dto.id;
         title = dto.title;
      }
      
      @Override
      public String getId()
      {
         return id;
      }
      
      @Override
      public String getDisplayName()
      {
         return title;
      }
   }
}