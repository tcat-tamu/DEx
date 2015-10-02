package edu.tamu.tcat.dex.psql.test;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class SimpleXMLTest
{

   public static void xsl(String inFilename, String outFilename, String xslFilename) {
      try {
         // Create transformer factory
         TransformerFactory factory = TransformerFactory.newInstance();

         // Use the factory to create a template containing the xsl file
         Templates template = factory.newTemplates(new StreamSource(
               new FileInputStream(xslFilename)));

         // Use the template to create a transformer
         Transformer xformer = template.newTransformer();

         // Prepare the input and output files
         Source source = new StreamSource(new FileInputStream(inFilename));
         Result result = new StreamResult(new FileOutputStream(outFilename));

         // Apply the xsl file to the source file and write the result to the output file
         xformer.transform(source, result);
      } catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   public static void main(String[] args)
   {
      xsl("C:\\Users\\neal.audenaert\\Documents\\manuscripts.xml",
          "D:\\Projects\\DramaticExtracts\\data\\html\\manuscripts.html",
          "D:\\dev\\git\\dex.deploy\\xslt\\tei-original-html.xsl");
   }
}
