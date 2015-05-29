package edu.tamu.tcat.dex.importer;

public class DexImportException extends Exception
{

   public DexImportException()
   {
   }

   public DexImportException(String message)
   {
      super(message);
   }

   public DexImportException(Throwable cause)
   {
      super(cause);
   }

   public DexImportException(String message, Throwable cause)
   {
      super(message, cause);
   }

   public DexImportException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
   {
      super(message, cause, enableSuppression, writableStackTrace);
   }

}
