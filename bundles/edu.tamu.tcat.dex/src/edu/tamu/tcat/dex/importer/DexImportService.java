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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
import edu.tamu.tcat.dex.importer.model.PlaywrightImportDTO;
import edu.tamu.tcat.dex.trc.entry.DramaticExtractException;
import edu.tamu.tcat.dex.trc.entry.EditExtractCommand;
import edu.tamu.tcat.dex.trc.entry.ExtractRepository;
import edu.tamu.tcat.dex.trc.extract.dto.ExtractDTO;
import edu.tamu.tcat.dex.trc.extract.dto.ReferenceDTO;
import edu.tamu.tcat.osgi.config.ConfigurationProperties;
import edu.tamu.tcat.trc.entries.common.dto.DateDescriptionDTO;
import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.biblio.AuthorReference;
import edu.tamu.tcat.trc.entries.types.biblio.Work;
import edu.tamu.tcat.trc.entries.types.biblio.dto.AuthorRefDV;
import edu.tamu.tcat.trc.entries.types.biblio.dto.PublicationInfoDV;
import edu.tamu.tcat.trc.entries.types.biblio.dto.TitleDV;
import edu.tamu.tcat.trc.entries.types.biblio.dto.WorkDV;
import edu.tamu.tcat.trc.entries.types.biblio.repo.EditWorkCommand;
import edu.tamu.tcat.trc.entries.types.biblio.repo.EditionMutator;
import edu.tamu.tcat.trc.entries.types.biblio.repo.WorkRepository;
import edu.tamu.tcat.trc.entries.types.bio.dto.PersonNameDTO;
import edu.tamu.tcat.trc.entries.types.bio.repo.EditPersonCommand;
import edu.tamu.tcat.trc.entries.types.bio.repo.PeopleRepository;

public class DexImportService
{
   // TODO perhaps set this up in the app layer (i.e. REST resource) rather than as an OSGi service

   private static final Logger logger = Logger.getLogger(DexImportService.class.getName());

   private static final String CONFIG_TEI_FILE_LOCATION = "dex.tei.filecache.path";

   private ExtractRepository extractRepo;
   private PeopleRepository peopleRepo;
   private WorkRepository worksRepo;

   private String teiFileLocation;

   private AutoCloseable workListenerRegistration;
   private AutoCloseable peopleListenerRegistration;
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

