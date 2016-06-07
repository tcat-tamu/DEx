package edu.tamu.tcat.dex.trc.extract.postgres;

import java.io.StringWriter;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import edu.tamu.tcat.dex.trc.extract.DramaticExtractException;
import edu.tamu.tcat.dex.trc.extract.EditExtractCommand;
import edu.tamu.tcat.dex.trc.extract.dto.ExtractDTO;
import edu.tamu.tcat.dex.trc.extract.dto.ReferenceDTO;

public class EditExtractCommandImpl implements EditExtractCommand
{
   private static final Logger logger = Logger.getLogger(EditExtractCommandImpl.class.getName());

   private final ExtractDTO dto;

   private Function<ExtractDTO, Future<String>> commitHook;


   public EditExtractCommandImpl(ExtractDTO dto)
   {
      this.dto = dto;
   }

   public void setCommitHook(Function<ExtractDTO, Future<String>> hook)
   {
      commitHook = hook;
   }

   @Override
   public void setAuthor(String author)
   {
      dto.author = author;
   }

   @Override
   public void setFolioIdentifier(String folio)
   {
      dto.folioIdent = folio;
   }

   @Override
   public void setMsIndex(int order)
   {
      dto.msIndex = order;
   }
   @Override
   public void setManuscriptId(String manuscriptId)
   {
      dto.manuscript.id = manuscriptId;
   }

   @Override
   public void setManuscriptTitle(String manuscriptTitle)
   {
      dto.manuscript.title = manuscriptTitle;
   }

   @Override
   public void setSourceId(String sourceId)
   {
      dto.source.id = sourceId;
   }

   @Override
   public void setSourceTitle(String sourceTitle)
   {
      dto.source.title = sourceTitle;
   }

   @Override
   public void setSourceRef(String sourceRef)
   {
      dto.sourceRef = sourceRef;
   }

   @Override
   public void setSpeakers(Set<ReferenceDTO> speakers)
   {
      dto.speakers = speakers;
   }

   @Override
   public void setPlaywrights(Set<ReferenceDTO> playwrights)
   {
      dto.playwrights = playwrights;
   }

   @Override
   public void setTEIContent(Document teiContent)
   {
      try
      {
         StringWriter writer = new StringWriter();
         TransformerFactory transformerFactory = TransformerFactory.newInstance();
         Transformer transformer = transformerFactory.newTransformer();
         transformer.transform(new DOMSource(teiContent), new StreamResult(writer));
         setTEIContent(writer.toString());
      }
      catch (Exception e)
      {
         logger.log(Level.WARNING, "Unable to serialize TEI DOM", e);
      }
   }

   @Override
   public void setTEIContent(String teiContent)
   {
      dto.teiContent = teiContent;
   }

   @Override
   public Future<String> execute() throws DramaticExtractException
   {
      Objects.requireNonNull(commitHook);
      return commitHook.apply(dto);
   }
}
