import controllers.Api.LinkCount
import controllers.SNS
import controllers.SNS.{BrowserId, PageView, PageViews}
import net.liftweb.json
import net.liftweb.json.{Serialization, DefaultFormats}
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import play.api.mvc.{AsyncResult, AnyContentAsText}
import play.api.test.Helpers._
import play.api.test.{FakeApplication, FakeHeaders, FakeRequest}

class HeatmapSpec extends Specification{
  "should make clicks available in heatmap" in {
    running(FakeApplication()) {
      val views = snsMessageOf(PageViews(List(PageView(
        v = "3",
        dt = DateTime.now(),
        url = "http://site.com/page",
        documentReferrer = Some("http://site.com/referrer"),
        browserId = BrowserId(""),
        userAgent = None,
        clientIp = None,
        previousPage = Some("http://site.com/referrer"),
        previousPageSelector = Some("selector"),
        previousPageElemHash = Some("hash")
      ))))
      routeAndCall(FakeRequest("POST", "/incoming/sns", FakeHeaders(), AnyContentAsText(views)))

      val Some(AsyncResult(result)) = routeAndCall(FakeRequest("GET", "/api/linkCounts?page=%s" format ("http://site.com/referrer")))
      val resultJson = contentAsString(result.await.get)
      val counts = json.parse(resultJson).extract[List[LinkCount]]

      counts(0) should be equalTo (LinkCount("selector", "hash", 1))
    }
  }


  implicit val formats = DefaultFormats ++ net.liftweb.json.ext.JodaTimeSerializers.all

  def snsMessageOf(views: PageViews) = {
    val n = SNS.SNSNotification(Serialization.write(views), "topic", "Notification", None)
    Serialization.write(n)
  }
}
