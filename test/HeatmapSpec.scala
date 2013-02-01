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
  "clicks for heatmap" should {
//    "provide links with appropriate referrer" in {
//      running(FakeApplication()) {
//        val views = snsMessageOf(PageViews(List(view("http://site.com/page", "http://site.com/referrer"))))
//        routeAndCall(FakeRequest("POST", "/incoming/sns", FakeHeaders(), AnyContentAsText(views)))
//
//        val Some(AsyncResult(result)) = routeAndCall(
//          FakeRequest("GET", "/api/linkCounts?page=%s" format ("http://site.com/referrer")))
//        val resultJson = contentAsString(result.await.get)
//        val counts = json.parse(resultJson).extract[List[LinkCount]]
//
//        counts(0) should be equalTo (LinkCount("selector", "hash", 1))
//      }
//    }
    "provide counts of multiple clicks of the same link" in {
      running(FakeApplication()) {
        val views = snsMessageOf(PageViews(List(
          view("http://site.com/page", "http://site.com/referrer"),
          view("http://site.com/page", "http://site.com/referrer")
        )))
        routeAndCall(FakeRequest("POST", "/incoming/sns", FakeHeaders(), AnyContentAsText(views)))

        val Some(AsyncResult(result)) = routeAndCall(
          FakeRequest("GET", "/api/linkCounts?page=%s" format ("http://site.com/referrer")))
        val resultJson = contentAsString(result.await.get)
        val counts = json.parse(resultJson).extract[List[LinkCount]]

        counts(0) should be equalTo (LinkCount("selector", "hash", 2))
      }
    }
  }

  def view(url: String, referrer: String, selector: String = "selector", hash: String = "hash") =
    PageView(
      v = "3",
      dt = DateTime.now(),
      url = url,
      documentReferrer = Some(referrer),
      browserId = BrowserId(""),
      userAgent = None,
      clientIp = None,
      previousPage = Some(referrer),
      previousPageSelector = Some(selector),
      previousPageElemHash = Some(hash)
    )

  def snsMessageOf(views: PageViews) = {
    val n = SNS.SNSNotification(Serialization.write(views), "topic", "Notification", None)
    Serialization.write(n)
  }
  implicit val formats = DefaultFormats.lossless ++ net.liftweb.json.ext.JodaTimeSerializers.all
}
