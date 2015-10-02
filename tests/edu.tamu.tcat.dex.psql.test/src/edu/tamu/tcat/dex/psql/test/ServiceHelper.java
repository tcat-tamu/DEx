package edu.tamu.tcat.dex.psql.test;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import edu.tamu.tcat.db.exec.sql.SqlExecutor;
import edu.tamu.tcat.db.postgresql.exec.PostgreSqlExecutorService;
import edu.tamu.tcat.db.provider.DataSourceProvider;
import edu.tamu.tcat.dex.importer.DexImportService;
import edu.tamu.tcat.dex.trc.entry.ExtractRepository;
import edu.tamu.tcat.dex.trc.entry.tei.transform.ExtractManipulationUtil;
import edu.tamu.tcat.osgi.config.ConfigurationProperties;
import edu.tamu.tcat.trc.entries.types.bib.postgres.PsqlWorkRepo;
import edu.tamu.tcat.trc.entries.types.biblio.repo.WorkRepository;
import edu.tamu.tcat.trc.entries.types.bio.postgres.PsqlPeopleRepo;
import edu.tamu.tcat.trc.entries.types.bio.repo.PeopleRepository;
import edu.tamu.tcat.trc.extract.postgres.PsqlExtractRepo;
import edu.tamu.tcat.trc.extract.search.ExtractSearchService;
import edu.tamu.tcat.trc.extract.search.solr.DramaticExtractsSearchService;
import edu.tamu.tcat.trc.repo.IdFactory;
import edu.tamu.tcat.trc.repo.postgres.PsqlDataSourceProvider;

public class ServiceHelper
{
   private static final String PROPERTY_CONFIG_FILE_PATH = "dex.war.config.file";

   // cached service instances
   private static ConfigurationProperties config;
   private static DataSourceProvider dataSourceProvider;
   private static IdFactory idFactory;
   private static SqlExecutor sqlExecutor;
   private static PeopleRepository peopleRepository;
   private static WorkRepository workRepository;
   private static ExtractRepository extractRepository;
   private static ExtractManipulationUtil extractManipulationUtil;
   private static ExtractSearchService extractSearchService;
   private static DexImportService importService;

   public static ConfigurationProperties getConfig()
   {
      if (config == null)
      {
         String filePath = System.getProperty(PROPERTY_CONFIG_FILE_PATH);

         if (filePath == null)
         {
            throw new IllegalArgumentException("config file path property [" + PROPERTY_CONFIG_FILE_PATH + "] is not defined.");
         }

         Properties properties = new Properties();
         try (Reader reader = new FileReader(filePath))
         {
            properties.load(reader);
         }
         catch (IOException e)
         {
            throw new RuntimeException("Unable to read properties file [" + filePath + "]", e);
         }

         BasicConfigurationProperties basicConfig = new BasicConfigurationProperties();
         properties.forEach((k,v) -> basicConfig.setProperty(k.toString(), v.toString()));

         config = basicConfig;
      }

      Objects.requireNonNull(config, "config service not set");
      return config;
   }

   public static DataSourceProvider getDataSourceProvider()
   {
      if (dataSourceProvider == null)
      {
         PsqlDataSourceProvider pdsp = new PsqlDataSourceProvider();

         ConfigurationProperties config = getConfig();
         pdsp.bind(config);

         pdsp.activate();

         dataSourceProvider = pdsp;
      }

      Objects.requireNonNull(dataSourceProvider, "data source provider service not set");
      return dataSourceProvider;
   }

   public static IdFactory getIdFactory()
   {
      if (idFactory == null)
      {
         idFactory = context -> UUID.randomUUID().toString();
      }

      Objects.requireNonNull(idFactory, "id factory service not set");
      return idFactory;
   }

   public static SqlExecutor getSqlExecutor()
   {
      if (sqlExecutor == null)
      {
         PostgreSqlExecutorService psqlExecutor = new PostgreSqlExecutorService();

         DataSourceProvider dsp = getDataSourceProvider();
         psqlExecutor.bind(dsp);

         psqlExecutor.activate();

         sqlExecutor = psqlExecutor;
      }

      Objects.requireNonNull(sqlExecutor, "sql executor service not set");
      return sqlExecutor;
   }

   public static PeopleRepository getPeopleRepository()
   {
      if (peopleRepository == null)
      {
         PsqlPeopleRepo repo = new PsqlPeopleRepo();

         SqlExecutor executor = getSqlExecutor();
         repo.setDatabaseExecutor(executor);

         IdFactory idFactory = getIdFactory();
         repo.setIdFactory(idFactory);

         repo.activate();

         peopleRepository = repo;
      }

      Objects.requireNonNull(peopleRepository, "people repository service not set");
      return peopleRepository;
   }

   public static WorkRepository getWorkRepository()
   {
      if (workRepository == null)
      {
         PsqlWorkRepo repo = new PsqlWorkRepo();

         SqlExecutor executor = getSqlExecutor();
         repo.setDatabaseExecutor(executor);

         IdFactory factory = getIdFactory();
         repo.setIdFactory(factory);

         PeopleRepository peopleRepo = getPeopleRepository();
         repo.setPeopleRepo(peopleRepo);

         repo.activate();

         workRepository = repo;
      }

      Objects.requireNonNull(workRepository, "works repository service not set");
      return workRepository;
   }

   public static ExtractRepository getExtractRepository()
   {
      if (extractRepository == null)
      {
         PsqlExtractRepo repo = new PsqlExtractRepo();

         SqlExecutor executor = getSqlExecutor();
         repo.setSqlExecutor(executor);

         repo.activate();

         extractRepository = repo;
      }

      Objects.requireNonNull(extractRepository, "extract repository service not set");
      return extractRepository;
   }

   public static ExtractManipulationUtil getExtractManipulationUtil()
   {
      if (extractManipulationUtil == null)
      {
         extractManipulationUtil = new ExtractManipulationUtil();

         ConfigurationProperties config = getConfig();
         extractManipulationUtil.setConfiguration(config);

         extractManipulationUtil.activate();
      }

      Objects.requireNonNull(extractManipulationUtil, "extract manipulation utility service not set");
      return extractManipulationUtil;
   }

   public static ExtractSearchService getSearchService()
   {
      if (extractSearchService == null)
      {
         DramaticExtractsSearchService dess = new DramaticExtractsSearchService();

         ConfigurationProperties config = getConfig();
         dess.setConfig(config);

         ExtractRepository extractRepo = getExtractRepository();
         dess.setRepo(extractRepo);

         PeopleRepository peopleRepo = getPeopleRepository();
         dess.setRepo(peopleRepo);

         WorkRepository workRepo = getWorkRepository();
         dess.setRepo(workRepo);

         ExtractManipulationUtil extractManipulationUtil = getExtractManipulationUtil();
         dess.setExtractManipulationUtil(extractManipulationUtil);

         dess.activate();

         extractSearchService = dess;
      }

      Objects.requireNonNull(extractSearchService, "extract search service not set");
      return extractSearchService;
   }

   public static DexImportService getImportService()
   {
      if (importService == null)
      {
         importService = new DexImportService();

         ExtractRepository extractsRepo = getExtractRepository();
         importService.setExtractRepository(extractsRepo);

         PeopleRepository peopleRepo = getPeopleRepository();
         importService.setPeopleRepository(peopleRepo);

         WorkRepository workRepo = getWorkRepository();
         importService.setWorksRepository(workRepo);

         importService.activate();
      }

      Objects.requireNonNull(importService, "import service not set");
      return importService;
   }
}
