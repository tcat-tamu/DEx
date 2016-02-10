package edu.tamu.tcat.dex.rest.v1;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.tamu.tcat.trc.entries.common.DateDescription;
import edu.tamu.tcat.trc.entries.types.biblio.AuthorList;
import edu.tamu.tcat.trc.entries.types.biblio.AuthorReference;
import edu.tamu.tcat.trc.entries.types.biblio.Edition;
import edu.tamu.tcat.trc.entries.types.biblio.PublicationInfo;
import edu.tamu.tcat.trc.entries.types.biblio.Title;
import edu.tamu.tcat.trc.entries.types.biblio.TitleDefinition;
import edu.tamu.tcat.trc.entries.types.biblio.Work;
import edu.tamu.tcat.trc.entries.types.bio.Person;
import edu.tamu.tcat.trc.entries.types.bio.PersonName;

public class RepoAdapter
{
   private static final Logger logger = Logger.getLogger(RepoAdapter.class.getName());

   public static RestApiV1.Manuscript toManuscriptDTO(Work work)
   {
      Objects.requireNonNull(work);

      RestApiV1.Manuscript dto = new RestApiV1.Manuscript();

      dto.id = work.getId();
      dto.title = getTitle(work.getTitle());
      dto.links = work.getSummary();

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
      dto.title = getTitle(work.getTitle());

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

   public static RestApiV1.PlayBibEntry toPlayBibEntryDTO(Work work)
   {
      RestApiV1.PlayBibEntry dto = new RestApiV1.PlayBibEntry();

      dto.id = work.getId();
      dto.title = getTitle(work.getTitle());
      dto.playwrights = getAuthors(work.getAuthors());
      dto.playwrights.addAll(getAuthors(work.getOtherAuthors()));

      dto.editions = work.getEditions().stream()
            .map(RepoAdapter::toEditionBibEntryDTO)
            .collect(Collectors.toList());

      return dto;
   }

   public static RestApiV1.EditionBibEntry toEditionBibEntryDTO(Edition edition)
   {
      RestApiV1.EditionBibEntry dto = new RestApiV1.EditionBibEntry();

      dto.id = edition.getId();
      dto.editors = getAuthors(edition.getAuthors());
      dto.editors.addAll(getAuthors(edition.getOtherAuthors()));

      Collection<Title> titles = edition.getTitles();
      if (titles != null && !titles.isEmpty())
      {
         dto.title = getTitle(titles.iterator().next());
      }

      PublicationInfo publicationInfo = edition.getPublicationInfo();
      if (publicationInfo != null)
      {
         DateDescription publicationDate = publicationInfo.getPublicationDate();
         if (publicationDate != null)
         {
            dto.date = publicationDate.getDescription();
         }
      }

      String summary = edition.getSummary();
      if (summary != null)
      {
         try {
            dto.link = new URI(summary);
         }
         catch (URISyntaxException e) {
            logger.log(Level.WARNING, "Unable to parse URI on edition [" + edition.getId() + "].", e);
         }
      }

      return dto;
   }

   private static String getTitle(TitleDefinition titleDefinition)
   {
      if (titleDefinition == null)
      {
         return "";
      }

      return getTitle(titleDefinition.getCanonicalTitle());
   }

   private static String getTitle(Title title)
   {
      if (title == null)
      {
         return "";
      }

      String titleString = title.getFullTitle();
      return titleString == null ? "" : titleString;
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

   private static List<String> getAuthors(List<AuthorReference> authorList)
   {
      return authorList.stream()
            .map(RepoAdapter::getName)
            .collect(Collectors.toList());
   }

   private static List<String> getAuthors(AuthorList authorList)
   {
      List<String> authors = new ArrayList<>(authorList.size());
      authorList.forEach(ar -> authors.add(getName(ar)));
      return authors;
   }
}
