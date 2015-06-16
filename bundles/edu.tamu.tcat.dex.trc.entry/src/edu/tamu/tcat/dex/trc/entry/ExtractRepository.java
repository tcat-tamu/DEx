package edu.tamu.tcat.dex.trc.entry;

import edu.tamu.tcat.trc.entries.notification.UpdateEvent;
import edu.tamu.tcat.trc.entries.notification.UpdateListener;

public interface ExtractRepository
{
   DramaticExtract get(String id) throws ExtractNotAvailableException, DramaticExtractException;

   EditExtractCommand create(String id) throws DramaticExtractException;

   EditExtractCommand edit(String id) throws ExtractNotAvailableException, DramaticExtractException;

   void remove(String id) throws DramaticExtractException;

   /**
    * Add listener to be notified whenever an extract is modified (created, updated or deleted).
    *
    * @param ears The listener to be added.
    * @return A registration handle that allows the listener to be removed.
    */
   AutoCloseable register(UpdateListener<UpdateEvent> ears);
}
