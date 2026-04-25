package com.music.payment.api

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.music.payment.messages.SupervisorMessages.SupervisorCommand
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ApiServer {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def start(
    system: ActorSystem[SupervisorCommand],
    host: String = "0.0.0.0",
    port: Int = 8080
  ): Future[Http.ServerBinding] = {

    implicit val sys: ActorSystem[SupervisorCommand] = system
    implicit val timeout: Timeout                    = 10.seconds
    implicit val ec: ExecutionContext                = system.executionContext
    implicit val scheduler: akka.actor.typed.Scheduler = system.scheduler

    val apiRoutes = ApiRoutes(system)

    val routes: Route =
      respondWithDefaultHeaders(
        RawHeader("Access-Control-Allow-Origin", "*"),
        RawHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS"),
        RawHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
      ) {
        options { complete("") } ~
        apiRoutes.routes
      }

    // ✅ API moderne Akka HTTP 10.2+ avec Akka Typed
    val binding = Http(system)
      .newServerAt(host, port)
      .bind(routes)

    binding.onComplete {
      case Success(serverBinding) =>
        logger.info(s"✓ API démarrée sur http://$host:$port")
        logger.info(s"  GET  /api/accounts")
        logger.info(s"  GET  /api/accounts/{id}")
        logger.info(s"  POST /api/accounts")
        logger.info(s"  POST /api/accounts/{id}/deposit")
        logger.info(s"  POST /api/accounts/{id}/withdraw")
        logger.info(s"  POST /api/transfer")
        logger.info(s"  GET  /api/health")
      case Failure(exception) =>
        logger.error(s"✗ Impossible de démarrer: ${exception.getMessage}")
    }

    binding
  }
}