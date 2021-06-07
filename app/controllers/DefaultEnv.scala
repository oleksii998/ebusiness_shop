package controllers

import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.UserEntry

trait DefaultEnv extends Env {
  type I = UserEntry
  type A = CookieAuthenticator
}
