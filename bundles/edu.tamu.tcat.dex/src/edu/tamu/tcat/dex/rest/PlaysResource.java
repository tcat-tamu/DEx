package edu.tamu.tcat.dex.rest;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import edu.tamu.tcat.dex.TrcBiblioType;
import edu.tamu.tcat.dex.rest.v1.RepoAdapter;
import edu.tamu.tcat.dex.rest.v1.RestApiV1;
import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.biblio.AuthorList;
import edu.tamu.tcat.trc.entries.types.biblio.AuthorReference;
import edu.tamu.tcat.trc.entries.types.biblio.Work;
import edu.tamu.tcat.trc.entries.types.biblio.repo.WorkRepository;

@Path("/plays")
public class PlaysResource
{
   private static final Logger logger = Logger.getLogger(PlaysResource.class.getName());

   private WorkRepository repo;

   public void setRepo(WorkRepository repo)
   {
      this.repo = repo;
   }

   public void activate()
   {
      try
      {
         Objects.requireNonNull(repo, "No repository provided");
      }
      catch (Exception ex)
      {
         logger.log(Level.SEVERE, "Failed to start /plays REST resource.", ex);
         throw ex;
      }
   }

   public void dispose()
   {
      repo = null;
   }

   @GET
   @Produces(MediaType.APPLICATION_JSON)
   public List<RestApiV1.PlayBibEntry> bibliography()
   {
      Iterable<Work> works = () -> this.repo.getAllWorks();
      return StreamSupport.stream(works.spliterator(), false)
            .filter(work -> TrcBiblioType.Play.toString().equals(work.getType()))
            .sorted((a, b) -> getSortKey(a).compareToIgnoreCase(getSortKey(b)))
            .map(RepoAdapter::toPlayBibEntryDTO)
            .collect(Collectors.toList());
   }

   @GET
   @Path("/{id}")
   @Produces(MediaType.APPLICATION_JSON)
   public RestApiV1.Play get(@PathParam("id") String id)
   {
      String playType = TrcBiblioType.Play.toString();
      try
      {
         Work work = repo.getWork(id);
         String workType = work.getType();
         if (!playType.equals(workType))
         {
            throw new NoSuchCatalogRecordException("Wrong bibliographic type: expected [" + playType + "] but recieved [" + workType + "].");
         }
         return RepoAdapter.toPlayDTO(work);
      }
      catch (NoSuchCatalogRecordException e)
      {
         throw new NotFoundException("unable to find play [" + id + "]");
      }
   }

   /**
    * @param work
    * @return A sort key based on the first author's last name, followed by the rest of his/her name; will not be null
    */
   private String getSortKey(Work work)
   {
      AuthorList authors = work.getAuthors();
      if (authors == null || authors.size() == 0)
      {
         return "";
      }

      AuthorReference ref = authors.get(0);
      if (ref == null)
      {
         return "";
      }

      // HACK: full playwright names are stored on the "lastName" field
      String name = ref.getLastName();
      if (name == null)
      {
         return  "";
      }

      String[] nameParts = name.trim().split("\\s+");


      // assume last name is the last part of the name
      // prioritize last name and then by remaining name parts
      int len = nameParts.length;

      StringJoiner sj = new StringJoiner(" ");
      for (int i = 0; i < len; i++) {
         // circular shift by one element
         int shiftedIndex = (len - 1 + i) % len;
         sj.add(nameParts[shiftedIndex]);
      }

      return sj.toString();
   }
}
