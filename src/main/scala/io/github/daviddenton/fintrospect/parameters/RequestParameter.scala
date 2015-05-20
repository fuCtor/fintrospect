package io.github.daviddenton.fintrospect.parameters

import org.jboss.netty.handler.codec.http.HttpRequest

import scala.util.Try


abstract class RequestParameter[T](override val requirement: Requirement, location: Location, parse: (String => Try[T])) extends Parameter[T] {
  override val where = location.toString

  def unapply(request: HttpRequest): Option[T] = location.from(name, request).flatMap(parse(_).toOption)
}
