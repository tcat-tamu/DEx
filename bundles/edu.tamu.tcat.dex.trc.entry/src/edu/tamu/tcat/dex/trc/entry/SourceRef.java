package edu.tamu.tcat.dex.trc.entry;


public interface SourceRef
{
   /**
    * @return The ID of the referenced bibliographic entry
    */
   String getId();

   /**
    * @return A string representing the exact location within the source bibliographic entry where
    *       the corresponding extract may be found.
    */
   String getLineReference();
}
