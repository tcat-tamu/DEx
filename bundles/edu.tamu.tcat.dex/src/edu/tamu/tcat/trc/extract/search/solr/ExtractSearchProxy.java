package edu.tamu.tcat.trc.extract.search.solr;

import java.util.ArrayList;
import java.util.List;

import edu.tamu.tcat.dex.trc.entry.DramaticExtract;
import edu.tamu.tcat.dex.trc.entry.ManuscriptRef;
import edu.tamu.tcat.dex.trc.entry.SourceRef;
import edu.tamu.tcat.dex.trc.entry.tei.transform.ExtractManipulationException;
import edu.tamu.tcat.dex.trc.entry.tei.transform.ExtractManipulationUtil;
import edu.tamu.tcat.trc.entries.search.SearchException;

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
   public ReferenceDTO source;
   public String sourceLineRef;
   public String normalized;
   public String original;
   public final List<ReferenceDTO> speakers = new ArrayList<>();

   public static ExtractSearchProxy create(DramaticExtract extract, ExtractManipulationUtil extractManipulationUtil) throws SearchException
   {
      ExtractSearchProxy proxy = new ExtractSearchProxy();

      proxy.id = extract.getId();

      ManuscriptRef mRef = extract.getManuscriptRef();
      proxy.manuscript = ReferenceDTO.create(mRef.getId(), mRef.getDisplayTitle());

      proxy.author = extract.getAuthor();

      SourceRef srcRef = extract.getSource();
      proxy.source = ReferenceDTO.create(srcRef.getId(), srcRef.getDisplayTitle());
      proxy.sourceLineRef = srcRef.getLineReference();

      try
      {
         proxy.normalized = extractManipulationUtil.toNormalized(extract.getTEIContent());
         proxy.original = extractManipulationUtil.toOriginal(extract.getTEIContent());
      }
      catch (ExtractManipulationException e)
      {
         throw new SearchException("Unable to transform TEI for search proxy", e);
      }

      extract.getSpeakerRefs().parallelStream()
         .map(ref -> ReferenceDTO.create(ref.getId(), ref.getDisplayName()))
         .forEach(proxy.speakers::add);

      // TODO: set playwrights

      return proxy;
   }
}
