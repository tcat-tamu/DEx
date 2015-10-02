package edu.tamu.tcat.dex;

import java.util.UUID;

import edu.tamu.tcat.trc.repo.IdFactory;


public class UUIDIdFactory implements IdFactory
{

   public void activate()
   {
   }

   public void dispose()
   {
   }

   @Override
   public String getNextId(String context)
   {
      return UUID.randomUUID().toString();
   }

}
