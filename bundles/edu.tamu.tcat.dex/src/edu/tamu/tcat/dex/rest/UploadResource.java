package edu.tamu.tcat.dex.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("/upload")
public class UploadResource
{
   public void activate()
   {
   }

   public void dispose()
   {
   }

   @POST
   @Path("/manuscript")
   @Consumes(MediaType.MULTIPART_FORM_DATA)
   @Produces(MediaType.TEXT_PLAIN)
   public String uploadManuscript(@FormDataParam("file") InputStream fileStream, @FormDataParam("file") FormDataContentDisposition fileInfo)
   {
      StringBuilder output = new StringBuilder();

      output.append("fileName: ")
            .append(fileInfo.getFileName())
            .append("\n\n");

      Reader reader = new InputStreamReader(fileStream);

      try
      {
         int c;
         while ((c = reader.read()) != -1)
         {
            output.append((char) c);
         }
      }
      catch (IOException e) {
         throw new IllegalStateException("unable to read file upload", e);
      }

      return output.toString();
   }

   @GET
   @Path("/manuscript")
   @Produces(MediaType.TEXT_HTML)
   public String getManuscriptUploadForm()
   {
      return "<!doctype html><html><head><meta charset=\"UTF-8\"><title>Upload Form</title></head>"
            + "<body><form method=\"post\" enctype=\"multipart/form-data\"><input type=\"file\" name=\"file\"><button type=\"submit\">Submit</button></form></body></html>";
   }
}
