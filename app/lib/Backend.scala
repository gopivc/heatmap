package lib

import java.util.concurrent.TimeUnit
import com.gu.openplatform.contentapi.model.Content
import org.joda.time.{Duration, DateTime}
import akka.actor._
import akka.util.duration._
import akka.dispatch.{Await, Future}
import akka.util.Timeout
import akka.pattern.ask
import concurrent.ops
import ops._
import play.api.libs.concurrent.Akka
import play.api.Play
import controllers.Api.LinkCount

object Backend {
  import Play.current

  val counter = Akka.system.actorOf(Props(new LinkCountActor(Config.eventHorizon)), name = "linkCounter")

  val eventProcessors = counter :: Nil

  def start() {
    Akka.system.scheduler.schedule(1 minute, 1 minute, counter, LinkCountActor.Truncate)
  }

  def stop() {
    Akka.system.shutdown()
  }

  // So this is a bad way to do this, should use akka Agents instead (which can read
  // without sending a message.)

  implicit val timeout = Timeout(5 seconds)

  def eventsFrom(page: String) = (counter ? Page(page)).mapTo[List[LinkCount]]

}