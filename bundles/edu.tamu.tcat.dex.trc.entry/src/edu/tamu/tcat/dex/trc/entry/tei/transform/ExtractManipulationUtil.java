package edu.tamu.tcat.dex.trc.entry.tei.transform;

import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import edu.tamu.tcat.dex.trc.entry.DramaticExtract;

/**
 *  Utilities for manipulating the content of a {@link DramaticExtract}.
 *
 */
public class ExtractManipulationUtil
{
   private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();

   private static final Transformer originalTransformer;
   private static final Transformer normalizedTransformer;

   static {
      // TODO: get XSLT source as a bundle resource
      StreamSource originalSource = new StreamSource();
      StreamSource normalizedSource = new StreamSource();

      try
      {
         originalTransformer = transformerFactory.newTransformer(originalSource);
         normalizedTransformer = transformerFactory.newTransformer(normalizedSource);
      }
      catch (Exception e)
      {
         throw new IllegalStateException("Unable to initialize XSLT transformers", e);
      }
   }

   public static String toHtml(Document teiContent)
   {
      throw new UnsupportedOperationException();
   }

   public static String toOriginal(Document teiContent) throws ExtractManipulationException
   {
      return applyTransformer(originalTransformer, teiContent);
   }

   public static String toNormalized(Document teiContent) throws ExtractManipulationException
   {
      return applyTransformer(normalizedTransformer, teiContent);
   }

   private static String applyTransformer(Transformer transformer, Document document) throws ExtractManipulationException
   {
      DOMSource teiSource = new DOMSource(document);
      Writer resultWriter = new StringWriter();
      StreamResult result = new StreamResult(resultWriter);

      try
      {
         transformer.transform(teiSource, result);
      }
      catch (TransformerException e)
      {
         throw new ExtractManipulationException("Unable to apply XSLT transformation to TEI source document", e);
      }

      return resultWriter.toString();
   }
}
