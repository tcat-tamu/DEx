package edu.tamu.tcat.dex.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;


@Provider
@Path("/")
public class HelloWorldService
{
   @GET
   @Produces(MediaType.TEXT_PLAIN)
   public String hello()
   {
      return "hello, world";
   }
}
