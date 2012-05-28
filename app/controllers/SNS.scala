package controllers

import play.api.mvc.{Action, Controller}
import play.api.libs.ws.WS
import net.liftweb.json
import json.DefaultFormats
import org.joda.time.DateTime
import lib.{Click, Backend}


object SNS extends Controller {

  def receive() = Action { request =>
    implicit val formats = DefaultFormats ++ net.liftweb.json.ext.JodaTimeSerializers.all

    request.body.asText map { text =>
      val notification = json.parse(text).extract[SNSNotification]
      notification.Type match {
        case "Notification" =>  {
          val pageViews = (json.parse(notification.Message)).extract[PageViews]
          for {
            view <- pageViews.views
            actor <- Backend.eventProcessors
            page <- view.previousPage
            selector <- view.previousPageSelector
            hash <- view.previousPageElemHash
          } actor ! Click(page, selector, hash, view.dt)

          Ok("")
        }
        case "SubscriptionConfirmation" => {
          WS.url("https://sns.eu-west-1.amazonaws.com/").withQueryString(
            "Action" -> "ConfirmSubscription",
            "TopicArn" -> notification.TopicArn,
            "Token" -> notification.Token.get
          ).get()
          Ok("")
        }
        case _ => BadRequest("Unknown type found")
      }
    } getOrElse {
      BadRequest("Expected some JSON")
    }
  }
  
  case class PageViews(views: List[PageView])
  case class PageView(
     v: String,
     dt: DateTime,
     url: String,
     documentReferrer: Option[String],

     browserId: BrowserId,
     userAgent: Option[String],
     clientIp: Option[String],

     // if this was a navigation, the following will be filled in
     previousPage: Option[String],
     previousPageSelector: Option[String],
     previousPageElemHash: Option[String])
  case class BrowserId(id: String)
  
  case class SNSNotification(Message: String, TopicArn: String, Type: String, Token: Option[String])
}
