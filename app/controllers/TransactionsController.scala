package controllers

import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request}

import javax.inject.{Inject, Singleton}

@Singleton
class TransactionsController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def addTransaction() = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def getTransaction(id: Long) = Action {
    NoContent
  }

  def modifyTransaction(id: Long) = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def removeTransaction(id: Long) = Action {
    NoContent
  }
}
