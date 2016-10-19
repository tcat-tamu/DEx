package edu.tamu.tcat.dex.importer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import edu.tamu.tcat.dex.TrcBiblioType;
import edu.tamu.tcat.dex.importer.PeopleAndPlaysParser.ImportResult;
import edu.tamu.tcat.dex.importer.model.CharacterImportDTO;
import edu.tamu.tcat.dex.importer.model.ExtractImportDTO;
import edu.tamu.tcat.dex.importer.model.ManuscriptImportDTO;
import edu.tamu.tcat.dex.importer.model.PlayImportDTO;
import edu.tamu.tcat.dex.importer.model.PlayImportDTO.EditionDTO;
import edu.tamu.tcat.dex.importer.model.PlayImportDTO.PlaywrightReferenceDTO;
import edu.tamu.tcat.dex.importer.model.PlaywrightImportDTO;
import edu.tamu.tcat.dex.trc.extract.DramaticExtractException;
import edu.tamu.tcat.dex.trc.extract.EditExtractCommand;
import edu.tamu.tcat.dex.trc.extract.ExtractRepository;
import edu.tamu.tcat.dex.trc.extract.dto.ReferenceDTO;
import edu.tamu.tcat.osgi.config.ConfigurationProperties;
import edu.tamu.tcat.trc.entries.common.dto.DateDescriptionDTO;
import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.biblio.AuthorReference;
import edu.tamu.tcat.trc.entries.types.biblio.TitleDefinition;
import edu.tamu.tcat.trc.entries.types.biblio.Work;
import edu.tamu.tcat.trc.entries.types.biblio.dto.AuthorReferenceDTO;
import edu.tamu.tcat.trc.entries.types.biblio.dto.PublicationInfoDTO;
import edu.tamu.tcat.trc.entries.types.biblio.dto.TitleDTO;
import edu.tamu.tcat.trc.entries.types.biblio.repo.EditWorkCommand;
import edu.tamu.tcat.trc.entries.types.biblio.repo.EditionMutator;
import edu.tamu.tcat.trc.entries.types.biblio.repo.WorkRepository;
import edu.tamu.tcat.trc.entries.types.bio.Person;
import edu.tamu.tcat.trc.entries.types.bio.dto.PersonNameDTO;
import edu.tamu.tcat.trc.entries.types.bio.repo.EditPersonCommand;
import edu.tamu.tcat.trc.entries.types.bio.repo.PeopleRepository;

public class DexImportService
{
   // TODO perhaps set this up in the app layer (i.e. REST resource) rather than as an OSGi service
   // TODO split into MSS Import Service and People and Plays Import Service


   private static final Logger logger = Logger.getLogger(DexImportService.class.getName());

   private static final String CONFIG_TEI_FILE_LOCATION = "dex.tei.filecache.path";

   private ExtractRepository extractRepo;
   private PeopleRepository peopleRepo;
   private WorkRepository worksRepo;

   private String teiFileLocation;

   private AutoCloseable extractListenerRegistration;

   public void setExtractRepository(ExtractRepository repo)
   {
      this.extractRepo = repo;
   }

   public void setPeopleRepository(PeopleRepository repo)
   {
      this.peopleRepo = repo;
   }

   public void setWorksRepository(WorkRepository repo)
   {
      this.worksRepo = repo;
   }

   public void setConfig(ConfigurationProperties config)
   {
      teiFileLocation = config.getPropertyValue(CONFIG_TEI_FILE_LOCATION, String.class);
   }

   public void activate()
   {
      Objects.requireNonNull(extractRepo, "No extract repository provided");
      Objects.requireNonNull(peopleRepo, "No people repository provided");
      Objects.requireNonNull(worksRepo, "No works repository provided");
      Objects.requireNonNull(teiFileLocation, "No TEI file location provided");

      extractListenerRegistration = extractRepo.register(evt -> 
            logger.log(Level.INFO, evt.getUpdateAction() + " extract [" + evt.getEntityId() + "]."));
   }

   public void dispose()
   {
      releaseRegistration(extractListenerRegistration, "extract repository");
//      releaseRegistration(workListenerRegistration, "work repository");
//      releaseRegistration(peopleListenerRegistration, "people repository");

      this.extractRepo = null;
      this.peopleRepo = null;
      this.worksRepo = null;
   }

   private void releaseRegistration(AutoCloseable reg, String repoName)
   {
      try
      {
         if (reg != null)
            reg.close();
      }
      catch (Exception e)
      {
         logger.log(Level.WARNING, "Failed to close update listener on " + repoName, e);
      }
   }

