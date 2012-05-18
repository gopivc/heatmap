package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent._
import net.liftweb.json._
import lib.Backend
import org.joda.time.DateTime

object Api extends Controller {
  implicit val formats = Serialization.formats(NoTypeHints) ++ ext.JodaTimeSerializers.all


  def withCallback(callback: Option[String])(block: => String) = {
    Ok(callback map { _ + "(" + block + ")" } getOrElse block).as("application/javascript")
  }

  case class LinkCount(sel: String, hash: String, count: Int)

  def linkCount(page: String, callback: Option[String] = None) = Action {
    Async {
      Backend.eventsFrom(page).asPromise map { events =>
        withCallback(callback) {
          val eventMap = events groupBy { e => (e.sel.getOrElse(""), e.hash.getOrElse("")) }

          val linkCounts = for {
            ((selector, hash), clicks) <- eventMap
          } yield LinkCount(selector, hash, clicks.size)

          Serialization.write(linkCounts.toList)
        }
      }
    }
  }
}
