package edu.tamu.tcat.dex.rest;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import edu.tamu.tcat.dex.importer.DexImportException;
import edu.tamu.tcat.dex.importer.DexImportService;

@Path("/upload")
public class UploadResource
{
   private static final Logger logger = Logger.getLogger(UploadResource.class.getName());

   private static final String MANUSCRIPT_UPLOAD_FORM = "<!doctype html><html><head><meta charset=\"UTF-8\"><title>Upload Form</title></head>"
         + "<body><form method=\"post\" enctype=\"multipart/form-data\">"
         + "<label for=\"msid\">Manuscript ID</label><input type=\"text\" name=\"id\" id=\"msid\">"
         + "<input type=\"file\" name=\"file\">"
         + "<button type=\"submit\">Submit</button>"
         + "</form></body></html>";

   private static final String PEOPLE_AND_PLAYS_UPLOAD_FORM = "<!doctype html><html><head><meta charset=\"UTF-8\"><title>Upload Form</title></head>"
         + "<body><form method=\"post\" enctype=\"multipart/form-data\">"
         + "<input type=\"file\" name=\"file\">"
         + "<button type=\"submit\">Submit</button>"
         + "</form></body></html>";

   private DexImportService importService;

   public void setImportService(DexImportService importService)
   {
      this.importService = importService;
   }

   public void activate()
   {
      try 
      {
         Objects.requireNonNull(importService, "No import service supplied.");
      }
      catch (Exception ex)
      {
         logger.log(Level.SEVERE, "Failed to start /upload REST resource.", ex);
         throw ex;
      }
   }

   public void dispose()
   {
      importService = null;
   }


   @GET
   @Path("/manuscript")
   @Produces(MediaType.TEXT_HTML)
   public String getManuscriptUploadForm()
   {
      return MANUSCRIPT_UPLOAD_FORM;
   }

   @POST
   @Path("/manuscript")
   @Consumes(MediaType.MULTIPART_FORM_DATA)
   @Produces(MediaType.TEXT_PLAIN)
   public String uploadManuscript(@FormDataParam("id") String id, 
                                  @FormDataParam("file") InputStream fileStream, 
                                  @FormDataParam("file") FormDataContentDisposition fileInfo)
   {
      logger.log(Level.INFO, "Uploading manuscript " + id);
      try
      {
         // TODO: send (Level.WARNING) log messages about import to user
         importService.importManuscriptTEI(id, fileStream);
         return "success!";
      }
      catch (Exception e)
      {
         logger.log(Level.WARNING, "Unable to import manuscript", e);
         throw new BadRequestException("Unable to import manuscript", e);
      }
   }

   @GET
   @Path("/peopleandplays")
   @Produces(MediaType.TEXT_HTML)
   public String getPeopleAndPlaysUploadForm()
   {
      return PEOPLE_AND_PLAYS_UPLOAD_FORM;
   }

   @POST
   @Path("/peopleandplays")
   @Consumes(MediaType.MULTIPART_FORM_DATA)
   @Produces(MediaType.TEXT_PLAIN)
   public String uploadPeopleAndPlays(@FormDataParam("file") InputStream fileStream, @FormDataParam("file") FormDataContentDisposition fileInfo)
   {
      try
      {
         
         logger.log(Level.INFO, "Uploading people and plays ");
         
         // TODO: send (Level.WARNING) log messages about import to user
         // TODO: convert API to use InputStream?
         importService.importPeopleAndPlaysTEI(new InputStreamReader(fileStream));
         return "success!";
      }
      catch (DexImportException e)
      {
         logger.log(Level.WARNING, "Unable to import people and plays", e);
         throw new BadRequestException("Unable to import people and plays", e);
      }
   }
}
