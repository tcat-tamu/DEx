package edu.tamu.tcat.trc.extract.postgres;

import java.io.StringWriter;
import java.net.URI;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import edu.tamu.tcat.dex.trc.entry.DramaticExtractException;
import edu.tamu.tcat.dex.trc.entry.EditExtractCommand;
import edu.tamu.tcat.trc.extract.dto.ExtractDTO;

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
   public void setManuscript(URI manuscript)
   {
      dto.manuscript = manuscript.toString();
   }

   @Override
   public void setSource(URI source)
   {
      dto.source = source.toString();
   }

   @Override
   public void setSourceRef(String sourceRef)
   {
      dto.sourceRef = sourceRef;
   }

   @Override
   public void setSpeakers(Set<URI> speakers)
   {
      dto.speakers = speakers.stream()
            .map(URI::toString)
            .collect(Collectors.toSet());
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
         dto.teiContent = writer.toString();
      }
      catch (Exception e)
      {
         logger.log(Level.WARNING, "Unable to serialize TEI DOM", e);
      }
   }

   @Override
   public Future<String> execute() throws DramaticExtractException
   {
      Objects.requireNonNull(commitHook);
      return commitHook.apply(dto);
   }
}
