package io.fintrospect

import com.twitter.finagle.Service
import com.twitter.finagle.http.path.{Path, Root}
import com.twitter.finagle.http.{Request, Response}
import io.fintrospect.renderers.swagger2dot0.{ApiInfo, Swagger2dot0Json}


import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.{currentMirror, universe}


/**
  * Marker interface which can be used to organise sets of ContactEndpoints, and also to create a ContractProxyModules.
  */
trait Contract

/**
  * A single Contract endpoint which provides an UnboundRoute to bind something to.
  */
trait ContractEndpoint {
  val route: UnboundRoute
}

/**
  * Creates a standard Module which acts as a proxy to the set of routes declared in the passed Contract.
  */
object ContractProxyModule {
  def apply[T <: Contract](name: String, service: Service[Request, Response], contract: T, rootPath: Path = Root, description: String = null)(implicit tag: TypeTag[T]): RouteModule[Request, Response] = {
    val descriptionOption = Option(description).getOrElse(s"Proxy services for $name API")
    val routes = universe.typeOf[T].members
      .filter(_.isModule)
      .map(_.asModule)
      .map(currentMirror.reflectModule(_).instance)
      .filter(_.isInstanceOf[ContractEndpoint])
      .map(_.asInstanceOf[ContractEndpoint].route)

    routes.foldLeft(RouteModule(rootPath, Swagger2dot0Json(ApiInfo(name, name, descriptionOption)))) {
      (spec, route) => spec.withRoute(route.bindToProxy(service))
    }
  }
}
