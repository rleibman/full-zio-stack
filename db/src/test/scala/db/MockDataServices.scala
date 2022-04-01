package db

import model.*
import zio.*

import javax.sql.DataSource

object MockDataServices {

  final private case class MockModelObjectDataService() extends ModelObjectDataService {

    def search(search: Option[Nothing]): IO[DataServiceException, List[ModelObject]] = ???

    def delete(
      id:         ModelObjectId,
      softDelete: Boolean
    ): zio.IO[DataServiceException, Boolean] = ???

    def get(id: ModelObjectId): zio.IO[DataServiceException, Option[ModelObject]] = ???

    def upsert(obj: ModelObject): zio.IO[DataServiceException, ModelObject] = ???

  }

  val modelObjectDataServices: ULayer[ModelObjectDataService] = (MockModelObjectDataService.apply _).toLayer[ModelObjectDataService]

}
