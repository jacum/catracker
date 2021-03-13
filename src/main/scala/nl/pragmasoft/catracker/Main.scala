package nl.pragmasoft.catracker

import akka.actor.typed.ActorSystem
import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import cats.implicits._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import doobie.util.ExecutionContexts
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, Logger}
import org.http4s.server.websocket._
import org.http4s.server.{Router, Server}
import org.http4s.websocket.WebSocketFrame._
import org.slf4j.LoggerFactory

import java.net.InetSocketAddress
import scala.concurrent.ExecutionContext

object Main extends IOApp with LazyLogging {

  override def run(args: List[String]): IO[ExitCode] = {
    val config = ConfigFactory.load()
    implicit val system: ActorSystem[Trackers.Command] = ActorSystem(Trackers(), "main", config)
    implicit val executionContext: ExecutionContext = system.executionContext

    val apiLoggingAction: Option[String => IO[Unit]] = {
      val apiLogger = LoggerFactory.getLogger("HTTP")
      Some(s => IO(apiLogger.info(s)))
    }

    val mainResource: Resource[IO, Server[IO]] =
      for {
        config <- Config.load()
        ec <- ExecutionContexts.fixedThreadPool[IO](config.database.threadPoolSize)
        blocker <- Blocker[IO]
        transactor <- Database.transactor(config.database, ec, blocker)
        _ <- Resource.liftF(Database.initialize(transactor))
        repository = new PositionDatabase(transactor)
        handler = new http.Resource[IO]().routes(new ApiHandler[IO](repository, system))
        apiService <- BlazeServerBuilder[IO](executionContext)
          .bindSocketAddress(InetSocketAddress.createUnresolved("0.0.0.0", 8081))
          .withHttpApp(Logger.httpApp(logHeaders = true, logBody = true, logAction = apiLoggingAction)(Router("/api/catracker" -> (
            CORS(HttpRoutes.of[IO] {
              case GET -> Root / "health" => Ok()
              case GET -> Root / "ws" / device / connectionId =>
                WebSocketBuilder[IO].build(
                  send = Trackers.streamUpdates(DeviceId(device), ConnectionId(connectionId)),
                  receive = _.evalMap {
                    case Text(t, _) => IO(println(t))
                    case f => IO(println(s"Unknown type: $f"))
                  },
                  onClose = Trackers.disconnect(DeviceId(device), ConnectionId(connectionId))
                )

          } <+> handler))) orNotFound))
          .resource
      } yield apiService
    mainResource.use(_ => IO.never).as(ExitCode.Success)
  }

}
