package edu.tamu.tcat.dex.trc.extract.search.solr;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import edu.tamu.tcat.dex.trc.extract.DramaticExtract;
import edu.tamu.tcat.dex.trc.extract.DramaticExtractException;
import edu.tamu.tcat.dex.trc.extract.ExtractNotAvailableException;
import edu.tamu.tcat.dex.trc.extract.ExtractRepository;
import edu.tamu.tcat.dex.trc.extract.search.ExtractQueryCommand;
import edu.tamu.tcat.dex.trc.extract.search.ExtractSearchService;
import edu.tamu.tcat.osgi.config.ConfigurationProperties;
import edu.tamu.tcat.trc.entries.notification.UpdateEvent;
import edu.tamu.tcat.trc.entries.types.biblio.repo.WorkRepository;
import edu.tamu.tcat.trc.entries.types.bio.repo.PeopleRepository;
import edu.tamu.tcat.trc.search.SearchException;
import edu.tamu.tcat.trc.search.solr.impl.TrcQueryBuilder;

public class DramaticExtractsSearchService implements ExtractSearchService
{
   private static final Logger logger = Logger.getLogger(DramaticExtractsSearchService.class.getName());

   /**
    * Configuration property key that defines the URI for the Solr server.
    */
   private static final String CONFIG_SOLR_API_ENDPOINT = "solr.api.endpoint";

   /**
    * Configuration property key that defines the Solr core to be used for dramatic extracts.
    */
   private static final String CONFIG_SOLR_CORE = "dex.solr.core";

   private ExtractRepository extractRepo;
   private PeopleRepository peopleRepo;
   private WorkRepository workRepo;
   private ConfigurationProperties config;
   private HttpSolrClient solrServer;
   private AutoCloseable extractsReg;

   private ExtractManipulationUtil extractManipulationUtil;
   private SearchableDocumentFactory solrDocFactory;

   public void setRepo(ExtractRepository repo)
   {
      extractRepo = repo;
   }

   public void setRepo(PeopleRepository repo)
   {
      peopleRepo = repo;
   }

   public void setRepo(WorkRepository repo)
   {
      workRepo = repo;
   }

   public void setConfig(ConfigurationProperties config)
   {
      this.config = config;
   }

   public void setExtractManipulationUtil(ExtractManipulationUtil extractManipulationUtil)
   {
      this.extractManipulationUtil = extractManipulationUtil;
   }

   public void activate()
   {
      try 
      {
         logger.log(Level.INFO, "Activating " + getClass().getSimpleName());
         
         Objects.requireNonNull(extractRepo, "No extracts repository supplied.");
         Objects.requireNonNull(peopleRepo, "No people repository supplied.");
         Objects.requireNonNull(workRepo, "No works repository supplied.");
         Objects.requireNonNull(config, "No configuration supplied.");
         Objects.requireNonNull(extractManipulationUtil, "No extract manipulation utility provided.");

         // listen for updates from the repository
         extractsReg = extractRepo.register(this::handleUpdateEvent);

         FacetValueManipulationUtil facetUtil = new FacetValueManipulationUtil(peopleRepo, workRepo);
         solrDocFactory = new SearchableDocumentFactory(extractManipulationUtil, facetUtil);

         // Solr setup
         URI solrBaseUri = config.getPropertyValue(CONFIG_SOLR_API_ENDPOINT, URI.class);
         String solrCore = config.getPropertyValue(CONFIG_SOLR_CORE, String.class);
         
         URI coreUri = solrBaseUri.resolve(solrCore);
         logger.info(format("Accessing Solr {0}", coreUri));

         solrServer = new HttpSolrServer(coreUri.toString());
      }
      catch (Exception ex)
      {
         logger.log(Level.SEVERE, "Failed to start dramatic extracts search service", ex);
         throw ex;
      }
   }

   public void dispose()
   {
      if (extractsReg != null)
      {
         try {
            extractsReg.close();
         } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to close extract repository listener registration", e);
         }
      }

      extractRepo = null;
      peopleRepo = null;
      workRepo = null;
      config = null;
      extractManipulationUtil = null;
      extractsReg = null;
   }

   @Override
   public ExtractQueryCommand createQueryCommand() throws SearchException
   {
      TrcQueryBuilder qb = new TrcQueryBuilder(new ExtractSolrConfig());
      FacetValueManipulationUtil facetUtil = new FacetValueManipulationUtil(peopleRepo, workRepo);
      return new ExtractSolrQueryCommand(solrServer, qb, facetUtil);
   }

   /**
    * Dispatch handler for all database events
    *
    * @param evt
    */
   private void handleUpdateEvent(UpdateEvent evt)
   {
      // TODO create monitor and log errors/problems . . . 
      //      need to find a way to pipe through to the REST API
      IndexCreationProblems monitor = new IndexCreationProblems(evt.getEntityId());
      switch (evt.getUpdateAction())
      {
         case CREATE:
            onCreate(evt, monitor);
            break;
         case UPDATE:
            onUpdate(evt, monitor);
            break;
         case DELETE:
            onDelete(evt, monitor);
            break;
      }
   }

   /**
    * Called when a new extract is added to the database
    *
    * @param evt
    */
   protected void onCreate(UpdateEvent evt, IndexCreationProblems monitor)
   {
      Objects.requireNonNull(solrDocFactory, "Solr document factory is not available.");
      
      String id = evt.getEntityId();
      try {
         logger.log(Level.INFO, "Indexing extract " + id);
         
         DramaticExtract extract = extractRepo.get(id);
         SolrInputDocument doc = solrDocFactory.create(extract, monitor);
         
         if (!monitor.isEmpty())
         {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            String json = mapper.writeValueAsString(monitor.warnings);
            logger.log(Level.WARNING, "Extract Indexing Errors: \n" + json);
         }
         
         solrServer.add(doc);
      }
      catch (ExtractNotAvailableException | DramaticExtractException e) {
         logger.log(Level.SEVERE, "Failed to retrieve extract [" + id + "] on create event.", e);
      }
      catch (SolrServerException | IOException e)
      {
         logger.log(Level.SEVERE, format("Failed to index extract [{0}", id), e);
      }
   }

   /**
    * Called when an extract is updated in the database
    *
    * @param evt
    */
   protected void onUpdate(UpdateEvent evt, IndexCreationProblems monitor)
   {
      onCreate(evt, monitor);
   }

   /**
    * Called when an extract is removed from the database
    * @param evt
    */
   protected void onDelete(UpdateEvent evt, IndexCreationProblems monitor)
   {
      String id = evt.getEntityId();

      try
      {
         solrServer.deleteById(id);
      }
      catch (SolrServerException | IOException e)
      {
         logger.log(Level.SEVERE, "Failed to remove extract [" + id + "] from the Solr server.", e);
      }
   }

}
