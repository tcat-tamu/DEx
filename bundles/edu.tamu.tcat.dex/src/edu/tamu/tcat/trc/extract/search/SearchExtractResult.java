package edu.tamu.tcat.trc.extract.search;

import java.util.List;

import edu.tamu.tcat.dex.trc.entry.DramaticExtract;

/**
 * The result set of {@link DramaticExtract}s matched by an {@link ExtractQueryCommand}.
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
   List<DramaticExtract> get();

   // TODO: add support for retrieving facet information
}
