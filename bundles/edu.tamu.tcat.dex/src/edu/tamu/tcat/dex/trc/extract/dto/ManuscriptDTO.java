package edu.tamu.tcat.dex.trc.extract.dto;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import edu.tamu.tcat.dex.TrcBiblioType;
import edu.tamu.tcat.trc.entries.types.biblio.AuthorList;
import edu.tamu.tcat.trc.entries.types.biblio.AuthorReference;
import edu.tamu.tcat.trc.entries.types.biblio.CopyReference;
import edu.tamu.tcat.trc.entries.types.biblio.Edition;
import edu.tamu.tcat.trc.entries.types.biblio.Title;
import edu.tamu.tcat.trc.entries.types.biblio.TitleDefinition;
import edu.tamu.tcat.trc.entries.types.biblio.Work;
import edu.tamu.tcat.trc.entries.types.biblio.dto.AuthorReferenceDTO;

public class ManuscriptDTO
{
   public String id;
   public String title;
   public String author;
   public String links;

   public static Work instantiate(ManuscriptDTO dto)
   {
      ManuscriptImpl manuscript = new ManuscriptImpl();
      manuscript.id = dto.id;
      manuscript.authors = singletonAuthorList(dto.author);
      manuscript.title = singletonTitleDef(dto.title);
      manuscript.summary = dto.links;

      return manuscript;
   }

   public static ManuscriptDTO create(Work manuscript)
   {
      ManuscriptDTO dto = new ManuscriptDTO();
      dto.id = manuscript.getId();
      dto.title = manuscript.getTitle().get("canonical").getFullTitle();
      dto.links = manuscript.getSummary();

      AuthorReference authorRef = manuscript.getAuthors().get(0);
      String firstName = authorRef.getFirstName();
      String lastName = authorRef.getLastName();

      firstName = (firstName == null || firstName.isEmpty()) ? "" : firstName.trim();
      lastName = (lastName == null || lastName.isEmpty()) ? "" : lastName.trim();

      dto.author = (firstName + " " + lastName).trim();

      return dto;
   }


   private static AuthorList singletonAuthorList(String authorName)
   {
      AuthorReferenceDTO refDTO = new AuthorReferenceDTO();
      refDTO.lastName = authorName;
      AuthorReference authorRef = new AuthorRefImpl(refDTO);
      Set<AuthorReference> authorRefSet = Collections.singleton(authorRef);

      return new AuthorList()
      {
         @Override
         public Iterator<AuthorReference> iterator()
         {
            return authorRefSet.iterator();
         }

         @Override
         public int size()
         {
            return 1;
         }

         @Override
         public AuthorReference get(int ix) throws IndexOutOfBoundsException
         {
            if (ix != 0) {
               throw new IndexOutOfBoundsException("The only acceptable index is [0]");
            }

            return authorRef;
         }
      };
   }

   private static TitleDefinition singletonTitleDef(String titleStr)
   {
      return new TitleDefinitionImpl(new TitleImpl(titleStr));
   }

   private static final String TITLE_TYPE = "canonical";

   private static final class TitleImpl implements Title
   {
      private final String title;

      public TitleImpl(String title)
      {
         this.title = title;
      }

      @Override
      public String getType()
      {
         return TITLE_TYPE;
      }

      @Override
      public String getTitle()
      {
         return title;
      }

      @Override
      public String getSubTitle()
      {
         return "";
      }

      @Override
      public String getFullTitle()
      {
         return title;
      }

      @Override
      public String getLanguage()
      {
         return "en";
      }
   }

   private static class AuthorRefImpl implements AuthorReference
   {

      private final String id;
      private final String first;
      private final String last;
      private final String role;

      public AuthorRefImpl(AuthorReferenceDTO refDTO)
      {
         this.id = refDTO.authorId;
         this.first = refDTO.firstName;
         this.last = refDTO.lastName;
         this.role = refDTO.role;

      }
      @Override
      public String getId()
      {
         return id;
      }

      @Override
      public String getFirstName()
      {
         return first;
      }

      @Override
      public String getLastName()
      {
         return last;
      }

      @Override
      public String getRole()
      {
         return role;
      }

   }
   private static final class TitleDefinitionImpl implements TitleDefinition
   {
      private final Title title;

      public TitleDefinitionImpl(Title title)
      {
         this.title = title;
      }

      @Override
      public Set<Title> get()
      {
         return new HashSet<>(Arrays.asList(title));
      }

      @Override
      public Title get(String type)
      {
         if (!Objects.equals(TITLE_TYPE, type.toLowerCase()))
            throw new IllegalArgumentException(
                  MessageFormat.format("No title defined for type {0}", type));

         return title;
      }

      @Override
      public Set<String> getTypes()
      {
         return new HashSet<>(Arrays.asList(TITLE_TYPE));
      }
   }

   public static class ManuscriptImpl implements Work
   {
      private final class AuthorListImpl implements AuthorList
      {
         @Override
         public Iterator<AuthorReference> iterator()
         {
            return Collections.<AuthorReference>emptyList().iterator();
         }

         @Override
         public int size()
         {
            return 0;
         }

         @Override
         public AuthorReference get(int ix) throws IndexOutOfBoundsException
         {
            throw new IndexOutOfBoundsException("tried to get element of empty Author List");
         }
      }

      private String id;
      private TitleDefinition title;
      private AuthorList authors;
      private String summary = "";

      @Override
      public String getId()
      {
         return id;
      }

      @Override
      public String getType()
      {
         return TrcBiblioType.Manuscript.toString();
      }

      @Override
      public AuthorList getAuthors()
      {
         return authors;
      }

      @Override
      public TitleDefinition getTitle()
      {
         return title;
      }

      @Override
      public AuthorList getOtherAuthors()
      {
         return new AuthorListImpl();
      }

      @Override
      public List<Edition> getEditions()
      {
         return Collections.emptyList();
      }

      @Override
      public Edition getEdition(String editionId)
      {
         return null;
      }

      @Override
      public String getSeries()
      {
         return null;
      }

      @Override
      public String getSummary()
      {
         return summary;
      }

      @Override
      public CopyReference getDefaultCopyReference()
      {
         return null;
      }

      @Override
      public Set<CopyReference> getCopyReferences()
      {
         return Collections.emptySet();
      }
   }
}
