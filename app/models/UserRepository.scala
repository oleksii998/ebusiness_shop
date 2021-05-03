package models

import controllers.{CreateCustomerForm, CreateUserForm, ModifyCustomerForm, ModifyUserForm}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@Singleton
class UserRepository @Inject()(databaseConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {
  private val databaseConfig = databaseConfigProvider.get[JdbcProfile]
  private val users = TableQuery[UserTable]

  import databaseConfig._
  import databaseConfig.profile.api._

  def add(createUserForm: CreateUserForm): Future[Try[User]] = db.run {
    ((users returning users.map(_.id) into (
      (user, id) => user.copy(id = id)
      )) += User(0, createUserForm.email,
      createUserForm.password,
      createUserForm.firstName,
      createUserForm.lastName,
      active = true)).asTry
  }

  def getAll: Future[Seq[User]] = db.run {
    users.filter(_.active).result
  }

  def get(id: Long): Future[Option[User]] = db.run {
    users.filter(user => user.id === id && user.active).result.headOption
  }

  def remove(id: Long): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(user) =>
        val replacement = user.copy(active = false)
        db.run(users.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Customer not found")))
    }
  }

  def modify(id: Long, modifyUserForm: ModifyUserForm): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(user) =>
        var email = user.email
        if (modifyUserForm.email.isDefined) {
          email = modifyUserForm.email.get
        }
        var password = user.password
        if (modifyUserForm.password.isDefined) {
          password = modifyUserForm.password.get
        }
        var firstName = user.firstName
        if (modifyUserForm.firstName.isDefined) {
          firstName = modifyUserForm.firstName.get
        }
        var lastName = user.lastName
        if (modifyUserForm.lastName.isDefined) {
          lastName = modifyUserForm.lastName.get
        }
        val replacement = user.copy(email = email, password = password, firstName = firstName, lastName = lastName)
        db.run(users.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Customer not found")))
    }
  }
}
