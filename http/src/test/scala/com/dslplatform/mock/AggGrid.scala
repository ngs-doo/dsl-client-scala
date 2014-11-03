package com.dslplatform.mock

case class AggGrid(val URI: String = "uri_value") extends com.dslplatform.api.patterns.Snowflake[Agg] {
}

object AggGrid extends com.dslplatform.api.client.SnowflakeCompanion[AggGrid, Agg]{
}
