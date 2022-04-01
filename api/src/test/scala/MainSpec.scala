import zhttp.http.*
import zio.logging.slf4j.Slf4jLogger
import zio.logging.{LogAnnotation, Logging}
import zio.test.Assertion.*
import zio.test.*
import zio.{Has, ULayer}

object MainSpec extends DefaultRunnableSpec {

  def spec: Spec[Any, TestFailure[Serializable], TestSuccess] =
    suite("http")(
      test("should be ok") {
        val req = URL.fromString("/text").map(s => Request(url = s)).getOrElse(Request())
        for {
          app <- Main.zapp
          one <- app(req)
        } yield assertTrue(one.status == Status.OK)
      },
      test("Running two tests against the same app") {
        val req = URL.fromString("/text").map(s => Request(url = s)).getOrElse(Request())

        for {
          app <- Main.zapp
          one <- app(req)
          two <- app(req)
        } yield assert(one.status)(equalTo(Status.OK)) && assert(two.status)(equalTo(Status.OK))
      }
    )

}
