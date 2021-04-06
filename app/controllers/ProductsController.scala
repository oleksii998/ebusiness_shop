package controllers

import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request}

import javax.inject.{Inject, Singleton}

@Singleton
class ProductsController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def addProduct() = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def getProduct(id: Long) = Action {
    NoContent
  }

  def modifyProduct(id: Long) = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def removeProduct(id: Long) = Action {
    NoContent
  }
}
