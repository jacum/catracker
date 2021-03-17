package nl.pragmasoft.catracker

import cats.effect.{Blocker, ContextShift, IO, Resource}
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import doobie.util.transactor.Transactor
import fs2.Stream
import doobie._
import doobie.implicits._
import doobie.implicits.javasql._
import doobie.implicits.javatime._
import nl.pragmasoft.catracker.Config.DatabaseConfig
import nl.pragmasoft.catracker.Model.{PositionRepository, StoredPosition}

import scala.concurrent.ExecutionContext

object Database extends LazyLogging {
  def transactor(config: DatabaseConfig, executionContext: ExecutionContext, blocker: Blocker)(implicit contextShift: ContextShift[IO]): Resource[IO, HikariTransactor[IO]] =
    HikariTransactor.newHikariTransactor[IO](
      config.driver,
      config.url,
      config.user,
      config.password,
      executionContext,
      blocker
    )

  def initialize(transactor: HikariTransactor[IO]): IO[Unit] =
    transactor.configure { dataSource =>
      IO {
        logger.info(s"Migrations for $dataSource")
        val flyWay = Flyway.configure().dataSource(dataSource).load()
        flyWay.migrate()
      }
    }
}

class PositionDatabase(transactor: Transactor[IO]) extends PositionRepository[IO] {

  def add(p: StoredPosition): IO[Unit] =
    sql"INSERT INTO positions (recorded, app, deviceType, deviceSerial, latitude, longitude, positionFix, bestGateway, bestSNR, battery, accuracy, temperature, counter) VALUES (${p.recorded}, ${p.app}, ${p.deviceType}, ${p.deviceSerial}, ${p.latitude}, ${p.longitude}, ${p.positionFix}, ${p.bestGateway}, ${p.bestSNR}, ${p.battery}, ${p.accuracy}, ${p.temperature}, ${p.counter})".update
      .withUniqueGeneratedKeys[Long]("id")
      .transact(transactor)
      .map(_ => ())

  def findForDevice(deviceSerial: String): IO[List[StoredPosition]] =
    sql"SELECT id, recorded, app, deviceType, deviceSerial, latitude, longitude, positionFix, bestGateway, bestSNR, battery, accuracy, temperature, counter FROM positions WHERE deviceSerial=$deviceSerial and positionFix=1 and latitude<>0 and longitude<>0 order by recorded desc limit 100"
      .query[StoredPosition]
      .to[List]
      .transact(transactor)

}
