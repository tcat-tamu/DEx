package edu.tamu.tcat.dex.trc.extract.dto;

import static java.text.MessageFormat.format;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

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

   public static ExtractDTO adapt(DramaticExtract extract)
   {
      ExtractDTO dto = new ExtractDTO();

      dto.id = extract.getId();
      dto.msIndex = extract.getManuscriptIndex();
      dto.folioIdent = extract.getFolioIdent();
      dto.author = extract.getAuthor();

      dto.manuscript = adapt(extract.getManuscriptRef());

      SourceRef sourceRef = extract.getSource();
      dto.source = adapt(sourceRef);
      dto.sourceRef = sourceRef == null ? null : sourceRef.getLineReference();

      dto.speakers = extract.getSpeakerRefs().parallelStream()
            .map(ExtractDTO::adapt)
            .collect(Collectors.toSet());

      dto.playwrights = extract.getPlaywrightRefs().parallelStream()
            .map(ExtractDTO::adapt)
            .collect(Collectors.toSet());

      dto.teiContent = getTeiContent(extract);

      return dto;
   }
   
   private static ReferenceDTO adapt(SpeakerRef ref) 
   {
      if (ref == null)
         return null;
      
      ReferenceDTO dto = new ReferenceDTO();
      dto.id = ref.getId();
      dto.title = ref.getDisplayName();
      
      return dto;
   }
   
   private static ReferenceDTO adapt(SourceRef ref) 
   {
      if (ref == null)
         return null;
      
      ReferenceDTO dto = new ReferenceDTO();
      dto.id = ref.getId();
      dto.title = ref.getDisplayTitle();
      
      return dto;
   }
   
   private static ReferenceDTO adapt(PlaywrightRef ref) 
   {
      if (ref == null)
         return null;
      
      ReferenceDTO dto = new ReferenceDTO();
      dto.id = ref.getId();
      dto.title = ref.getDisplayName();
      
      return dto;
   }
   
   private static ReferenceDTO adapt(ManuscriptRef ref) 
   {
      if (ref == null)
         return null;
            
      ReferenceDTO dto = new ReferenceDTO();
      dto.id = ref.getId();
      dto.title = ref.getDisplayTitle();
      
      return dto;
   }

   private static String getTeiContent(DramaticExtract extract) throws TransformerFactoryConfigurationError
   {
      Document teiContent = extract.getTEIContent();
      if (teiContent == null)
      {
         logger.log(Level.WARNING, format("No TEI content for extract {0}", extract.getId()));
         return "";
      }
      
      try
      {
         StringWriter writer = new StringWriter();
         TransformerFactory transformerFactory = TransformerFactory.newInstance();
         Transformer transformer = transformerFactory.newTransformer();
         transformer.transform(new DOMSource(teiContent), new StreamResult(writer));
         return writer.toString();
      }
      catch (Exception e)
      {
         logger.log(Level.WARNING, "Failed to serialize extract TEI", e);
         return "";
      }
   }
}
