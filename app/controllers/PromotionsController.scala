package controllers

import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request}

import javax.inject.{Inject, Singleton}

@Singleton
class PromotionsController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def addPromotion() = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def getPromotion(id: Long) = Action {
    NoContent
  }

  def modifyPromotion(id: Long) = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def removePromotion(id: Long) = Action {
    NoContent
  }
}
