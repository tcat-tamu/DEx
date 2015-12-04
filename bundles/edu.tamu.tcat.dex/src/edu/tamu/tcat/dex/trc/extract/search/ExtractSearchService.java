package edu.tamu.tcat.dex.trc.extract.search;

import edu.tamu.tcat.dex.trc.entry.ExtractRepository;
import edu.tamu.tcat.trc.search.SearchException;

/**
 * The main API for searching a {@link ExtractRepository}.
 *
 * This interface is based on edu.tamu.tcat.trc.entries.types.bib.WorkSearchService
 */
public interface ExtractSearchService
{
   /**
    * Create a new command for searching extracts. The returned {@link ExtractQueryCommand} may be
    * parameterized according to the search criteria and executed to run the search against
    * this service and the {@link ExtractRepository} backing it.
    *
    * @throws SearchException
    */
   ExtractQueryCommand createQueryCommand() throws SearchException;
}
