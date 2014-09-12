package M

import com.dslplatform.api.patterns._
import com.dslplatform.api.client._
import scala.concurrent.{Await, Future, ExecutionContext}
import scala.concurrent.duration.Duration




class Agg @com.fasterxml.jackson.annotation.JsonIgnore()  private(
	  private var _URI: String,
	  @transient private var __locator: Option[ServiceLocator],
	  private var _ID: Int,
	  private var _s: String,
	  private var _i: Int
	) extends Serializable with AggregateRoot {
	
	
	
	
	@com.fasterxml.jackson.annotation.JsonProperty("URI")
	def URI = { 
		_URI
	}

	
	private [M] def URI_= (value: String) { 
		_URI = value
		
	}

	
	override def hashCode = URI.hashCode
	override def equals(o: Any) = o match {
		case c: Agg => c.URI == URI
		case _ => false
	}

	override def toString = "Agg("+ URI +")"
	
		
	 def copy(s: String = this._s, i: Int = this._i): Agg = {
		

			
	require(s ne null, "Null value was provided for property \"s\"")
		new Agg(_URI = this.URI, __locator = this.__locator, _ID = _ID, _s = s, _i = i)
	}

	
	
	@com.fasterxml.jackson.annotation.JsonProperty("ID")
	def ID = { 
		_ID
	}

	
	private [M] def ID_= (value: Int) { 
		_ID = value
		
	}

	
	@com.fasterxml.jackson.annotation.JsonIgnore
	def isNewAggregate() = __locator == None || _URI == null
	

	private def updateWithAnother(result: M.Agg): this.type = {
		this._URI = result._URI
		this._s = result._s
		this._i = result._i
		this._ID = result._ID
		this
	}

	private def create()(implicit locator: ServiceLocator, ec: ExecutionContext, duration: Duration): this.type = {
		__locator = Some(if (locator ne null) locator else Bootstrap.getLocator)
		val toUpdateWith = Await.result(__locator.get.resolve(classOf[CrudProxy]).create(this), duration)
		updateWithAnother(toUpdateWith)

	}

	private def update()(implicit ec: ExecutionContext, duration: Duration): this.type = {
		val toUpdateWith = Await.result(__locator.get.resolve(classOf[CrudProxy]).update(this), duration)
		updateWithAnother(toUpdateWith)

	}

	private def delete()(implicit ec: ExecutionContext, duration: Duration) = {
		if (__locator.isEmpty) throw new IllegalArgumentException("Can't delete an aggregate before it's been saved")
		Await.result(__locator.get.resolve(classOf[CrudProxy]).delete[M.Agg](URI), duration)
	}


	
	
	@com.fasterxml.jackson.annotation.JsonProperty("s")
	def s = { 
		_s
	}

	
	def s_= (value: String) { 
		_s = value
		
	}

	
	
	@com.fasterxml.jackson.annotation.JsonProperty("i")
	def i = { 
		_i
	}

	
	def i_= (value: Int) { 
		_i = value
		
	}

	
	@com.fasterxml.jackson.annotation.JsonCreator private def this(
		@com.fasterxml.jackson.annotation.JacksonInject("__locator") __locator: ServiceLocator
	, @com.fasterxml.jackson.annotation.JsonProperty("URI") URI: String
	, @com.fasterxml.jackson.annotation.JsonProperty("ID") ID: Int
	, @com.fasterxml.jackson.annotation.JsonProperty("s") s: String
	, @com.fasterxml.jackson.annotation.JsonProperty("i") i: Int
	) =
	  this(__locator = Some(__locator), _URI = URI, _ID = ID, _s = if (s == null) "" else s, _i = i)

}

object Agg extends AggregateRootCompanion[Agg]{

	def apply(
		s: String = ""
	, i: Int = 0
	) = {
		require(s ne null, "Null value was provided for property \"s\"")
		new Agg(
			__locator = None
		, _URI = java.util.UUID.randomUUID.toString
		, _ID = 0
		, _s = s
		, _i = i)
	}

	
}
