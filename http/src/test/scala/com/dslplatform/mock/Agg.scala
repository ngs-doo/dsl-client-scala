package com.dslplatform.mock

import com.dslplatform.api.client._
import com.dslplatform.api.patterns._

class Agg extends AggregateRoot {
  def URI = "uri_value"
}

object Agg extends AggregateRootCompanion[Agg]{}
