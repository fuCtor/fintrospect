package io.fintrospect.formats.json

import java.math.BigInteger

import spray.json.DefaultJsonProtocol._
import spray.json._

/**
 * Argo JSON support.
 */
object SprayJson extends JsonLibrary[JsValue, JsValue] {

 object JsonFormat extends JsonFormat[JsValue, JsValue] {

    override def parse(in: String) = JsonParser(in)

    override def pretty(node: JsValue): String = node.prettyPrint

    override def compact(node: JsValue): String = node.compactPrint

    override def obj(fields: Iterable[Field]) = JsObject(fields.toList: _*)

    override def obj(fields: Field*) = this.obj(fields.toSeq)

    override def array(elements: Iterable[JsValue]) = elements.toJson

    override def array(elements: JsValue*) = elements.toJson

    override def string(value: String) = value.toJson

    override def number(value: Int) = value.toJson

    override def number(value: BigDecimal) = value.toJson

    override def number(value: Long) = value.toJson

    override def number(value: BigInteger) = value.intValue().toJson

    override def boolean(value: Boolean) = value.toJson

    override def nullNode() = JsNull
  }

}