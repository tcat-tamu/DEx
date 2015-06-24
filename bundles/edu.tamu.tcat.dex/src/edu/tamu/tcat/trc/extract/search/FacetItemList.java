package edu.tamu.tcat.trc.extract.search;

import java.util.Iterator;
import java.util.List;

import edu.tamu.tcat.trc.extract.search.FacetItemList.FacetItem;

/**
 * Provides an API to access the faceted values of a search field.
 */
public interface FacetItemList extends Iterable<FacetItem>
{
   /**
    * Represents the label and count of a facet line item.
    */
   public static interface FacetItem
   {
      /**
       * @return the label
       */
      String getLabel();

      // TODO The label may eventually be considered a human-readable value. The underlying facet
      //      value (e.g. an ID) might be retrieved using a separate getValue() method.

      // TODO There are no use cases for this in the DEx project, but what about range facets like numbers and dates?

      /**
       * @return a count of the number of result documents that contain the label value.
       */
      long getCount();
   }

   /**
    * @return the name of the field represented by this facet
    */
   String getFieldName();

   /**
    * Members of this facet are ranked by count or alphabetically by label.
    * This ordering can be set at query time.
    *
    * @return The label/count pairs within this facet.
    */
   List<FacetItem> getValues();

   @Override
   default Iterator<FacetItem> iterator()
   {
      return getValues().iterator();
   }
}
