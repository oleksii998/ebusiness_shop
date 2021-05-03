package controllers

import models.PromotionType.PromotionType
import models.{PromotionRepository, PromotionType}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats.doubleFormat
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

@Singleton
class PromotionsController @Inject()(promotionRepository: PromotionRepository, val controllerComponents: ControllerComponents) extends BaseController {

  val createPromotionForm: Form[CreatePromotionForm] = Form {
    mapping(
      "productId" -> longNumber,
      "discount" -> of[Double],
      "type" -> number.verifying("Error.invalidType", promotionType =>
        promotionType.equals(PromotionType.PERCENTAGE) ||
          promotionType.equals(PromotionType.CONSTANT)),
    )(CreatePromotionForm.apply)(CreatePromotionForm.unapply)
  }

  val modifyPromotionForm: Form[ModifyPromotionForm] = Form {
    mapping(
      "productId" -> optional(longNumber),
      "discount" -> optional(of[Double]),
      "type" -> optional(number.verifying("Error.invalidType", promotionType =>
        promotionType.equals(PromotionType.PERCENTAGE) ||
          promotionType.equals(PromotionType.CONSTANT))),
    )(ModifyPromotionForm.apply)(ModifyPromotionForm.unapply)
  }

  def addPromotion(): Action[AnyContent] = Action.async { implicit request =>
    createPromotionForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      promotion => {
        promotionRepository.add(promotion)
          .map {
            case Success(promotion) => Ok(Json.toJson(promotion))
            case Failure(exception) => BadRequest(Json.toJson(exception.getMessage))
          }
      }
    )
  }

  def getPromotion(id: Long): Action[AnyContent] = Action.async { implicit request =>
    promotionRepository.get(id).map {
      case Some(promotion) => Ok(Json.toJson(promotion))
      case None => Ok("{\"error\":\"not found\"}")
    }
  }

  def getAllPromotions: Action[AnyContent] = Action.async { implicit request =>
    promotionRepository.getAll.map(promotions => Ok(Json.toJson(promotions)))
  }

  def modifyPromotion(id: Long): Action[AnyContent] = Action.async { implicit request =>
    modifyPromotionForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      promotion => {
        promotionRepository.modify(id, promotion)
          .flatMap(result => result.map {
            case Success(_) => Ok("{\"result\":\"updated\"}")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removePromotion(id: Long): Action[AnyContent] = Action.async { implicit request =>
    promotionRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Ok("{\"result\":\"removed\"}")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }
}

case class CreatePromotionForm(productId: Long, discount: Double, promotionType: PromotionType)
case class ModifyPromotionForm(productId: Option[Long], discount: Option[Double], promotionType: Option[PromotionType])

