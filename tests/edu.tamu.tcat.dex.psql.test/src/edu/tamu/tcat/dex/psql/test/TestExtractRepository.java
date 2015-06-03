package edu.tamu.tcat.dex.psql.test;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.tamu.tcat.db.exec.sql.SqlExecutor;
import edu.tamu.tcat.db.postgresql.exec.PostgreSqlExecutorService;
import edu.tamu.tcat.db.provider.DataSourceProvider;
import edu.tamu.tcat.dex.trc.entry.DramaticExtractException;
import edu.tamu.tcat.dex.trc.entry.EditExtractCommand;
import edu.tamu.tcat.dex.trc.entry.ExtractRepository;
import edu.tamu.tcat.dex.trc.entry.Pair;
import edu.tamu.tcat.dex.trc.entry.tei.transform.ExtractManipulationUtil;
import edu.tamu.tcat.osgi.config.ConfigurationProperties;
import edu.tamu.tcat.trc.extract.postgres.PsqlDataSourceProvider;
import edu.tamu.tcat.trc.extract.postgres.PsqlExtractRepo;
import edu.tamu.tcat.trc.extract.search.ExtractSearchService;
import edu.tamu.tcat.trc.extract.search.solr.DramaticExtractsSearchService;

public class TestExtractRepository
{
   // cached service instances
   private static ConfigurationProperties config;
   private static DataSourceProvider dataSourceProvider;
   private static SqlExecutor sqlExecutor;
   private static ExtractRepository extractRepository;
   private static ExtractManipulationUtil extractManipulationUtil;
   private static ExtractSearchService extractSearchService;

   private static ConfigurationProperties getConfig()
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

      return config;
   }

   private static DataSourceProvider getDataSourceProvider()
   {
      if (dataSourceProvider == null)
      {
         PsqlDataSourceProvider pdsp = new PsqlDataSourceProvider();

         ConfigurationProperties config = getConfig();
         pdsp.bind(config);

         pdsp.activate();

         dataSourceProvider = pdsp;
      }

      return dataSourceProvider;
   }

   private static SqlExecutor getSqlExecutor()
   {
      if (sqlExecutor == null)
      {
         PostgreSqlExecutorService psqlExecutor = new PostgreSqlExecutorService();

         DataSourceProvider dsp = getDataSourceProvider();
         psqlExecutor.bind(dsp);

         psqlExecutor.activate();

         sqlExecutor = psqlExecutor;
      }

      return sqlExecutor;
   }

   private static ExtractRepository getExtractRepository()
   {
      if (extractRepository == null)
      {
         PsqlExtractRepo repo = new PsqlExtractRepo();

         SqlExecutor executor = getSqlExecutor();
         repo.setSqlExecutor(executor);

         repo.activate();

         extractRepository = repo;
      }

      return extractRepository;
   }

   private static ExtractManipulationUtil getExtractManipulationUtil()
   {
      if (extractManipulationUtil == null)
      {
         extractManipulationUtil = new ExtractManipulationUtil();

         ConfigurationProperties config = getConfig();
         extractManipulationUtil.setConfiguration(config);

         extractManipulationUtil.activate();
      }

      return extractManipulationUtil;
   }

   private static ExtractSearchService getSearchService()
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

      return extractSearchService;
   }

   @Test
   public void testSaveEntry() throws InterruptedException, ExecutionException, DramaticExtractException, SAXException, IOException, ParserConfigurationException
   {
      ExtractRepository repo = getExtractRepository();
      // ensure search service is started and listening to repository
      getSearchService();

      EditExtractCommand editCommand = repo.create(UUID.randomUUID().toString());

      editCommand.setAuthor("Matthew J. Barry");
      editCommand.setManuscriptId("MJB_1234");
      editCommand.setSourceId("Shakespeare_Hamlet");
      editCommand.setSourceRef("3.1.64");

      Set<Pair<String, String>> speakers = new HashSet<>(Arrays.asList(Pair.of("Hamlet_Hamlet", "Hamlet")));
      editCommand.setSpeakers(speakers);

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder = dbf.newDocumentBuilder();

      String tei = "<div type=\"extract\" n=\"3.1.64\" corresp=\"#Shakespeare_Hamlet\"><sp who=\"Hamlet\"><l>To be or not to be—<choice><orig>that is the question:</orig><seg type=\"smartalec\">that's not a question</seg></choice></l></sp></div>";
      InputSource is = new InputSource(new StringReader(tei));
      Document document = documentBuilder.parse(is);

      editCommand.setTEIContent(document);

      Future<String> idFuture = editCommand.execute();
      String id = idFuture.get();

      assertNotNull("No ID returned", id);
   }

}
