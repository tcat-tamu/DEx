package edu.tamu.tcat.dex.importer;

public class XmlParseException extends Exception
{

   public XmlParseException()
   {
   }

   public XmlParseException(String message)
   {
      super(message);
   }

   public XmlParseException(Throwable cause)
   {
      super(cause);
   }

   public XmlParseException(String message, Throwable cause)
   {
      super(message, cause);
   }

   public XmlParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
   {
      super(message, cause, enableSuppression, writableStackTrace);
   }

}
