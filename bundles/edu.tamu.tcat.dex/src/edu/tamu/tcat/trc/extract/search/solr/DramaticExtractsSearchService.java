package edu.tamu.tcat.trc.extract.search.solr;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

import edu.tamu.tcat.dex.trc.entry.DramaticExtract;
import edu.tamu.tcat.dex.trc.entry.ExtractRepository;
import edu.tamu.tcat.dex.trc.entry.tei.transform.ExtractManipulationUtil;
import edu.tamu.tcat.osgi.config.ConfigurationProperties;
import edu.tamu.tcat.trc.entries.notification.UpdateEvent;
import edu.tamu.tcat.trc.entries.search.SearchException;
import edu.tamu.tcat.trc.extract.search.ExtractQueryCommand;
import edu.tamu.tcat.trc.extract.search.ExtractSearchService;

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

   private ExtractRepository repo;
   private ExtractManipulationUtil extractManipulationUtil;
   private ConfigurationProperties config;
   private SolrServer solrServer;
   private AutoCloseable repoListenerRegistration;


   public void setRepo(ExtractRepository repo)
   {
      this.repo = repo;
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
      Objects.requireNonNull(repo, "No extracts repository supplied.");
      Objects.requireNonNull(config, "No configuration supplied.");
      Objects.requireNonNull(extractManipulationUtil, "No extract manipulation utility provided.");

      // listen for updates from the repository
      repoListenerRegistration = repo.register(this::handleUpdateEvent);

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

      repo = null;
      config = null;
      extractManipulationUtil = null;
      repoListenerRegistration = null;
   }

   @Override
   public ExtractQueryCommand createQueryCommand() throws SearchException
   {
      return null;
   }

   /**
    * Dispatch handler for all database events
    *
    * @param evt
    */
   private void handleUpdateEvent(UpdateEvent<DramaticExtract> evt)
   {
      switch (evt.getAction())
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
   protected void onCreate(UpdateEvent<DramaticExtract> evt)
   {
      DramaticExtract extract = evt.get();

      try
      {
         ExtractDocument extractDocument = ExtractDocument.create(extract, extractManipulationUtil);
         SolrInputDocument solrDocument = extractDocument.getDocument();
         solrServer.add(solrDocument);
         solrServer.commit();
      }
      catch (SearchException e)
      {
         logger.log(Level.SEVERE, "Failed to create extract search document.", e);
      }
      catch (SolrServerException | IOException e)
      {
         logger.log(Level.SEVERE, "Failed to commit extract [" + extract.getId() + "] to the Solr server.", e);
      }
   }

   /**
    * Called when an extract is updated in the database
    *
    * @param evt
    */
   protected void onUpdate(UpdateEvent<DramaticExtract> evt)
   {
      onCreate(evt);
   }

   /**
    * Called when an extract is removed from the database
    * @param evt
    */
   protected void onDelete(UpdateEvent<DramaticExtract> evt)
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