   public ManuscriptImportDTO parseManuscript(String msId, InputStream tei) throws DexImportException
   {
      try
      {
         ManuscriptImportDTO ms = ManuscriptParser.load(tei);
         ms.id = msId;

         return ms;
      }
      catch (IOException e)
      {
         throw new DexImportException("Unable to load TEI content", e);
      }
   }

   /**
    * Imports all dramatic extracts from the supplied TEI data.
    *
    * @param manuscriptId
    * @param tei
    * @throws DexImportException
    */
   public void importManuscriptTEI(String manuscriptId, InputStream tei) throws DexImportException
   {
      // save TEI to file on disk
      String filename = manuscriptId + ".xml";
      Path teiFilePath = Paths.get(teiFileLocation, filename);
      try
      {
         Files.copy(tei, teiFilePath, StandardCopyOption.REPLACE_EXISTING);
      }
      catch (IOException e)
      {
         throw new DexImportException("Unable to save TEI content to file", e);
      }

      // read saved TEI and import into system
      ManuscriptImportDTO manuscript;
      try
      {
         InputStream input = Files.newInputStream(teiFilePath, StandardOpenOption.READ);
         manuscript = ManuscriptParser.load(input);
      }
      catch (IOException e)
      {
         throw new IllegalArgumentException("Unable to load TEI content", e);
      }

      manuscript.id = manuscriptId;
      SaveManuscriptTask task = new SaveManuscriptTask(manuscript);
      task.run();
   }

   public void importPeopleAndPlaysTEI(Reader tei) throws DexImportException
   {
      ImportResult importResult;
      try
      {
         importResult = PeopleAndPlaysParser.load(tei);
      }
      catch (IOException e)
      {
         throw new DexImportException("Unable to load TEI content", e);
      }

      importResult.playwrights.values().stream()
         .filter(pw -> !pw.names.isEmpty())
         .forEach(this::savePlaywright);

      importResult.plays.values().stream()
         .forEach(this::savePlay);

      importResult.characters.values().stream()
         .filter(c -> !c.names.isEmpty())
         .forEach(this::saveCharacter);
   }

   public void exportManuscriptTEI(String manuscriptId, OutputStream out)
   {
      String filename = manuscriptId + ".xml";
      Path teiPath = Paths.get(teiFileLocation, filename);
      try
      {
         Files.copy(teiPath, out);
      }
      catch (IOException e)
      {
         throw new IllegalArgumentException("Unable to export manuscript with ID [" + manuscriptId + "].", e);
      }
   }

   /**
    * Saves an imported play
    *
    * @param play
    */
   private void savePlay(PlayImportDTO play)
   {
      EditWorkCommand editCommand = createOrEditWork(play.id);

      editCommand.setType(TrcBiblioType.Play.toString());

      editCommand.setTitles(play.titles.stream()
            .map(this::adaptToTitle)
            .collect(Collectors.toList()));

      editCommand.setAuthors(play.playwrightRefs.stream()
            .map(this::adaptToAuthor)
            .collect(Collectors.toList()));

      clearEditions(play, editCommand);

      play.editions.stream().forEach(ed -> addEdition(editCommand, ed));

      editCommand.execute();
   }

   private TitleDTO adaptToTitle(String t)
   {
      TitleDTO dto = new TitleDTO();
      dto.title = t;
      dto.type = ExtractRepository.TITLE_TYPE;

      return dto;
   }

   private AuthorReferenceDTO adaptToAuthor(PlaywrightReferenceDTO ref)
   {
      AuthorReferenceDTO dto = new AuthorReferenceDTO();
      dto.role = "Playwright";
      dto.authorId = ref.playwrightId;

      // HACK: storing author reference's full name in lastName field.
      dto.lastName = ref.displayName;
      return dto;
   }

   private void addEdition(EditWorkCommand editCommand, EditionDTO edition)
   {
      EditionMutator editionMutator = editCommand.createEdition();

      TitleDTO titleDTO = new TitleDTO();
      titleDTO.title = edition.title;
      editionMutator.setTitles(Collections.singleton(titleDTO));

      editionMutator.setAuthors(edition.editors.stream()
            .map(this::adaptEditionEditor)
            .collect(Collectors.toList()));

      PublicationInfoDTO pubInfoDTO = new PublicationInfoDTO();
      pubInfoDTO.publisher = edition.publisher;
      pubInfoDTO.date = new DateDescriptionDTO();
      pubInfoDTO.date.description = edition.date;
      editionMutator.setPublicationInfo(pubInfoDTO);

      // HACK: link to online play edition is currently being stored in the summary field.
      if (edition.link != null)
         editionMutator.setSummary(edition.link.toString());
   }

