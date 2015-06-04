package edu.tamu.tcat.dex.psql.test;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

import edu.tamu.tcat.db.exec.sql.SqlExecutor;
import edu.tamu.tcat.db.postgresql.exec.PostgreSqlExecutorService;
import edu.tamu.tcat.db.provider.DataSourceProvider;
import edu.tamu.tcat.dex.importer.DexImportService;
import edu.tamu.tcat.dex.trc.entry.ExtractRepository;
import edu.tamu.tcat.dex.trc.entry.tei.transform.ExtractManipulationUtil;
import edu.tamu.tcat.osgi.config.ConfigurationProperties;
import edu.tamu.tcat.trc.entries.core.IdFactory;
import edu.tamu.tcat.trc.entries.types.bib.postgres.PsqlWorkRepo;
import edu.tamu.tcat.trc.entries.types.bib.repo.WorkRepository;
import edu.tamu.tcat.trc.entries.types.bio.postgres.PsqlPeopleRepo;
import edu.tamu.tcat.trc.entries.types.bio.repo.PeopleRepository;
import edu.tamu.tcat.trc.extract.postgres.PsqlDataSourceProvider;
import edu.tamu.tcat.trc.extract.postgres.PsqlExtractRepo;
import edu.tamu.tcat.trc.extract.search.ExtractSearchService;
import edu.tamu.tcat.trc.extract.search.solr.DramaticExtractsSearchService;

public class ServiceHelper
{
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
         BasicConfigurationProperties basicConfig = new BasicConfigurationProperties();
         basicConfig.setProperty("db.postgres.url", "jdbc:postgresql://localhost:5432/dex");
         basicConfig.setProperty("db.postgres.user", "postgres");
         basicConfig.setProperty("db.postgres.pass", "1Password2");
         basicConfig.setProperty("dex.xslt.tei.original", "/home/CITD/matt.barry/git/git.citd.tamu.edu/dex.deploy/xslt/tei-original.xsl");
         basicConfig.setProperty("dex.xslt.tei.normalized", "/home/CITD/matt.barry/git/git.citd.tamu.edu/dex.deploy/xslt/tei-normalized.xsl");
         basicConfig.setProperty("solr.api.endpoint", URI.create("http://mbarry.citd.tamu.edu:8983/solr/"));
         basicConfig.setProperty("dex.solr.core", "extracts");

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

         ExtractRepository repo = getExtractRepository();
         dess.setRepo(repo);

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
