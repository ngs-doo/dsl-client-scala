package com.dslplatform.api.client

import com.dslplatform.api.patterns.DomainEvent

abstract class EventCompanion[TEvent <: DomainEvent: scala.reflect.ClassTag]
    extends IdentifiableCompanion[TEvent] {
  //TODO missing helper methods
}