   private AuthorReferenceDTO adaptEditionEditor(String name)
   {
      AuthorReferenceDTO dto = new AuthorReferenceDTO();
      dto.role = "Editor";
      dto.lastName = name;

      return dto;
   }
   private void clearEditions(PlayImportDTO play, EditWorkCommand editCommand)
   {
      // HACK: remove all existing editions. --
      //       -- TODO find a way to update existing editions
      //       We should have a way to determine if a work exists and to retrieve the current
      //       state from the work repo. See new API model for TRC repos and impl on work repo.
      try {
         Work existing = worksRepo.getWork(play.id);
         existing.getEditions().forEach(ed ->
         {
            try {
               editCommand.removeEdition(ed.getId());
            } catch (Exception ex) {
               // no-op
            }
         });
      }
      catch (Exception ex)
      {
         // TODO almost certainly indicates that the work does not exist (i.e., normal use case).
      }
   }

   /**
    * Saves an imported playwright
    *
    * @param playwright
    */
   private void savePlaywright(PlaywrightImportDTO playwright)
   {
      EditPersonCommand editCommand = createOrEditPerson(playwright.id);
      List<PersonNameDTO> personNames = playwright.names.stream()
         .map(this::adaptToNameDto)
         .collect(Collectors.toList());

      // use first name listed as canonical name and all subsequent as other names
      editCommand.setName(personNames.get(0));
      editCommand.setNames(new HashSet<>(personNames.subList(1, personNames.size())));

      editCommand.execute();
   }

   /**
    * Saves an imported character
    *
    * @param character
    */
   private void saveCharacter(CharacterImportDTO character)
   {
      EditPersonCommand editCommand = createOrEditPerson(character.id);

      List<PersonNameDTO> personNameDTOs = character.names.stream()
            .map(this::adaptToNameDto)
            .collect(Collectors.toList());


      editCommand.setName(personNameDTOs.get(0));
      editCommand.setNames(new HashSet<>(personNameDTOs.subList(1, personNameDTOs.size())));

      // TODO: save character-to-play relationships
      editCommand.execute();
   }

   private PersonNameDTO adaptToNameDto(String name) 
   {
      PersonNameDTO dto = new PersonNameDTO();
      dto.displayName = name;

      return dto;
   }

   private EditPersonCommand createOrEditPerson(String id)
   {
      Person person = null;
      try {
         person = peopleRepo.get(id);
      } catch (Exception e) {
         // no-op;
      }
      
      try
      {
         return person == null ? peopleRepo.create(id) : peopleRepo.update(id);
      }
      catch (NoSuchCatalogRecordException e)
      {
         throw new IllegalStateException("Failed to edit person [" + id + "]", e);
      }
   }

   private EditWorkCommand createOrEditWork(String id)
   {
      Work work = null;
      try {
         work = worksRepo.getWork(id);
      } catch (IllegalArgumentException ex) {
         // no-op
      }
      
      return work == null ? worksRepo.createWork(id) : worksRepo.editWork(id);
   }

   private class SaveManuscriptTask implements Runnable
   {
      // TODO collect errors and log status
      private final ManuscriptImportDTO manuscript;
      
      /**
       * @param manuscript The manuscript to be saved.
       */
      public SaveManuscriptTask(ManuscriptImportDTO manuscript)
      {
         this.manuscript = manuscript;
      }
      
      /**
       * Saves an imported manuscript
       *
       * @param manuscript
       */
      public void run()
      {
         saveManuscript().thenAccept(id -> saveExtracts(manuscript));
      }
   
      /** Saves information about the manuscript. */
      private CompletableFuture<String> saveManuscript()
      {
         EditWorkCommand editCmd = createOrEditWork(manuscript.id);
         
         // NOTE we don't get the ID here, because there is, more or less, a one-to-one
         //      correspondence between ms authors and mss. We are using their names for
         //      display and don't currently plan to reference these authors formally 
         AuthorReferenceDTO author = new AuthorReferenceDTO();
         author.lastName = manuscript.author;
         
         TitleDTO title = new TitleDTO();
         title.title = manuscript.title;
         title.type = ExtractRepository.TITLE_TYPE;
         
         editCmd.setAuthors(Arrays.asList(author));
         editCmd.setTitles(Arrays.asList(title));
         editCmd.setSummary(manuscript.links);
         editCmd.setType(TrcBiblioType.Manuscript.toString());
   
         Future<String> future = editCmd.execute();
         return CompletableFuture.completedFuture(unwrap(future));    // NOTE runs on fork-join pool
      }
      
   
      private void saveExtracts(ManuscriptImportDTO manuscript)
      {
         try {
            // HACK: hope this isn't async.
            extractRepo.removeByManuscriptId(manuscript.id);
         } catch (DramaticExtractException e) {
            logger.log(Level.WARNING, "Unable to remove existing extracts from manuscript [" + manuscript.id + "].", e);
         }
   
         // TODO seems like we could do this iteration within the 
         //      repo layer and use a single connection
         manuscript.extracts.forEach(this::saveExtract);
      }
      
