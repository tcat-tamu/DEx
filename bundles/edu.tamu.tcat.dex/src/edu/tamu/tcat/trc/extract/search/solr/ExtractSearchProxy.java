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
   public static class Anchor
   {
      public String id;
      public String display;

      public static Anchor create(String id, String display)
      {
         Anchor a = new Anchor();
         a.id = id;
         a.display = display;
         return a;
      }
   }

   public String id;
   public Anchor manuscript;
   public String sourceId;
   public String sourceLineRef;
   public String normalized;
   public String original;
   public final List<Anchor> playwrights = new ArrayList<>();
   public final List<Anchor> speakers = new ArrayList<>();

   public static ExtractSearchProxy create(DramaticExtract extract, ExtractManipulationUtil extractManipulationUtil) throws SearchException
   {
      ExtractSearchProxy proxy = new ExtractSearchProxy();

      proxy.id = extract.getId();

      ManuscriptRef mRef = extract.getManuscriptRef();
      proxy.manuscript = Anchor.create(mRef.getId(), mRef.getDisplayTitle());

      SourceRef srcRef = extract.getSource();
      proxy.sourceId = srcRef.getId();
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
         .map(ref -> Anchor.create(ref.getId(), ref.getDisplayName()))
         .forEach(proxy.speakers::add);

      // TODO: set playwrights

      return proxy;
   }
}
