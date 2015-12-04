package edu.tamu.tcat.dex.trc.extract;

public interface ManuscriptRef
{
   /**
    * @return The ID of the referenced bibliographic entry.
    */
   String getId();

   /**
    * @return The display title of the referenced bibliographic entry;
    */
   String getDisplayTitle();
}