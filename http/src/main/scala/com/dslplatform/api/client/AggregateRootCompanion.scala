package com.dslplatform.api.client

import com.dslplatform.api.patterns.AggregateRoot
import com.dslplatform.api.patterns.ServiceLocator
import com.dslplatform.api.patterns.Specification

abstract class AggregateRootCompanion[TRoot <: AggregateRoot: scala.reflect.ClassTag]
    extends IdentifiableCompanion[TRoot] {
  //TODO missing helper methods
}
