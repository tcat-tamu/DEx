package edu.tamu.tcat.dex.importer;

import java.io.IOException;
import java.io.Reader;
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
import edu.tamu.tcat.trc.entries.common.dto.DateDescriptionDTO;
import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.bib.AuthorReference;
import edu.tamu.tcat.trc.entries.types.bib.Work;
import edu.tamu.tcat.trc.entries.types.bib.dto.AuthorRefDV;
import edu.tamu.tcat.trc.entries.types.bib.dto.PublicationInfoDV;
import edu.tamu.tcat.trc.entries.types.bib.dto.TitleDV;
import edu.tamu.tcat.trc.entries.types.bib.dto.WorkDV;
import edu.tamu.tcat.trc.entries.types.bib.repo.EditWorkCommand;
import edu.tamu.tcat.trc.entries.types.bib.repo.EditionMutator;
import edu.tamu.tcat.trc.entries.types.bib.repo.WorkRepository;
import edu.tamu.tcat.trc.entries.types.bio.dto.PersonNameDTO;
import edu.tamu.tcat.trc.entries.types.bio.repo.EditPersonCommand;
import edu.tamu.tcat.trc.entries.types.bio.repo.PeopleRepository;
import edu.tamu.tcat.trc.extract.dto.ExtractDTO;
import edu.tamu.tcat.trc.extract.dto.ReferenceDTO;

public class DexImportService
{
   private static final Logger logger = Logger.getLogger(DexImportService.class.getName());

   private ExtractRepository extractRepo;
   private PeopleRepository peopleRepo;
   private WorkRepository worksRepo;

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

   public void activate()
   {
      Objects.requireNonNull(extractRepo, "No extract repository provided");
      Objects.requireNonNull(peopleRepo, "No people repository provided");
      Objects.requireNonNull(worksRepo, "No works repository provided");

      extractListenerRegistration = extractRepo.register(evt -> logger.log(Level.INFO, evt.getUpdateAction().toString().toLowerCase() + " extract [" + evt.getEntityId() + "]."));
      workListenerRegistration = worksRepo.addUpdateListener(evt -> logger.log(Level.INFO, evt.getUpdateAction().toString().toLowerCase() + " work [" + evt.getEntityId() + "]."));
      peopleListenerRegistration = peopleRepo.addUpdateListener(evt -> logger.log(Level.INFO, evt.getUpdateAction().toString().toLowerCase() + " person [" + evt.getEntityId() + "]."));
   }

   public void dispose()
   {
      try
      {
         extractListenerRegistration.close();
      }
      catch (Exception e)
      {
         logger.log(Level.WARNING, "Failed to close update listener on extract repository.", e);
      }

      try
      {
         workListenerRegistration.close();
      }
      catch (Exception e)
      {
         logger.log(Level.WARNING, "Failed to close update listener on work repository.", e);
      }

      try
      {
         peopleListenerRegistration.close();
      }
      catch (Exception e)
      {
         logger.log(Level.WARNING, "Failed to close update listener on people repository.", e);
      }

      this.extractRepo = null;
      this.peopleRepo = null;
      this.worksRepo = null;
   }


   public void importManuscriptTEI(String manuscriptId, Reader tei) throws DexImportException
   {
      ManuscriptImportDTO manuscript;
      try
      {
         manuscript = ManuscriptParser.load(tei);
      }
      catch (IOException e)
      {
         throw new IllegalStateException("Unable to load TEI content", e);
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

      // TODO: delete extracts associated with manuscript prior to adding new ones or find a way to update existing extracts

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

      // TODO: remove all editions prior to adding new ones or find a way to update existing editions

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
