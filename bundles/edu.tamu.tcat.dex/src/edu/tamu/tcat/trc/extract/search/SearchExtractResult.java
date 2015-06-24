package edu.tamu.tcat.trc.extract.search;

import java.util.Collection;
import java.util.List;

import edu.tamu.tcat.trc.extract.search.solr.ExtractSearchProxy;

/**
 * The result set of {@link ExtractSearchProxy}'s matched by an {@link ExtractQueryCommand}.
 *
 * A result set has no functionality other than retrieving matched results from an executed query.
 * It should be considered "stale" as soon as it is acquired due to the inherently unstable nature
 * of a search framework.
 *
 * This interface is based on edu.tamu.tcat.trc.entries.types.bib.search.SearchWorksResult
 */
public interface SearchExtractResult
{
   /**
    * @return the {@link ExtractQueryCommand} which executed to provide this result.
    */
   ExtractQueryCommand getCommand();

   /**
    * @return The extracts that match the current search.
    */
   List<ExtractSearchProxy> get();

   /**
    * Only the results that fall within the current window [offset .. offset + maxResults]
    * are included in this data structure. This value allows clients to compute pagination values.
    *
    * @return the total number of results.
    */
   long getNumFound();

   /**
    * Facets allow the user to refine search results by filtering according to the extracted values
    * of certain fields. Each {@link FacetItemList} corresponds to a single field, and the items
    * therein to the values.
    *
    * @return Facet definitions for each defined facet field.
    */
   Collection<FacetItemList> getFacets();
}
