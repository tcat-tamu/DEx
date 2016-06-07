package edu.tamu.tcat.dex.trc.extract.search.solr;

import edu.tamu.tcat.dex.trc.extract.ExtractRepository;
import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.biblio.TitleDefinition;
import edu.tamu.tcat.trc.entries.types.biblio.Work;
import edu.tamu.tcat.trc.entries.types.biblio.repo.WorkRepository;
import edu.tamu.tcat.trc.entries.types.bio.Person;
import edu.tamu.tcat.trc.entries.types.bio.repo.PeopleRepository;

public class FacetValueManipulationUtil
{
   /**
    * Delimiter between ID and display value for faceting
    */
   private static final String FACET_ID_LABEL_DELIMITER = "::";

   private final PeopleRepository peopleRepo;
   private final WorkRepository workRepo;

   public FacetValueManipulationUtil(PeopleRepository pRepo, WorkRepository wRepo)
   {
      peopleRepo = pRepo;
      workRepo = wRepo;
   }

   /**
    * Transforms a work ID into a value for faceting.
    *
    * @param id
    * @return
    * @throws IllegalStateException when unable to get an associated title for the work ID.
    */
   public String getWorkFacetValue(String id) throws FacetValueException
   {
      try
      {
         Work work = workRepo.getWork(id);
         TitleDefinition t = work.getTitle();
         String title = t != null && t.get(ExtractRepository.TITLE_TYPE) != null
               ? t.get(ExtractRepository.TITLE_TYPE).getFullTitle()
               : "Unknown";
         return id + FACET_ID_LABEL_DELIMITER + title;
      }
      catch (IllegalArgumentException e)
      {
         throw new FacetValueException("unable to find work [" + id + "] for faceting.", e);
      }
      catch (Exception e)
      {
         throw new FacetValueException("unable to get facet value for work [" + id + "].", e);
      }
   }

   /**
    * Transforms a person ID into a value for faceting.
    *
    * @param id
    * @return
    * @throws IllegalStateException when unable to get an associated name for the person ID.
    */
   public String getPersonFacetValue(String id) throws FacetValueException
   {
      try
      {
         Person person = peopleRepo.get(id);
         String name = person.getCanonicalName().getDisplayName();
         return id + FACET_ID_LABEL_DELIMITER + name;
      }
      catch (NoSuchCatalogRecordException e)
      {
         throw new FacetValueException("unable to find person [" + id + "] for faceting.", e);
      }
      catch (Exception e)
      {
         throw new FacetValueException("unable to get facet value for person [" + id + "].", e);
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
