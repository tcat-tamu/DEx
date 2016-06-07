package edu.tamu.tcat.dex.trc.extract;

import edu.tamu.tcat.trc.entries.notification.UpdateEvent;
import edu.tamu.tcat.trc.entries.notification.UpdateListener;

public interface ExtractRepository
{
   public static final String TITLE_TYPE = "canonical";

   /**
    * Determines the existence of a dramatic extract by ID.
    * This method is expected to be cheaper than fetching the entire extract (if it exists):
    * performance is critical.
    *
    * @param id The unique identifier of an extract
    * @return whether an extract with the given ID exists.
    */
   boolean exists(String id) throws DramaticExtractException;

   /**
    * Gets an extract by ID
    *
    * @param id The ID of the extract to retrieve
    * @return
    * @throws ExtractNotAvailableException if the extract does not exist
    * @throws DramaticExtractException
    */
   DramaticExtract get(String id) throws ExtractNotAvailableException, DramaticExtractException;

   /**
    * Creates an extract with the given ID
    *
    * @param id The ID of the extract
    * @return an {@link EditExtractCommand} for the newly created extract
    * @throws DramaticExtractException
    */
   EditExtractCommand create(String id) throws DramaticExtractException;

   /**
    * Fetches an existing extract for editing
    *
    * @param id The unique ID of the extract
    * @return an {@link EditExtractCommand} for the desired extract
    * @throws ExtractNotAvailableException if the extract does not exist
    * @throws DramaticExtractException
    */
   EditExtractCommand edit(String id) throws ExtractNotAvailableException, DramaticExtractException;

   /**
    * Attempts to edit the extract with the given ID if it exists, or creates a new extract with
    * the given ID if not.
    *
    * @param id The unique ID of the extract
    * @return an {@link EditExtractCommand} for the existing or newly created extract
    * @throws DramaticExtractException
    */
   EditExtractCommand createOrEdit(String id) throws DramaticExtractException;

   /**
    * Deletes the extract with the given ID. If an extract with the given ID does not exist,
    * then this method is a no-op.
    *
    * @param id The unique ID of the extract.
    * @throws DramaticExtractException
    */
   void remove(String id) throws DramaticExtractException;

   /**
    * Add listener to be notified whenever an extract is modified (created, updated or deleted).
    *
    * @param ears The listener to be added.
    * @return A registration handle that allows the listener to be removed.
    */
   AutoCloseable register(UpdateListener<UpdateEvent> ears);

   /**
    * Deletes extracts belonging to the given manuscript.
    * @param id
    * @throws DramaticExtractException
    */
   void removeByManuscriptId(String manuscriptId) throws DramaticExtractException;
}
