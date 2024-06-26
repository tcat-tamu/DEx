package edu.tamu.tcat.dex.trc.extract.search.solr;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import edu.tamu.tcat.dex.trc.extract.DramaticExtract;
import edu.tamu.tcat.dex.trc.extract.ManuscriptRef;
import edu.tamu.tcat.dex.trc.extract.SourceRef;
import edu.tamu.tcat.trc.search.SearchException;

public class ExtractSearchProxy
{
   public static class ReferenceDTO
   {
      public String id;
      public String display;

      public static ReferenceDTO create(String id, String display)
      {
         ReferenceDTO a = new ReferenceDTO();
         a.id = id;
         a.display = display;
         return a;
      }
   }

   public String id;
   public ReferenceDTO manuscript;
   public String author;
   public String folio;
   public int msOrder;
   public ReferenceDTO source;
   public String sourceLineRef;
   public String normalized;
   public String original;
   public final List<ReferenceDTO> speakers = new ArrayList<>();
   public final List<ReferenceDTO> playwrights = new ArrayList<>();

   public static ExtractSearchProxy create(DramaticExtract extract, ExtractManipulationUtil extractManipulationUtil) throws SearchException
   {
      ExtractSearchProxy proxy = new ExtractSearchProxy();

      proxy.id = extract.getId();
      proxy.folio = extract.getFolioIdent();
      proxy.msOrder = extract.getManuscriptIndex();

      ManuscriptRef mRef = extract.getManuscriptRef();
      proxy.manuscript = ReferenceDTO.create(mRef.getId(), mRef.getDisplayTitle());

      proxy.author = extract.getAuthor();

      SourceRef srcRef = extract.getSource();
      proxy.source = ReferenceDTO.create(srcRef.getId(), srcRef.getDisplayTitle());
      proxy.sourceLineRef = srcRef.getLineReference();

      Document teiContent = extract.getTEIContent();
      
      try
      {
         proxy.normalized = extractManipulationUtil.toNormalized(teiContent);
         proxy.original = extractManipulationUtil.toOriginal(teiContent);
      }
      catch (Exception e)
      {
         teiContent.getDocumentElement();
         teiContent.toString();
         throw new SearchException("Unable to transform TEI for search proxy: " + teiContent, e);
      }

      extract.getSpeakerRefs().parallelStream()
         .map(ref -> ReferenceDTO.create(ref.getId(), ref.getDisplayName()))
         .forEach(proxy.speakers::add);

      extract.getPlaywrightRefs().parallelStream()
         .map(ref -> ReferenceDTO.create(ref.getId(), ref.getDisplayName()))
         .forEach(proxy.playwrights::add);

      return proxy;
   }
}
