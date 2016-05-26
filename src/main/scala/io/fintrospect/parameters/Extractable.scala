package io.fintrospect.parameters

/**
  * Mechanism to extract (or fail to extract) an entity value from a particular 'From' context
  */
trait Extractable[-From, +T] {
  /**
    * Attempt to manually deserialise from the message object.
    */
  def <--?(from: From): Extraction[T]

  /**
    * Attempt to manually deserialise from the message object.
    * User-friendly synonym for <--?(), which is why the method is final.
    */
  final def extract(from: From): Extraction[T] = <--?(from)
}

object Extractable {
  /**
    * Create an extractable from a simple function. This is stylistic, similar to Service.mk and Filter.mk
    */
  def mk[From, T](fn:From => Extraction[T]): Extractable[From, T] = new Extractable[From, T] {
    override def <--?(from: From): Extraction[T] = fn(from)
  }
}

/**
  * Mechanism to extract (or fail to extract) a parameter from a particular 'From' context, adding
  */
trait ExtractableParameter[-From, +T] {
  self: Parameter with Extractable[From, T] =>

  /**
    * Attempt to manually deserialise from the message object, using a validation predicate and reason for failure.
    */
  def <--?(from: From, reason: String, predicate: T => Boolean): Extraction[T] =
    <--?(from).flatMap[T](v => if (v.map(predicate).getOrElse(true)) Extraction(v) else ExtractionFailed(InvalidParameter(this, reason)))

  /**
    * Attempt to manually deserialise from the message object, using a validation predicate and reason for failure.
    * User-friendly synonym for <--?(), which is why the method is final.
    */
  final def extract(from: From, reason: String, predicate: T => Boolean): Extraction[T] = <--?(from, reason, predicate)
}