package edu.tamu.tcat.dex.rest.v1;

import java.util.Objects;

import edu.tamu.tcat.trc.entries.types.bib.AuthorList;
import edu.tamu.tcat.trc.entries.types.bib.AuthorReference;
import edu.tamu.tcat.trc.entries.types.bib.Title;
import edu.tamu.tcat.trc.entries.types.bib.TitleDefinition;
import edu.tamu.tcat.trc.entries.types.bib.Work;

public class RepoAdapter
{
   public static RestApiV1.Manuscript toManuscriptDTO(Work work)
   {
      Objects.requireNonNull(work);

      RestApiV1.Manuscript dto = new RestApiV1.Manuscript();

      dto.id = work.getId();
      dto.title = getTitle(work);

      AuthorList authorList = work.getAuthors();
      AuthorReference authorReference = authorList == null || authorList.size() == 0 ? null : authorList.get(0);
      dto.author = getName(authorReference);

      return dto;
   }

   private static String getTitle(Work work)
   {
      if (work == null)
      {
         return "";
      }

      TitleDefinition titleDefinition = work.getTitle();
      Title canonicalTitle = titleDefinition == null ? null : titleDefinition.getCanonicalTitle();
      String title = canonicalTitle == null ? null : canonicalTitle.getFullTitle();
      return title == null ? "" : title;
   }

   private static String getName(AuthorReference ref)
   {
      if (ref == null)
      {
         return "";
      }

      String firstName = ref.getFirstName();
      String firstNameTrimmed = firstName == null ? "" : firstName.trim();

      String lastName = ref.getLastName();
      String lastNameTrimmed = lastName == null ? "" : lastName.trim();

      return (firstNameTrimmed + " " + lastNameTrimmed).trim();
   }
}
