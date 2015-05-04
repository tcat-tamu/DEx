package edu.tamu.tcat.dex.trc.entry;


public interface ExtractsChangeEvent
{
   enum ChangeType
   {
      CREATED,
      MODIFIED,
      DELETED;
   }

   /**
    * @return The type of change that occurred.
    */
   ChangeType getChangeType();

   /**
    * @return The persistent identifier for the extract that was changed.
    */
   String getExtractId();

   /**
    * @return the extract that was changed
    * @throws ExtractNotAvailableException If the extract cannot be retrieved (e.g. if it was deleted)
    */
   DramaticExtract getExtractEvt() throws ExtractNotAvailableException;
}
