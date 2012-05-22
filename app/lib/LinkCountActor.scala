package lib

import akka.actor.{Actor, ActorLogging}
import org.scala_tools.time.Imports._
import lib.LinkCountActor.Truncate
import collection.mutable.{HashMap, MultiMap}
import collection.mutable
import controllers.Api.LinkCount
import util.Random

case class LinkIdentifier(selector: String, hash: String)
case class Click(page: String, selector: String, hash: String, dt: DateTime) {
  def link = LinkIdentifier(selector, hash)
}
case class Page(url: String)

class Clicks {
  val clicks = mutable.Map[String, MultiMap[LinkIdentifier, (DateTime, Int)]]().withDefaultValue(
    new HashMap[LinkIdentifier, mutable.Set[(DateTime, Int)]] with MultiMap[LinkIdentifier, (DateTime, Int)]
  )
  val random = new Random()

  def += (click: Click) {
    val linkClicks = clicks(click.page)
    linkClicks.addBinding(click.link, (click.dt, random.nextInt()))
  }

  def linkCountsFor(page: String) = {
    val copyOfPageClicks = clicks(page).toSeq
    (for { (k,v) <- copyOfPageClicks } yield LinkCount(k.selector, k.hash, v.size)) toList
  }

  def removeBefore(dt: DateTime) {
    for {
      (_, v) <- clicks
      (id, dts) <- v
      (clickTime, num) <- dts
    } {
      if (clickTime < dt) v.removeBinding(id, (clickTime, num))
    }
  }
}

class LinkCountActor(retentionPeriod: Long) extends Actor with ActorLogging {
  val clicks = new Clicks()

  protected def receive = {
    case c: Click => {
      clicks += (c)
    }
    case p: Page => sender ! clicks.linkCountsFor(p.url)
    case Truncate => {
      clicks.removeBefore(DateTime.now - retentionPeriod)
    }
  }
}

object LinkCountActor {
  case object Truncate
}
