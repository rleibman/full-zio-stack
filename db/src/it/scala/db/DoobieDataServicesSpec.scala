package db

import zio.test.*

object DoobieDataServicesSpec extends ZIOSpecDefault {
  def spec: Spec[Any, TestFailure[Serializable], TestSuccess] = suite("Simple CRUD tests")(
    test("All") {
      ???
    },
    test("Delete") {
      ???
    },
    test("Get") {
      ???
    },
    test("Upsert") {
      ???
    }
  )
}
