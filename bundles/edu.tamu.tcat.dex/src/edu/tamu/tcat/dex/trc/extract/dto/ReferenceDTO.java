package edu.tamu.tcat.dex.trc.extract.dto;

public class ReferenceDTO
{
   public String id;
   public String title;

   public static ReferenceDTO create(String id, String title)
   {
      ReferenceDTO dto = new ReferenceDTO();
      dto.id = id;
      dto.title = title;

      return dto;
   }
}
