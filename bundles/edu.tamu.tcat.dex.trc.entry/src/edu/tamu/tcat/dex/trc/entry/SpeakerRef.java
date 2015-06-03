package edu.tamu.tcat.dex.trc.entry;

public interface SpeakerRef
{
   /**
    * @return the ID of the referenced biographical entry.
    */
   String getId();

   /**
    * @return The display name of the referenced biographical entry.
    */
   String getDisplayName();
}
