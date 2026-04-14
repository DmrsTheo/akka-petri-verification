package com.music.payment.messages

import com.music.payment.messages.CommonMessages.OperationResult

/**
 * Messages for the TransactionManager actor.
 * Manages money transfers between bank accounts.
 */
object TransactionMessages {

  sealed trait TransactionCommand
  case class TransferRequest(
    fromAccountId: String,
    toAccountId: String,
    amount: Double,
    replyTo: akka.actor.typed.ActorRef[TransactionResult]
  ) extends TransactionCommand

  /** Messages internes pour le protocole de transfert */
  case class DebitResult(result: OperationResult, toAccountId: String, amount: Double,
                         replyTo: akka.actor.typed.ActorRef[TransactionResult]) extends TransactionCommand
  case class CreditResult(result: OperationResult, fromAccountId: String, amount: Double,
                          replyTo: akka.actor.typed.ActorRef[TransactionResult]) extends TransactionCommand
  /** Message interne pour le résultat du rollback (compensation) */
  case class RollbackResult(result: OperationResult, fromId: String, toId: String,
                            amount: Double, replyTo: akka.actor.typed.ActorRef[TransactionResult]) extends TransactionCommand

  /** Résultats des transactions */
  sealed trait TransactionResult
  case class TransferSuccess(fromId: String, toId: String, amount: Double, message: String) extends TransactionResult
  case class TransferFailure(fromId: String, toId: String, amount: Double, reason: String) extends TransactionResult
}
