package edu.tamu.tcat.dex.rest.v1;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.tamu.tcat.trc.entries.types.biblio.AuthorList;
import edu.tamu.tcat.trc.entries.types.biblio.AuthorReference;
import edu.tamu.tcat.trc.entries.types.biblio.Title;
import edu.tamu.tcat.trc.entries.types.biblio.TitleDefinition;
import edu.tamu.tcat.trc.entries.types.biblio.Work;
import edu.tamu.tcat.trc.entries.types.bio.Person;
import edu.tamu.tcat.trc.entries.types.bio.PersonName;

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

   public static RestApiV1.Play toPlayDTO(Work work)
   {
      Objects.requireNonNull(work);

      RestApiV1.Play dto = new RestApiV1.Play();

      dto.id = work.getId();
      dto.title = getTitle(work);

      for (AuthorReference ref : work.getAuthors())
      {
         if (ref == null)
         {
            continue;
         }

         dto.playwrights.add(toDTO(ref));
      }

      return dto;
   }

   public static RestApiV1.Playwright toPlaywrightDTO(Person person)
   {
      Objects.requireNonNull(person);

      RestApiV1.Playwright dto = new RestApiV1.Playwright();

      dto.id = person.getId();

      dto.names = Stream.concat(Stream.of(person.getCanonicalName()), person.getAlternativeNames().stream())
            .map(RepoAdapter::getName)
            .collect(Collectors.toList());

      return dto;
   }

   public static RestApiV1.Character toCharacterDTO(Person person)
   {
      RestApiV1.Character dto = new RestApiV1.Character();

      dto.id = person.getId();
      dto.name = getName(person.getCanonicalName());

      // TODO: dto.plays

      return dto;
   }

   private static RestApiV1.PlaywrightReference toDTO(AuthorReference ref)
   {
      RestApiV1.PlaywrightReference dto = new RestApiV1.PlaywrightReference();

      dto.id = ref.getId();
      dto.name = getName(ref);

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

   private static String getName(PersonName name)
   {
      if (name == null)
      {
         return "";
      }

      // prefer display name
      String displayName = name.getDisplayName();
      if (displayName != null && !displayName.isEmpty())
      {
         return displayName;
      }

      // fall back to construction from name parts
      List<String> nameParts = Arrays.asList(
            name.getTitle(),
            name.getGivenName(),
            name.getMiddleName(),
            name.getFamilyName(),
            name.getSuffix()
      );

      StringJoiner nameJoiner = new StringJoiner(" ");
      nameJoiner.setEmptyValue("");
      nameParts.stream()
            .filter(Objects::nonNull)
            .forEach(nameJoiner::add);

      return nameJoiner.toString().replaceAll("\\w+", " ");
   }
}
