package edu.tamu.tcat.trc.extract.postgres;

import java.time.Instant;
import java.util.UUID;

import edu.tamu.tcat.trc.entries.notification.BaseUpdateEvent;
import edu.tamu.tcat.trc.entries.notification.UpdateEvent;
import edu.tamu.tcat.trc.entries.notification.UpdateEvent.UpdateAction;

/**
 * Provides a common utility for creating {@link UpdateEvent}s
 *
 * @todo This class may be refactored into edu.tamu.tcat.trc.entries.notification
 *
 * @param <T> The type of object that will be referenced in the constructed {@link UpdateEvent} instances
 */
public class BaseUpdateEventFactory
{
   /**
    * Creates an update event to notify listeners of an object's creation.
    *
    * @param id
    * @return
    */
   public UpdateEvent makeCreateEvent(String id, UUID actor)
   {
      return new BaseUpdateEvent(id, UpdateAction.CREATE, actor, Instant.now());
   }

   /**
    * Creates an update event to notify listeners of an object's modification.
    *
    * @param id
    * @return
    */
   public UpdateEvent makeUpdateEvent(String id, UUID actor)
   {
      return new BaseUpdateEvent(id, UpdateAction.UPDATE, actor, Instant.now());
   }

   /**
    * Creates an update event to notify listeners of an object's deletion.
    *
    * @param id
    * @return
    */
   public UpdateEvent makeDeleteEvent(String id, UUID actor)
   {
      return new BaseUpdateEvent(id, UpdateAction.DELETE, actor, Instant.now());
   }
}
