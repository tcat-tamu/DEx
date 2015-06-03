package edu.tamu.tcat.dex.psql.test;

import java.util.HashMap;
import java.util.Map;

import edu.tamu.tcat.osgi.config.ConfigurationProperties;

class BasicConfigurationProperties implements ConfigurationProperties
{
   private final Map<String, Object> properties;

   public BasicConfigurationProperties()
   {
      this.properties = new HashMap<>();
   }

   public BasicConfigurationProperties(Map<String, Object> properties)
   {
      this.properties = properties;
   }

   public void setProperty(String name, Object value)
   {
      properties.put(name, value);
   }

   @Override
   public <T> T getPropertyValue(String name, Class<T> type) throws IllegalStateException
   {
      if (!properties.containsKey(name))
      {
         throw new IllegalStateException("No property with display {" + name + "}");
      }

      Object property = properties.get(name);
      return type.cast(property);
   }

   @Override
   public <T> T getPropertyValue(String name, Class<T> type, T defaultValue) throws IllegalStateException
   {
      if (!properties.containsKey(name))
      {
         return defaultValue;
      }

      Object property = properties.get(name);
      return type.cast(property);
   }

}