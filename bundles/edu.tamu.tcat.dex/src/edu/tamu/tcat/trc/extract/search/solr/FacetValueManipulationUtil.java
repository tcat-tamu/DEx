package edu.tamu.tcat.trc.extract.search.solr;

import java.util.Objects;

import edu.tamu.tcat.trc.entries.types.bib.Work;
import edu.tamu.tcat.trc.entries.types.bib.repo.WorkRepository;
import edu.tamu.tcat.trc.entries.types.bio.Person;
import edu.tamu.tcat.trc.entries.types.bio.repo.PeopleRepository;

public class FacetValueManipulationUtil
{
   /**
    * Delimiter between ID and display value for faceting
    */
   private static final String FACET_ID_LABEL_DELIMITER = "::";

   private PeopleRepository peopleRepo;
   private WorkRepository workRepo;

   public void setRepo(PeopleRepository repo)
   {
      peopleRepo = repo;
   }

   public void setRepo(WorkRepository repo)
   {
      workRepo = repo;
   }

   public void activate()
   {
      Objects.requireNonNull(peopleRepo, "No people repository supplied.");
      Objects.requireNonNull(workRepo, "No work repository supplied.");
   }

   public void dispose()
   {
      peopleRepo = null;
      workRepo = null;
   }


   /**
    * Transforms a work ID into a value for faceting.
    *
    * @param id
    * @return
    * @throws IllegalStateException when unable to get an associated title for the work ID.
    */
   public String getWorkFacetValue(String id)
   {
      try
      {
         Work work = workRepo.getWork(id);
         String title = work.getTitle().getCanonicalTitle().getFullTitle();
         return id + FACET_ID_LABEL_DELIMITER + title;
      }
      catch (Exception e)
      {
         throw new IllegalStateException("unable to facet by manuscript ID [" + id + "]", e);
      }
   }

   /**
    * Transforms a person ID into a value for faceting.
    *
    * @param id
    * @return
    * @throws IllegalStateException when unable to get an associated name for the person ID.
    */
   public String getPersonFacetValue(String id)
   {
      try
      {
         Person person = peopleRepo.get(id);
         String name = person.getCanonicalName().getDisplayName();
         return id + FACET_ID_LABEL_DELIMITER + name;
      }
      catch (Exception e)
      {
         throw new IllegalStateException("unable to facet by person ID [" + id + "]", e);
      }
   }

   /**
    * Splits a facet value into an id/label pair.
    *
    * @param facetValue
    * @return
    * @throws IllegalArgumentException on malformed facet value
    */
   public static FacetValue splitFacetValue(String facetValue)
   {
      int splitIndex = facetValue.indexOf(FACET_ID_LABEL_DELIMITER);
      if (splitIndex == -1)
      {
         throw new IllegalArgumentException("malformed facet value [" + facetValue + "].");
      }

      FacetValue value = new FacetValue();
      value.id = facetValue.substring(0, splitIndex);
      value.label = facetValue.substring(splitIndex + FACET_ID_LABEL_DELIMITER.length());

      return value;
   }


   public static class FacetValue
   {
      public String id;
      public String label;
   }
}
