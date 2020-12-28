package nl.pragmasoft.catracker

object Model {

  case class StoredPosition(
    id: Long = 0L,
    recorded: Long,
    app: String,
    deviceType: String,
    deviceSerial: String,
    latitude: BigDecimal,
    longitude: BigDecimal,
    positionFix: Boolean,
    bestGateway: String,
    bestSNR: BigDecimal,
    battery: Int,
    temperature: Int,
    counter: Long
  )

  trait PositionRepository[F[_]] {
    def add(p: StoredPosition): F[Unit]
    def findForDevice(deviceSerial: String): F[List[StoredPosition]]
  }

}
