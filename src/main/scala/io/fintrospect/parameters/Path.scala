package io.fintrospect.parameters

import java.net.URI

import org.jboss.netty.handler.codec.http.HttpRequest

import scala.util.Try

/**
 * Builder for parameters that are encoded in the HTTP request path.
 */
object Path extends Parameters[PathParameter, Mandatory] {

  /**
   * A special path segment that is defined, but has no intrinsic value other than for route matching. Useful when embedded
   * between 2 other path parameters. eg. /myRoute/{id}/aFixedPart/{subId}
   */
  def fixed(aName: String): PathParameter[String] = new PathParameter[String](aName, None, StringParamType) with Mandatory[String] {
    override def toString() = name

    override def unapply(str: String): Option[String] = if (str == name) Some(str) else None

    override def iterator: Iterator[PathParameter[_]] = Nil.iterator

    override def parseFrom(request: HttpRequest): Option[Try[String]] = ???
  }

  override protected def builder(): ParameterBuilder[PathParameter, Mandatory] = new ParameterBuilder[PathParameter, Mandatory]() {
    override def apply[T](name: String,
                          description: Option[String],
                          paramType: ParamType,
                          parse: (String => Try[T]))
    = new PathParameter[T](name, description, paramType) with Mandatory[T] {

      override def toString() = s"{$name}"

      override def unapply(str: String): Option[T] = Option(str).flatMap(s => {
        parse(new URI("http://localhost/" + s).getPath.substring(1)).toOption
      })

      override def iterator: Iterator[PathParameter[_]] = Some(this).iterator

      override def parseFrom(request: HttpRequest): Option[Try[T]] = ???
    }
  }
}
