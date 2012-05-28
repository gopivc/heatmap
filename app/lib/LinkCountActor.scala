package lib

import akka.actor.{Actor, ActorLogging}
import org.scala_tools.time.Imports._
import lib.LinkCountActor.Truncate
import collection.mutable
import controllers.Api.LinkCount
import util.Random
import collection.mutable.{MultiMap, HashMap}

case class LinkIdentifier(selector: String, hash: String)
case class Click(page: String, selector: String, hash: String, dt: DateTime) {
  def link = LinkIdentifier(selector, hash)
}
case class Page(url: String)

class Clicks {
  val clicks = mutable.Map.empty[String, MultiMap[LinkIdentifier, (DateTime, Int)]]
  val random = new Random
  def emptyMulti = new HashMap[LinkIdentifier, mutable.Set[(DateTime, Int)]] with MultiMap[LinkIdentifier, (DateTime, Int)]

  def += (click: Click) {
    val linkClicks = (clicks.get(click.page).orElse {
      val v = emptyMulti
      clicks.put(click.page, v)
      Some(v)
    }).get
    linkClicks.addBinding(click.link, (click.dt, random.nextInt()))
  }

  def linkCountsFor(page: String) = {
    val copyOfPageClicks = (clicks.get(page).orElse(Some(emptyMulti))).get.toSeq
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