      extractListenerRegistration = extractRepo.register(evt -> logger.log(Level.INFO, evt.getUpdateAction().toString().toLowerCase() + " extract [" + evt.getEntityId() + "]."));
      workListenerRegistration = worksRepo.addUpdateListener(evt -> logger.log(Level.INFO, evt.getUpdateAction().toString().toLowerCase() + " work [" + evt.getEntityId() + "]."));
      peopleListenerRegistration = peopleRepo.addUpdateListener(evt -> logger.log(Level.INFO, evt.getUpdateAction().toString().toLowerCase() + " person [" + evt.getEntityId() + "]."));
   }

   public void dispose()
   {
      releaseRegistration(extractListenerRegistration, "extract repository");
      releaseRegistration(workListenerRegistration, "work repository");
      releaseRegistration(peopleListenerRegistration, "people repository");

      this.extractRepo = null;
      this.peopleRepo = null;
      this.worksRepo = null;
   }

   private void releaseRegistration(AutoCloseable reg, String repoName)
   {
      try
      {
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

      saveManuscript(manuscript);
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
    * Saves an imported manuscript
    *
    * @param manuscript
    */
   private void saveManuscript(ManuscriptImportDTO manuscript)
   {
      EditWorkCommand editManuscriptCommand = createOrEditWork(manuscript.id);

      Work manuscriptWork = ManuscriptImportDTO.instantiate(manuscript);
      editManuscriptCommand.setAll(WorkDV.create(manuscriptWork));
      editManuscriptCommand.execute();

      try {
         extractRepo.removebyManuscriptId(manuscript.id);
      }
      catch (DramaticExtractException e) {
         logger.log(Level.WARNING, "Unable to remove existing extracts from manuscript [" + manuscript.id + "].", e);
      }

      for (ExtractImportDTO extract : manuscript.extracts)
      {
         extract.manuscript = ReferenceDTO.create(manuscript.id, manuscript.title);

         extract.speakers = extract.speakerIds.parallelStream()
               .map(id ->
                     {
                        String name = null;
                        try
                        {
                           name = peopleRepo.get(id).getCanonicalName().getDisplayName();
                        }
                        catch (Exception e)
                        {
                           // logger.log(Level.WARNING, "Unable to resolve name of referenced speaker [" + id + "] in [" + manuscript.id + "].", e);
                           logger.log(Level.WARNING, "Unable to resolve name of referenced speaker [" + id + "] in [" + manuscript.id + "].");
                        }

                        return ReferenceDTO.create(id, name);
                     })
               .filter(Objects::nonNull)
               .collect(Collectors.toSet());

         saveExtract(extract);
      }
   }

   /**
    * Saves an imported extract
    *
    * @param extract
    */
   private void saveExtract(ExtractImportDTO extract)
   {
      String sourceTitle = null;
      try
      {
         // resolve extract source and set source display title on extract
         Work source = worksRepo.getWork(extract.sourceId);
         sourceTitle = source.getTitle().getCanonicalTitle().getFullTitle();

         // set playwrights on extract
         for (AuthorReference aRef : source.getAuthors())
         {
            String firstName = aRef.getFirstName();
            String lastName = aRef.getLastName();
            String name = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
            extract.playwrights.add(ReferenceDTO.create(aRef.getId(), name.trim()));
         }
      }
      catch (NoSuchCatalogRecordException e)
      {
         // logger.log(Level.WARNING, "unable to resolve referenced play [" + extract.sourceId + "] in [" + extract.manuscript.id + "].", e);
         logger.log(Level.WARNING, "unable to resolve referenced play [" + extract.sourceId + "] in [" + extract.manuscript.id + "].");
      }
      extract.source = ReferenceDTO.create(extract.sourceId, sourceTitle);

      try
      {
         EditExtractCommand editExtractCommand = extractRepo.createOrEdit(extract.id);
         editExtractCommand.setAll(ExtractDTO.instantiate(extract));

         editExtractCommand.execute();
      }
      catch (DramaticExtractException e)
      {
         // TODO: accumulate and report at end.
         // logger.log(Level.WARNING, "unable to import extract [" + extract.id + "] in [" + extract.manuscript.id + "].", e);
         logger.log(Level.WARNING, "unable to import extract [" + extract.id + "] in [" + extract.manuscript.id + "].");
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
            .map(t ->
                  {
                     TitleDV dto = new TitleDV();
                     dto.title = t;
                     dto.type = "Canonical";

                     return dto;
                  })
            .collect(Collectors.toList()));

      editCommand.setAuthors(play.playwrightRefs.stream()
            .map(ref ->
                  {
                     AuthorRefDV dto = new AuthorRefDV();
                     dto.role = "Playwright";
                     dto.authorId = ref.playwrightId;
                     // HACK: storing author reference's full name in lastName field.
                     dto.lastName = ref.displayName;

                     return dto;
                  })
            .collect(Collectors.toList()));

      clearEditions(play, editCommand);

      for (EditionDTO edition : play.editions)
      {
         EditionMutator editionMutator = editCommand.createEdition();

         TitleDV titleDTO = new TitleDV();
         titleDTO.title = edition.title;
         editionMutator.setTitles(Collections.singleton(titleDTO));

         editionMutator.setAuthors(edition.editors.stream()
               .map(name ->
                     {
                        AuthorRefDV dto = new AuthorRefDV();
                        dto.role = "Editor";
                        dto.lastName = name;

                        return dto;
                     })
               .collect(Collectors.toList()));

         PublicationInfoDV pubInfoDTO = new PublicationInfoDV();
         pubInfoDTO.publisher = edition.publisher;
         pubInfoDTO.date = new DateDescriptionDTO();
         pubInfoDTO.date.description = edition.date;
         editionMutator.setPublicationInfo(pubInfoDTO);

         // HACK: link to online play edition is currently being stored in the summary field.
         if (edition.link != null)
         {
            editionMutator.setSummary(edition.link.toString());
         }
      }

      editCommand.execute();
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
         .map(n ->
               {
                  PersonNameDTO dto = new PersonNameDTO();
                  dto.displayName = n;

                  return dto;
               })
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
            .map(name ->
                  {
                     PersonNameDTO dto = new PersonNameDTO();
                     dto.displayName = name;

                     return dto;
                  })
            .collect(Collectors.toList());


      editCommand.setName(personNameDTOs.get(0));
      editCommand.setNames(new HashSet<>(personNameDTOs.subList(1, personNameDTOs.size())));

      // TODO: save character-to-play relationships
      editCommand.execute();
   }


   private EditPersonCommand createOrEditPerson(String id)
   {
      // HACK: I don't think this is what try/catch blocks are meant to do...
      try
      {
         return peopleRepo.update(id);
      }
      catch (NoSuchCatalogRecordException e)
      {
         return peopleRepo.create(id);
      }
   }

   private EditWorkCommand createOrEditWork(String id)
   {
      // HACK: I don't think this is what try/catch blocks are meant to do...
      try
      {
         return worksRepo.edit(id);
      }
      catch (NoSuchCatalogRecordException e)
      {
         return worksRepo.create(id);
      }
   }

}
