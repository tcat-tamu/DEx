package edu.tamu.tcat.dex.psql.test;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import edu.tamu.tcat.osgi.config.ConfigurationProperties;

class BasicConfigurationProperties implements ConfigurationProperties
{
   private final Map<String, String> properties;

   public BasicConfigurationProperties()
   {
      this.properties = new HashMap<>();
   }

   public BasicConfigurationProperties(Map<String, String> properties)
   {
      this.properties = properties;
   }

   public void setProperty(String name, String value)
   {
      properties.put(name, value);
   }

   @Override
   public <T> T getPropertyValue(String name, Class<T> type) throws IllegalStateException
   {
      return getPropertyValue(name, type, null);
   }

   @Override
   @SuppressWarnings("unchecked")
   public <T> T getPropertyValue(String name, Class<T> type, T defaultValue) throws IllegalStateException
   {
      Objects.requireNonNull(name, "property name is null");
      Objects.requireNonNull(type, "property type is null");

      String str = properties.get(name);

      if (str == null)
         return defaultValue;
      if (type.isInstance(str))
         return (T)str;

      try
      {
         if (Number.class.isAssignableFrom(type))
         {
            if (Byte.class.isAssignableFrom(type))
               return (T)Byte.valueOf(str);
            if (Short.class.isAssignableFrom(type))
               return (T)Short.valueOf(str);
            if (Integer.class.isAssignableFrom(type))
               return (T)Integer.valueOf(str);
            if (Long.class.isAssignableFrom(type))
               return (T)Long.valueOf(str);
            if (Float.class.isAssignableFrom(type))
               return (T)Float.valueOf(str);
            if (Double.class.isAssignableFrom(type))
               return (T)Double.valueOf(str);

            throw new IllegalStateException("Unhandled numeric type ["+type+"] for property ["+name+"] value ["+str+"]");
         }

         if (Boolean.class.isAssignableFrom(type))
            return (T)Boolean.valueOf(str);
      }
      catch (NumberFormatException e)
      {
         throw new IllegalStateException("Failed converting property ["+name+"] value ["+str+"] to primitive "+type, e);
      }

      try
      {
         if (Path.class.isAssignableFrom(type))
         {
            return (T)Paths.get(str);
         }
      }
      catch (Exception e)
      {
         throw new IllegalStateException("Failed converting property ["+name+"] value ["+str+"] to OS file system path "+type, e);
      }

      try
      {
         if (URI.class.isAssignableFrom(type))
         {
            return (T)new URI(str);
         }
      }
      catch (Exception e)
      {
         throw new IllegalStateException("Failed converting property ["+name+"] value ["+str+"] to URI "+type, e);
      }

      throw new IllegalStateException("Unhandled type: " + type.getCanonicalName());
   }

}