package edu.tamu.tcat.trc.extract.postgres;

import java.io.StringWriter;
import java.util.Collections;
import java.util.HashSet;
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

import edu.tamu.tcat.dex.trc.entry.DramaticExtract;
import edu.tamu.tcat.dex.trc.entry.DramaticExtractException;
import edu.tamu.tcat.dex.trc.entry.EditExtractCommand;
import edu.tamu.tcat.dex.trc.entry.Pair;
import edu.tamu.tcat.trc.extract.dto.ExtractDTO;
import edu.tamu.tcat.trc.extract.dto.ReferenceDTO;

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
   public void setAll(DramaticExtract extract)
   {
      ExtractDTO dto = ExtractDTO.create(extract);

      setAuthor(dto.author);
      setManuscriptId(dto.manuscript == null ? null : dto.manuscript.id);
      setManuscriptTitle(dto.manuscript == null ? null : dto.manuscript.title);
      setSourceId(dto.source == null ? null : dto.source.id);
      setSourceTitle(dto.source == null ? null : dto.source.title);
      setSourceRef(dto.sourceRef);
      setTEIContent(dto.teiContent);
      setFolioIdentifier(dto.folioIdent);
      setMsIndex(dto.msIndex);

      // speedup by not using converting to API types just to convert back
      this.dto.speakers = Collections.unmodifiableSet(new HashSet<>(dto.speakers));
      this.dto.playwrights = Collections.unmodifiableSet(new HashSet<>(dto.playwrights));

//      setSpeakers(dto.speakers.parallelStream()
//            .map(anchor -> Pair.of(anchor.id, anchor.title))
//            .collect(Collectors.toSet()));
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
   public void setSpeakers(Set<Pair<String, String>> speakers)
   {
      dto.speakers = speakers.parallelStream()
            .map(pair -> ReferenceDTO.create(pair.first, pair.second))
            .collect(Collectors.toSet());
   }

   @Override
   public void setPlaywrights(Set<Pair<String, String>> playwrights)
   {
      dto.playwrights = playwrights.parallelStream()
            .map(pair -> ReferenceDTO.create(pair.first, pair.second))
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
