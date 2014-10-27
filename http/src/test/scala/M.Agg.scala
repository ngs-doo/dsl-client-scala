package M

import com.dslplatform.api.patterns._
import com.dslplatform.api.client._

class Agg extends AggregateRoot {
  def URI = "uri_value"
}

object Agg extends AggregateRootCompanion[Agg] {}
