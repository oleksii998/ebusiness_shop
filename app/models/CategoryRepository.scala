package models

import controllers.{CreateCategoryForm, ModifyCategoryForm, ModifyProductForm}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@Singleton
class CategoryRepository @Inject()(databaseConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {
  private val databaseConfig = databaseConfigProvider.get[JdbcProfile]
  private val categories = TableQuery[CategoryTable]

  import databaseConfig._
  import databaseConfig.profile.api._

  def add(createCategoryForm: CreateCategoryForm): Future[Try[Category]] = db.run {
    ((categories returning categories.map(_.id) into (
      (category, id) => category.copy(id = id)
      )) += Category(0, createCategoryForm.name,
      createCategoryForm.description,
      active = true)).asTry
  }

  def getAll: Future[Seq[Category]] = db.run {
    categories.filter(_.active).result
  }

  def get(id: Long): Future[Option[Category]] = db.run {
    categories.filter(category => category.id === id && category.active).result.headOption
  }

  def remove(id: Long): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(product) =>
        val replacement = product.copy(active = false)
        db.run(categories.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Product not found")))
    }
  }

  def modify(id: Long, modifyCategoryForm: ModifyCategoryForm): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(category) =>
        var name = category.name
        if (modifyCategoryForm.name.isDefined) {
          name = modifyCategoryForm.name.get
        }
        var description = category.description
        if (modifyCategoryForm.description.isDefined) {
          description = modifyCategoryForm.description.get
        }
        val replacement = category.copy(name = name, description = description)
        db.run(categories.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Product not found")))
    }
  }
}