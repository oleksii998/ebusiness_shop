package controllers

import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request}

import javax.inject.{Inject, Singleton}

@Singleton
class BonusCardsController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def addBonusCard() = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def getBonusCard(id: Long) = Action {
    NoContent
  }

  def modifyBonusCard(id: Long) = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def removeBonusCard(id: Long) = Action {
    NoContent
  }
}
