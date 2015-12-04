package edu.tamu.tcat.dex.trc.extract;

public interface PlaywrightRef
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
