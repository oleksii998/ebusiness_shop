package controllers

import models.PromotionType.PromotionType
import models.{ProductRepository, PromotionRepository, PromotionType}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats.doubleFormat
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, MessagesAbstractController, MessagesControllerComponents, MessagesRequest}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class PromotionsController @Inject()(promotionRepository: PromotionRepository,
                                     productRepository: ProductRepository,
                                     scc: DefaultSilhouetteControllerComponents)(implicit val ec: ExecutionContext) extends SilhouetteController(scc) {

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

  def addPromotion(): Action[AnyContent] = securedAction.async { implicit request =>
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
      case None => NotFound("{\"error\":\"Promotion not found\"}")
    }
  }

  def getAllPromotions: Action[AnyContent] = Action.async { implicit request =>
    promotionRepository.getAll.map(promotions => Ok(Json.toJson(promotions)))
  }

  def modifyPromotion(id: Long): Action[AnyContent] = securedAction.async { implicit request =>
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

  def removePromotion(id: Long): Action[AnyContent] = securedAction.async { implicit request =>
    promotionRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Ok("{\"result\":\"removed\"}")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }

  //VIEWS

  def addPromotionView(): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
    productRepository.getAll.map(products => Ok(views.html.promotions.promotionAdd(createPromotionForm, products.map(_.product))))
  }

  def addPromotionViewResponse(): Action[AnyContent] = Action.async { implicit request =>
    createPromotionForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      transaction => {
        promotionRepository.add(transaction)
          .map {
            case Success(_) => Redirect(routes.PromotionsController.addPromotionView()).flashing("success" -> "Promotion updated")
            case Failure(exception) => BadRequest(exception.getMessage)
          }
      }
    )
  }

  def getPromotionView(id: Long): Action[AnyContent] = Action.async {
    promotionRepository.get(id).map {
      case Some(promotion) => Ok(views.html.promotions.promotion(promotion))
      case None => BadRequest("Promotion not found")
    }
  }

  def getAllPromotionsView: Action[AnyContent] = Action.async {
    promotionRepository.getAll.map(promotions => Ok(views.html.promotions.promotions(promotions)))
  }

  def modifyPromotionView(id: Long): Action[AnyContent] = Action.async { implicit request =>
    promotionRepository.get(id).map {
      case Some(productPromotion) =>
        val products = Await.result(productRepository.getAll, Duration.Inf).map(_.product)
        val filled = modifyPromotionForm.fill(ModifyPromotionForm(Option.apply(productPromotion.product.id),
          Option.apply(productPromotion.promotion.discount), Option.apply(productPromotion.promotion.promotionType)))
        Ok(views.html.promotions.promotionModify(filled, productPromotion.promotion.id, products))
      case None => BadRequest("Promotion not found")
    }
  }

  def modifyPromotionViewResponse(id: Long): Action[AnyContent] = Action.async { implicit request =>
    modifyPromotionForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      promotion => {
        promotionRepository.modify(id, promotion)
          .flatMap(result => result.map {
            case Success(_) => Redirect(routes.PromotionsController.modifyPromotionView(id)).flashing("success" -> "Promotion updated")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removePromotionView(id: Long): Action[AnyContent] = Action.async {
    promotionRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Redirect("/promotionsView")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }
}

case class CreatePromotionForm(productId: Long, discount: Double, promotionType: PromotionType)
case class ModifyPromotionForm(productId: Option[Long], discount: Option[Double], promotionType: Option[PromotionType])

