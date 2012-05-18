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

object Backend {
  implicit val system = ActorSystem("liveDashboard")
  val listener = system.actorOf(Props(new ClickStreamActor(Config.eventHorizon)), name = "clickStreamListener")

  val eventProcessors = listener :: Nil

  def start() {
    system.scheduler.schedule(1 minute, 1 minute, listener, ClickStreamActor.TruncateClickStream)
  }

  def stop() {
    system.shutdown()
  }

  // So this is a bad way to do this, should use akka Agents instead (which can read
  // without sending a message.)

  implicit val timeout = Timeout(5 seconds)

  def eventsFrom(page: String) = (listener ? ClickStreamActor.GetClickStream).mapTo[ClickStream] map { clickStream =>
    clickStream.userClicks.filter(_.referrer == Some(page))
  }

}