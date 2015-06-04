package edu.tamu.tcat.trc.extract.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.bib.AuthorList;
import edu.tamu.tcat.trc.entries.types.bib.AuthorReference;
import edu.tamu.tcat.trc.entries.types.bib.Edition;
import edu.tamu.tcat.trc.entries.types.bib.PublicationInfo;
import edu.tamu.tcat.trc.entries.types.bib.Title;
import edu.tamu.tcat.trc.entries.types.bib.TitleDefinition;
import edu.tamu.tcat.trc.entries.types.bib.Work;
import edu.tamu.tcat.trc.entries.types.bib.dto.AuthorRefDV;
import edu.tamu.tcat.trc.entries.types.bib.dto.TitleDV;

public class ManuscriptDTO
{
   public String id;
   public String title;
   public String author;

   public static Work instantiate(ManuscriptDTO dto)
   {
      ManuscriptImpl manuscript = new ManuscriptImpl();
      manuscript.id = dto.id;
      manuscript.authors = singletonAuthorList(dto.author);
      manuscript.title = singletonTitleDef(dto.title);

      return manuscript;
   }

   public static ManuscriptDTO create(Work manuscript)
   {
      ManuscriptDTO dto = new ManuscriptDTO();
      dto.id = manuscript.getId();
      dto.title = manuscript.getTitle().getCanonicalTitle().getFullTitle();

      AuthorReference authorRef = manuscript.getAuthors().get(0);
      String firstName = authorRef.getFirstName().trim();
      String lastName = authorRef.getLastName().trim();

      dto.author = (firstName.isEmpty() ? "" : firstName + " ") + lastName;

      return dto;
   }


   private static AuthorList singletonAuthorList(String authorName)
   {
      AuthorRefDV refDTO = new AuthorRefDV();
      refDTO.lastName = authorName;
      AuthorReference authorRef = AuthorRefDV.instantiate(refDTO);
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
      TitleDV titleDTO = new TitleDV();
      titleDTO.title = titleStr;
      titleDTO.type = "Canonical";
      Title title = TitleDV.instantiate(titleDTO);
      Set<Title> titleSet = new HashSet<>();

      return new TitleDefinition()
      {
         @Override
         public Title getTitle(Locale language)
         {
            return title;
         }

         @Override
         public Title getShortTitle()
         {
            return title;
         }

         @Override
         public Title getCanonicalTitle()
         {
            return title;
         }

         @Override
         public Set<Title> getAlternateTitles()
         {
            return titleSet;
         }
      };
   }


   public static class ManuscriptImpl implements Work
   {
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
         return new AuthorList()
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
         };
      }

      @Override
      public PublicationInfo getPublicationInfo()
      {
         return null;
      }

      @Override
      public Collection<Edition> getEditions()
      {
         return Collections.emptyList();
      }

      @Override
      public Edition getEdition(String editionId) throws NoSuchCatalogRecordException
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

   }
}
