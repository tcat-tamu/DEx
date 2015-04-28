package edu.tamu.tcat.dex.trc.entry;

import java.net.URI;

import edu.tamu.tcat.trc.entries.notification.UpdateListener;

public interface ExtractRepository
{
   DramaticExtract get(URI id) throws ExtractNotAvailableException, DramaticExtractException;
   
   EditExtractCommand create() throws DramaticExtractException;
   
   EditExtractCommand edit(URI id) throws ExtractNotAvailableException;
   
   void remove(URI id) throws DramaticExtractException;
   
   /**
    * Add listener to be notified whenever an extract is modified (created, updated or deleted).
    *
    * @param ears The listener to be added.
    * @return A registration handle that allows the listener to be removed.
    */
   AutoCloseable register(UpdateListener<DramaticExtract> ears);
}
