package com.music.payment.api

import akka.actor.typed.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.music.payment.messages.SupervisorMessages.SupervisorCommand
import com.music.payment.messages.PaymentMessages._
import com.music.payment.api.ApiModels._
import akka.actor.typed.scaladsl.AskPattern._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Routes API REST pour le système de paiement
 * 
 * Endpoints :
 * - GET  /accounts                          : liste tous les comptes
 * - GET  /accounts/{id}                     : récupère un compte
 * - POST /accounts                          : crée un compte
 * - POST /accounts/{id}/deposit             : effectue un dépôt
 * - POST /accounts/{id}/withdraw            : effectue un retrait
 * - POST /transfer                          : transfert entre comptes
 * - GET  /health                            : vérification de santé
 */
class ApiRoutes(supervisor: ActorRef[SupervisorCommand])(implicit timeout: Timeout, ec: ExecutionContext, scheduler: akka.actor.typed.Scheduler) {

  private def handleOperationResult(result: OperationResult): (akka.http.scaladsl.model.StatusCode, OperationResponse) = {
    result match {
      case OperationSuccess(id, balance, msg) =>
        (StatusCodes.OK, OperationResponse(success = true, Some(id), Some(balance), msg))
      case OperationFailure(id, reason) =>
        (StatusCodes.BadRequest, OperationResponse(success = false, Some(id), None, reason))
    }
  }

  private def handleTransactionResult(result: TransactionResult): (akka.http.scaladsl.model.StatusCode, String, String, Double, TransferResponse) = {
    result match {
      case TransferSuccess(from, to, amount, msg) =>
        (StatusCodes.OK, from, to, amount, TransferResponse(success = true, from, to, amount, None, None, msg))
      case TransferFailure(from, to, amount, reason) =>
        (StatusCodes.BadRequest, from, to, amount, TransferResponse(success = false, from, to, amount, None, None, reason))
    }
  }

  val routes: Route =
    pathPrefix("api") {
      pathPrefix("accounts") {
        // GET /api/accounts - Lister tous les comptes
        get {
          path(Segment / "") { _ => reject } ~ path("") {
            val futureList = supervisor.ask[AccountList](ref => ListAccounts(ref))
            onSuccess(futureList) { accountList =>
              // Récupérer les détails de chaque compte
              val futureDetails = Future.sequence(
                accountList.accounts.map { accountId =>
                  supervisor.ask[BalanceResponse](ref => QueryBalance(accountId, ref))
                }
              )
              onSuccess(futureDetails) { responses =>
                val accounts = responses.map { resp =>
                  AccountResponse(resp.accountId, "Unknown", resp.balance)
                }
                complete(StatusCodes.OK, AccountListResponse(accounts))
              }
            }
          }
        } ~
        // GET /api/accounts/{id} - Récupérer un compte spécifique
        get {
          path(Segment) { id =>
            val futureBalance = supervisor.ask[BalanceResponse](ref => QueryBalance(id, ref))
            onSuccess(futureBalance) { response =>
              complete(StatusCodes.OK, AccountResponse(response.accountId, "Unknown", response.balance))
            }
          }
        } ~
        // POST /api/accounts - Créer un compte
        post {
          entity(as[CreateAccountRequest]) { req =>
            val futureResult = supervisor.ask[OperationResult](ref => 
              CreateAccount(req.id, req.ownerName, req.initialBalance, ref)
            )
            onSuccess(futureResult) { result =>
              val (status, response) = handleOperationResult(result)
              complete(status, response)
            }
          }
        }
      } ~
      pathPrefix("accounts" / Segment) { accountId =>
        // POST /api/accounts/{id}/deposit - Dépôt
        path("deposit") {
          post {
            entity(as[DepositRequest]) { req =>
              val futureResult = supervisor.ask[OperationResult](ref => 
                PerformDeposit(accountId, req.amount, ref)
              )
              onSuccess(futureResult) { result =>
                val (status, response) = handleOperationResult(result)
                complete(status, response)
              }
            }
          }
        } ~
        // POST /api/accounts/{id}/withdraw - Retrait
        path("withdraw") {
          post {
            entity(as[WithdrawRequest]) { req =>
              val futureResult = supervisor.ask[OperationResult](ref => 
                PerformWithdraw(accountId, req.amount, ref)
              )
              onSuccess(futureResult) { result =>
                val (status, response) = handleOperationResult(result)
                complete(status, response)
              }
            }
          }
        }
      } ~
      // POST /api/transfer - Transfert entre comptes
      path("transfer") {
        post {
          entity(as[ApiModels.TransferRequest]) { req =>
            val futureResult = supervisor.ask[TransactionResult](ref => 
              PerformTransfer(req.fromAccountId, req.toAccountId, req.amount, ref)
            )
            onSuccess(futureResult) { result =>
              val (status, from, to, amount, response) = handleTransactionResult(result)
              complete(status, response)
            }
          }
        }
      } ~
      // GET /api/health - Vérification de santé
      path("health") {
        get {
          complete(StatusCodes.OK, Map("status" -> "ok", "message" -> "Payment system API is running"))
        }
      }
    }
}

object ApiRoutes {
  def apply(supervisor: ActorRef[SupervisorCommand])(implicit timeout: Timeout, ec: ExecutionContext, scheduler: akka.actor.typed.Scheduler): ApiRoutes =
    new ApiRoutes(supervisor)
}
