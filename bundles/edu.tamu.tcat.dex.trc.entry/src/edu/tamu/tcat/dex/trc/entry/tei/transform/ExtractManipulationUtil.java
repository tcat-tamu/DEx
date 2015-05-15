package edu.tamu.tcat.dex.trc.entry.tei.transform;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Objects;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import edu.tamu.tcat.dex.trc.entry.DramaticExtract;
import edu.tamu.tcat.osgi.config.ConfigurationProperties;

/**
 *  Utilities for manipulating the content of a {@link DramaticExtract}.
 *
 */
public class ExtractManipulationUtil
{
   private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();

   private static final String CONFIG_TRANSFORMER_ORIGINAL = "dex.xslt.tei.original";
   private static final String CONFIG_TRANSFORMER_NORMALIZED = "dex.xslt.tei.normalized";

   private Transformer originalTransformer;
   private Transformer normalizedTransformer;

   private ConfigurationProperties config;

   public void setConfiguration(ConfigurationProperties config)
   {
      this.config = config;
   }

   public void activate()
   {
      Objects.requireNonNull(config, "No configuration provided");

      String originalXsltPath = config.getPropertyValue(CONFIG_TRANSFORMER_ORIGINAL, String.class);
      String normalizedXsltPath = config.getPropertyValue(CONFIG_TRANSFORMER_NORMALIZED, String.class);

      File originalXslt = new File(originalXsltPath);
      File normalizedXslt = new File(normalizedXsltPath);

      StreamSource originalSource = new StreamSource(originalXslt);
      StreamSource normalizedSource = new StreamSource(normalizedXslt);

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

   public void dispose()
   {
   }

   public String toHtml(Document teiContent)
   {
      throw new UnsupportedOperationException();
   }

   public String toOriginal(Document teiContent) throws ExtractManipulationException
   {
      return applyTransformer(originalTransformer, teiContent);
   }

   public String toNormalized(Document teiContent) throws ExtractManipulationException
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
