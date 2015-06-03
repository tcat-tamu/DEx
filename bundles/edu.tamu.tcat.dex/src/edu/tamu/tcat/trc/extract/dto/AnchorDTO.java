package edu.tamu.tcat.trc.extract.dto;

public class AnchorDTO
{
   public String id;
   public String title;

   public static AnchorDTO create(String id, String title)
   {
      AnchorDTO dto = new AnchorDTO();
      dto.id = id;
      dto.title = title;

      return dto;
   }
}