      /**
       * Saves an imported extract
       *
       * @param extract
       */
      private void saveExtract(ExtractImportDTO extract)
      {
         
         extract.manuscript = new ReferenceDTO();
         extract.manuscript.id = manuscript.id;
         extract.manuscript.title = manuscript.title;

         extract.speakers = extract.speakerIds.parallelStream()
               .map(this::getNameReference)
               .collect(Collectors.toSet());

         updateSourceDocumentTitle(extract);
         try
         {
            EditExtractCommand editCmd = extractRepo.createOrEdit(extract.id);
            editCmd.setAuthor(extract.author);
            editCmd.setManuscriptId(extract.manuscript == null ? null : extract.manuscript.id);
            editCmd.setManuscriptTitle(extract.manuscript == null ? null : extract.manuscript.title);
            editCmd.setSourceId(extract.source == null ? null : extract.source.id);
            editCmd.setSourceTitle(extract.source == null ? null : extract.source.title);
            editCmd.setSourceRef(extract.sourceRef);
            editCmd.setTEIContent(extract.teiContent);
            editCmd.setFolioIdentifier(extract.folioIdent);
            editCmd.setMsIndex(extract.msIndex);
            editCmd.setSpeakers(extract.speakers);
            editCmd.setPlaywrights(extract.playwrights);
   
            editCmd.execute();
         }
         catch (DramaticExtractException e)
         {
            // TODO: accumulate and report at end.
            logger.log(Level.WARNING, "unable to import extract [" + extract.id + "] in [" + manuscript.id + "].");
         }
      }
      
      private void updateSourceDocumentTitle(ExtractImportDTO extract)
      {
         String sourceTitle = null;
         try
         {
            // resolve extract source and set source display title on extract
            Work source = worksRepo.getWork(extract.sourceId);
            TitleDefinition title = source.getTitle();
            sourceTitle = title != null && title.get(ExtractRepository.TITLE_TYPE) != null
                  ? title.get(ExtractRepository.TITLE_TYPE).getFullTitle()
                  : "Unknown";
   
            // set playwrights on extract
            for (AuthorReference aRef : source.getAuthors())
            {
               ReferenceDTO ref = new ReferenceDTO();
               ref.id = aRef.getId();
               String firstName = aRef.getFirstName();
               String lastName = aRef.getLastName();
               ref.title = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
               
               extract.playwrights.add(ref);
            }
         }
         catch (Exception e)
         {
            // HACK. Should be IllegalArgumentException but TRC Doc Repo throws a NullPointerException from
            //       within the caching mechanism. 
            logger.log(Level.WARNING, "unable to resolve referenced play [" + extract.sourceId + "] in [" + extract.manuscript.id + "].");
         }
   
         extract.source = new ReferenceDTO();
         extract.source.id = extract.sourceId;
         extract.source.title = sourceTitle;
      }
      
      private ReferenceDTO getNameReference(String personId)
      {
         ReferenceDTO dto = new ReferenceDTO();
         dto.id = personId;
         dto.title = "Unknown";
         try
         {
            Person person = peopleRepo.get(personId);
            if (person != null && person.getCanonicalName() != null)
               dto.title = person.getCanonicalName().getDisplayName();
         }
         catch (Exception e)
         {
            logger.log(Level.WARNING, "Unable to resolve name of referenced speaker [" + personId + "] in [" + manuscript.id + "]");
         }
   
         return dto;
      }
   
      public <X> X unwrap(Future<X> future) {
         try
         {
            return future.get();
         }
         catch (InterruptedException e)
         {
            throw new IllegalStateException("Failed to save manuscript [" + manuscript.id +"]", e);
         }
         catch (ExecutionException e)
         {
            Throwable cause = e.getCause();
            
            if (cause instanceof RuntimeException)
               throw (RuntimeException)cause;
               
            throw new IllegalStateException("Failed to save manuscript [" + manuscript.id +"]", cause);
         }
      }
      
   
   }

   

}
