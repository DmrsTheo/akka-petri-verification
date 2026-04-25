package com.music.payment.api

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
 * Modèles de sérialisation JSON pour les requêtes/réponses API
 */
object ApiModels extends DefaultJsonProtocol {

  // ===== Requêtes d'entrée =====
  
  case class CreateAccountRequest(
    id: String,
    ownerName: String,
    initialBalance: Double
  )

  case class DepositRequest(
    amount: Double
  )

  case class WithdrawRequest(
    amount: Double
  )

  case class TransferRequest(
    fromAccountId: String,
    toAccountId: String,
    amount: Double
  )

  // ===== Réponses =====

  case class AccountResponse(
    id: String,
    ownerName: String,
    balance: Double
  )

  case class OperationResponse(
    success: Boolean,
    accountId: Option[String] = None,
    newBalance: Option[Double] = None,
    message: String
  )

  case class TransferResponse(
    success: Boolean,
    fromAccountId: String,
    toAccountId: String,
    amount: Double,
    fromNewBalance: Option[Double] = None,
    toNewBalance: Option[Double] = None,
    message: String
  )

  case class AccountListResponse(
    accounts: List[AccountResponse]
  )

  case class ErrorResponse(
    error: String,
    details: Option[String] = None
  )

  // ===== Format JSON Implicits =====
  
  implicit val createAccountRequestFormat: RootJsonFormat[CreateAccountRequest] = jsonFormat3(CreateAccountRequest)
  implicit val depositRequestFormat: RootJsonFormat[DepositRequest] = jsonFormat1(DepositRequest)
  implicit val withdrawRequestFormat: RootJsonFormat[WithdrawRequest] = jsonFormat1(WithdrawRequest)
  implicit val transferRequestFormat: RootJsonFormat[TransferRequest] = jsonFormat3(TransferRequest)

  implicit val accountResponseFormat: RootJsonFormat[AccountResponse] = jsonFormat3(AccountResponse)
  implicit val operationResponseFormat: RootJsonFormat[OperationResponse] = jsonFormat4(OperationResponse)
  implicit val transferResponseFormat: RootJsonFormat[TransferResponse] = jsonFormat7(TransferResponse)
  implicit val accountListResponseFormat: RootJsonFormat[AccountListResponse] = jsonFormat1(AccountListResponse)
  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse] = jsonFormat2(ErrorResponse)
}
