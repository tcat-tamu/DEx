package edu.tamu.tcat.dex.trc.extract;

public class ExtractNotAvailableException extends Exception
{
   // This seems like it should likely be part of trc.entries.core 
   public ExtractNotAvailableException()
   {
   }

   public ExtractNotAvailableException(String message)
   {
      super(message);
   }

   public ExtractNotAvailableException(Throwable cause)
   {
      super(cause);
   }

   public ExtractNotAvailableException(String message, Throwable cause)
   {
      super(message, cause);
   }

   public ExtractNotAvailableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
   {
      super(message, cause, enableSuppression, writableStackTrace);
   }

}
