package edu.tamu.tcat.trc.extract.search;

import edu.tamu.tcat.trc.entries.search.SearchException;

/**
 * Command for use in querying the associated {@link ExtractSearchService} which provides instances.
 *
 * An {@link ExtractQueryCommand} is intended to be initialized, executed a single time, provide
 * results, and be discarded.
 *
 * The various "query" methods are intended to be for user-entered criteria which results in "like",
 * wildcard, or otherwise interpreted query criteria which may apply to multiple fields of the index.
 * Alternatively, the various "filter" methods are intended for specific criteria which typically
 * applies to faceted searching or to known criteria for specific stored data.
 *
 * This interface is based on edu.tamu.tcat.trc.entries.types.bib.search.WorkQueryCommand
 */
public interface ExtractQueryCommand
{
   /**
    * Execute this query command after it has been parameterized. The query itself contains all
    * parameters and refinement criteria, and the result is simply a listing of matches.
    *
    * @return
    * @throws SearchException
    */
   SearchExtractResult execute() throws SearchException;

   /**
    * Supply a "basic" free-text, keyword query to be executed. In general, the supplied query will
    * be executed against a wide range of fields with different fields being assigned different
    * levels of boosting (per-field weights). The specific fields to be searched and the relative
    * weights associated with different fields is implementation-dependent.
    *
    * @param basicQueryString The "basic" query string. May be {@code null} or empty.
    * @throws SearchException
    */
   void query(String basicQueryString) throws SearchException;

   // TODO: add "query" and "filter" methods

   /**
    * Sets the index offset of the first result to be returned.
    *
    * Useful in conjunction with {@link ExtractQueryCommand#setMaxResults(int)} to support result
    * paging.
    *
    * Note that implementations are <em>strongly</em> encouraged to make a best-effort attempt to
    * preserve result order across multiple invocations of the same query. In general, this is a
    * challenging problem in the face of updates to the underlying index and implementations that
    * are not required to guarantee result order consistency across multiple calls.
    *
    * @param offset
    */
   void setOffset(int offset);

   /**
    * Specify the maximum number of results to be returned. Implementations may return fewer results
    * but must not return more.
    *
    * If not specified, the default is 25.
    *
    * @param count
    */
   void setMaxResults(int count);
}
