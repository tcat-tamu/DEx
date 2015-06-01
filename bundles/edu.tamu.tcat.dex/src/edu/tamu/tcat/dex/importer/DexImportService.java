package edu.tamu.tcat.dex.importer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

public class DexImportService
{
   private static final Logger logger = Logger.getLogger(DexImportService.class.getName());

   private ExtractRepository extractRepo;
   private PeopleRepository peopleRepo;
   private WorkRepository worksRepo;

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
   }

   public void dispose()
   {
   }


   public void importManuscriptTEI(String tei) throws DexImportException
   {
      Reader teiReader = new StringReader(tei);

      ManuscriptImportDTO manuscript;
      try
      {
         manuscript = ManuscriptParser.load(teiReader);
      }
      catch (IOException e)
      {
         throw new IllegalStateException("IO Exception from StringReader!?", e);
      }

      // TODO: set manuscript ID

      EditWorkCommand editManuscriptCommand = worksRepo.create(manuscript.id);
      Work manuscriptWork = ManuscriptImportDTO.instantiate(manuscript);
      editManuscriptCommand.setAll(WorkDV.create(manuscriptWork));

      for (ExtractImportDTO extract : manuscript.extracts)
      {
         extract.manuscriptId = manuscript.id;

         try
         {
            EditExtractCommand editExtractCommand = extractRepo.create(extract.id);
            editExtractCommand.setAll(extract);

            editExtractCommand.execute();
         }
         catch (DramaticExtractException e)
         {
            // TODO: accumulate and report at end.
            logger.log(Level.WARNING, "unable to import extract [" + extract.id + "]", e);
         }
      }
   }

   public void importPeopleAndPlays(String tei) throws DexImportException
   {
      Reader teiReader = new StringReader(tei);

      ImportResult importResult;
      try
      {
         importResult = PeopleAndPlaysParser.load(teiReader);
      }
      catch (IOException e)
      {
         throw new IllegalStateException("IO Exception from StringReader!?", e);
      }

      for (PlaywrightImportDTO playwright : importResult.playwrights.values())
      {
         if (playwright.names.isEmpty()) {
            logger.log(Level.WARNING, "skipping playwright with no names");
            continue;
         }

         // save playwright
         EditPersonCommand editCommand = peopleRepo.create(playwright.id);

         List<PersonNameDTO> personNames = playwright.names.stream()
            .map(n -> {
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

      for (PlayImportDTO play : importResult.plays.values())
      {
         // save plays
         EditWorkCommand editCommand = worksRepo.create(play.id);

         editCommand.setTitles(play.titles.stream()
               .map(t -> {
                  TitleDV dto = new TitleDV();
                  dto.title = t;

                  return dto;
               })
               .collect(Collectors.toList()));

         editCommand.setAuthors(play.playwrightRefs.stream()
               .map(ref -> {
                  AuthorRefDV dto = new AuthorRefDV();
                  dto.role = "Playwright";
                  dto.authorId = ref.playwrightId;
                  // HACK: store full name in last name
                  dto.lastName = ref.displayName;

                  return dto;
               })
               .collect(Collectors.toList()));

         for (EditionDTO edition : play.editions)
         {
            EditionMutator editionMutator = editCommand.createEdition();

            TitleDV titleDTO = new TitleDV();
            titleDTO.title = edition.title;
            editionMutator.setTitles(Collections.singleton(titleDTO));

            editionMutator.setAuthors(edition.editors.stream()
                  .map(name -> {
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

            // HACK: store link as summary
            editionMutator.setSummary(edition.link.toString());
         }

         editCommand.execute();
      }

      for (CharacterImportDTO character : importResult.characters.values())
      {
         if (character.names.isEmpty()) {
            logger.log(Level.WARNING, "skipping character with no names");
            continue;
         }

         // save characters
         EditPersonCommand editCommand = peopleRepo.create(character.id);

         List<PersonNameDTO> personNameDTOs = character.names.stream()
               .map(name -> {
                  PersonNameDTO dto = new PersonNameDTO();
                  dto.displayName = name;

                  return dto;
               })
               .collect(Collectors.toList());


         editCommand.setName(personNameDTOs.get(0));
         editCommand.setNames(new HashSet<>(personNameDTOs.subList(1, personNameDTOs.size())));

         // TODO: save references to plays
         editCommand.execute();
      }
   }

}
