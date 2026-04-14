package com.music.payment.messages

/**
 * Messages communs partagés entre plusieurs acteurs.
 * Contient les types et réponses utilisés par différents acteurs.
 */
object CommonMessages {

  /** Résultats des opérations sur les comptes */
  sealed trait OperationResult
  case class OperationSuccess(accountId: String, newBalance: Double, message: String) extends OperationResult
  case class OperationFailure(accountId: String, reason: String) extends OperationResult

  /** Réponse du solde d'un compte */
  case class BalanceResponse(accountId: String, balance: Double)

  // Réexportation des messages spécifiques aux acteurs
  // Pour faciliter les imports centralisés si nécessaire
}
