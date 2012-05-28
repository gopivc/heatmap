import lib.{Click, Clicks}
import org.joda.time.DateTime
import org.specs2.mutable.Specification

class ClicksSpec extends Specification {
  "Click stream" should {
    "be emptied of old messages" in {
      val clicks = new Clicks()
      clicks += Click("page", "selector", "hash", DateTime.now().minusMinutes(5))
      clicks.linkCountsFor("page") should haveSize(1)
      clicks.removeBefore(DateTime.now().minusMinutes(1))
      clicks.linkCountsFor("page") should beEmpty
    }
  }
}
