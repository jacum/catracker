package nl.pragmasoft.catracker

import cats.effect.{Blocker, ContextShift, IO, Resource}
import com.typesafe.config.ConfigFactory
import nl.pragmasoft.catracker.Config.DatabaseConfig
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._

case class Config(database: DatabaseConfig)

object Config {

  case class DatabaseConfig(driver: String, url: String, user: String, password: String, threadPoolSize: Int)

  def load(configFile: String = "application.conf")(implicit cs: ContextShift[IO]): Resource[IO, Config] = {
    Blocker[IO].flatMap { blocker =>
      Resource.liftF(ConfigSource.fromConfig(ConfigFactory.load(configFile)).loadF[IO, Config](blocker))
    }
  }
}
