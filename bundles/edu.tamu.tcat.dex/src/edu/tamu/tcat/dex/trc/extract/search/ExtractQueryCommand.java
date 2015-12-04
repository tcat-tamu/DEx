package edu.tamu.tcat.dex.trc.extract.search;

import java.util.Collection;

import edu.tamu.tcat.trc.search.SearchException;


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
    * @param basicQueryString The "basic" query string. If empty, a catch-all query will be used.
    * @throws SearchException
    */
   void query(String basicQueryString) throws SearchException;

   /**
    * Supply an "advanced" keyword query to be executed. This is to be used in conjunction with
    * {@code queryManuscript}, {@code queryPlaywright}, {@code queryPlay}, etc. and should therefore
    * exclude those fields from the query fields.
    *
    * @param advancedQuery
    * @throws SearchException
    */
   void advancedQuery(String advancedQueryString) throws SearchException;

   /**
    * Include results whose manuscript title matches the supplied query.
    * This is used for advanced search.
    *
    * @param manuscriptQuery
    * @throws SearchException
    */
   void queryManuscript(String manuscriptQuery) throws SearchException;

   /**
    * Limit results to only those associated with one or more of the supplied manuscripts.
    * This is used for faceting.
    *
    * @param manuscriptIds
    * @throws SearchException
    */
   void filterManuscript(Collection<String> manuscriptIds) throws SearchException;

   /**
    * Include results whose playwright name matches the supplied query.
    * This is used for advanced search.
    *
    * @param playwrightQuery
    * @throws SearchException
    */
   void queryPlaywright(String playwrightQuery) throws SearchException;

   /**
    * Limit results to only those associated with one or more of the supplied playwrights.
    * This is used for faceting.
    *
    * @param playwrightIds
    * @throws SearchException
    */
   void filterPlaywright(Collection<String> playwrightIds) throws SearchException;

   /**
    * Include results whose play title matches the supplied query.
    * This is used for advanced search.
    *
    * @param playQuery
    * @throws SearchException
    */
   void queryPlay(String playQuery) throws SearchException;

   /**
    * Limit results to only those associated with one or more of the supplied plays.
    * This is used for faceting.
    *
    * @param playIds
    * @throws SearchException
    */
   void filterPlay(Collection<String> playIds) throws SearchException;

   /**
    * Include results whose speaker name matches the supplied query.
    * This is used for advanced search.
    *
    * @param speakerQuery
    * @throws SearchException
    */
   void querySpeaker(String speakerQuery) throws SearchException;

   /**
    * Limit results to only those associated with one or more of the supplied speakers.
    * This is used for faceting.
    *
    * @param speakerIds
    * @throws SearchException
    */
   void filterSpeaker(Collection<String> speakerIds) throws SearchException;

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

   /**
    * Specify the maximum number of facet items to be included in the results. Implementations may
    * return fewer results but must not return more.
    *
    * If not specified, the default is 10.
    *
    * @param count
    */
   void setMaxFacets(int count);

}
