package edu.tamu.tcat.trc.extract.postgres;

import edu.tamu.tcat.trc.entries.notification.BasicUpdateEvent;
import edu.tamu.tcat.trc.entries.notification.UpdateEvent;
import edu.tamu.tcat.trc.entries.notification.UpdateEvent.UpdateAction;

/**
 * Provides a common utility for creating {@link UpdateEvent}s
 *
 * @todo This class may be refactored into edu.tamu.tcat.trc.entries.notification
 *
 * @param <T> The type of object that will be referenced in the constructed {@link UpdateEvent} instances
 */
public class BasicUpdateEventFactory<T>
{
   /**
    * Creates an update event to notify listeners of an object's creation.
    *
    * @param id
    * @param entity
    * @return
    */
   public UpdateEvent<T> makeCreateEvent(String id, T entity)
   {
      return new BasicUpdateEvent<>(id, UpdateAction.CREATE, () -> null, () -> entity);
   }

   /**
    * Creates an update event to notify listeners of an object's modification.
    *
    * @param id
    * @param original The original entity prior to any updates.
    * @param updated The entity in its current, updated state.
    * @return
    */
   public UpdateEvent<T> makeUpdateEvent(String id, T original, T updated)
   {
      return new BasicUpdateEvent<>(id, UpdateAction.UPDATE, () -> original, () -> updated);
   }

   /**
    * Creates an update event to notify listeners of an object's deletion.
    *
    * @param id
    * @param entity The entity that was deleted. This parameter may be null.
    * @return
    */
   public UpdateEvent<T> makeDeleteEvent(String id, T entity)
   {
      return new BasicUpdateEvent<>(id, UpdateAction.DELETE, () -> entity, () -> null);
   }
}
