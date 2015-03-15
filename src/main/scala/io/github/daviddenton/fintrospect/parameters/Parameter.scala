package io.github.daviddenton.fintrospect.parameters

import java.beans.Introspector._

import scala.reflect.ClassTag

abstract class Parameter[T] protected[fintrospect](val name: String, val description: Option[String], val where: String, val required: Requirement)(implicit ct: ClassTag[T]) {
  val paramType = decapitalize(ct.runtimeClass.getSimpleName)
  def unapply(str: String): Option[T]
}



