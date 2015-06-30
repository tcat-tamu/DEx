package edu.tamu.tcat.dex.rest.v1;

import java.util.Objects;

import edu.tamu.tcat.trc.entries.types.bib.AuthorList;
import edu.tamu.tcat.trc.entries.types.bib.AuthorReference;
import edu.tamu.tcat.trc.entries.types.bib.Title;
import edu.tamu.tcat.trc.entries.types.bib.TitleDefinition;
import edu.tamu.tcat.trc.entries.types.bib.Work;

public class RepoAdapter
{
   public static RestApiV1.Manuscript toDTO(Work work)
   {
      Objects.requireNonNull(work);

      RestApiV1.Manuscript dto = new RestApiV1.Manuscript();

      dto.id = work.getId();

      TitleDefinition titleDefinition = work.getTitle();
      Title canonicalTitle = titleDefinition == null ? null : titleDefinition.getCanonicalTitle();
      String title = canonicalTitle == null ? null : canonicalTitle.getFullTitle();
      dto.title = title == null ? "" : title;

      AuthorList authorList = work.getAuthors();
      AuthorReference authorReference = authorList == null || authorList.size() == 0 ? null : authorList.get(0);
      String firstName = authorReference == null ? null : authorReference.getFirstName();
      String firstNameTrimmed = firstName == null ? "" : firstName.trim();
      String lastName = authorReference == null ? null : authorReference.getLastName();
      String lastNameTrimmed = lastName == null ? "" : lastName.trim();
      dto.author = (firstNameTrimmed + " " + lastNameTrimmed).trim();

      return dto;
   }
}
