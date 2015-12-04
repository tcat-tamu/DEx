package edu.tamu.tcat.dex.trc.extract.search.solr;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

import edu.tamu.tcat.dex.trc.entry.DramaticExtract;
import edu.tamu.tcat.dex.trc.entry.DramaticExtractException;
import edu.tamu.tcat.dex.trc.entry.ExtractNotAvailableException;
import edu.tamu.tcat.dex.trc.entry.ExtractRepository;
import edu.tamu.tcat.dex.trc.entry.tei.transform.ExtractManipulationUtil;
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
   private SolrServer solrServer;
   private AutoCloseable repoListenerRegistration;

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
      Objects.requireNonNull(extractRepo, "No extracts repository supplied.");
      Objects.requireNonNull(peopleRepo, "No people repository supplied.");
      Objects.requireNonNull(workRepo, "No works repository supplied.");
      Objects.requireNonNull(config, "No configuration supplied.");
      Objects.requireNonNull(extractManipulationUtil, "No extract manipulation utility provided.");

      // listen for updates from the repository
      repoListenerRegistration = extractRepo.register(this::handleUpdateEvent);

      FacetValueManipulationUtil facetUtil = new FacetValueManipulationUtil(peopleRepo, workRepo);
      solrDocFactory = new SearchableDocumentFactory(extractManipulationUtil, facetUtil);

      // Solr setup
      URI solrBaseUri = config.getPropertyValue(CONFIG_SOLR_API_ENDPOINT, URI.class);
      String solrCore = config.getPropertyValue(CONFIG_SOLR_CORE, String.class);

      URI coreUri = solrBaseUri.resolve(solrCore);

      solrServer = new HttpSolrServer(coreUri.toString());
   }

   public void dispose()
   {
      if (repoListenerRegistration != null)
      {
         try {
            repoListenerRegistration.close();
         }
         catch (Exception e) {
            logger.log(Level.WARNING, "Unable to close extract repository listener registration", e);
         }
      }

      extractRepo = null;
      peopleRepo = null;
      workRepo = null;
      config = null;
      extractManipulationUtil = null;
      repoListenerRegistration = null;
   }

   @Override
   public ExtractQueryCommand createQueryCommand() throws SearchException
   {
      TrcQueryBuilder qb = new TrcQueryBuilder(solrServer, new ExtractSolrConfig());
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
      switch (evt.getUpdateAction())
      {
         case CREATE:
            onCreate(evt);
            break;
         case UPDATE:
            onUpdate(evt);
            break;
         case DELETE:
            onDelete(evt);
            break;
      }
   }

   /**
    * Called when a new extract is added to the database
    *
    * @param evt
    */
   protected void onCreate(UpdateEvent evt)
   {
      // these should create a task and delegate, monitor task queue and commit after
      // timeout or fixed number of updates
      Objects.requireNonNull(solrDocFactory, "Solr document factory is not available.");
      String id = evt.getEntityId();
      try {
         DramaticExtract extract = extractRepo.get(id);

         solrServer.add(solrDocFactory.create(extract));
         solrServer.commit();
      }
      catch (ExtractNotAvailableException e) {
         logger.log(Level.SEVERE, "Failed to retrieve seemingly non-existant extract [" + id + "] on create event.", e);
      }
      catch (DramaticExtractException e) {
         logger.log(Level.SEVERE, "Failed to retrieve extract [" + id + "] on create event.", e);
      }
      catch (SolrServerException | IOException e)
      {
         logger.log(Level.SEVERE, "Failed to commit extract [" + id + "] to the Solr server.", e);
      }
   }

   /**
    * Called when an extract is updated in the database
    *
    * @param evt
    */
   protected void onUpdate(UpdateEvent evt)
   {
      onCreate(evt);
   }

   /**
    * Called when an extract is removed from the database
    * @param evt
    */
   protected void onDelete(UpdateEvent evt)
   {
      String id = evt.getEntityId();

      try
      {
         solrServer.deleteById(id);
         solrServer.commit();
      }
      catch (SolrServerException | IOException e)
      {
         logger.log(Level.SEVERE, "Failed to remove extract [" + id + "] from the Solr server.", e);
      }
   }

}
