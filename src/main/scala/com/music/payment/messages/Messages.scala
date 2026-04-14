package com.music.payment.messages

/**
 * Centralized entry point for all payment system messages.
 * 
 * This file re-exports all messages defined in actor-specific files
 * to allow backward compatibility and better code organization.
 * 
 * Messages organized by actor:
 * - BankAccountMessages: Deposit, withdrawal, balance query
 * - TransactionMessages: Money transfers between accounts
 * - SupervisorMessages: Account management and coordination
 * - LoggingMessages: Operation logging
 * - CommonMessages: Shared types and responses
 */
object PaymentMessages {
  // Re-export for backward compatibility with existing imports
  
  // Messages BankAccount
  type Deposit = BankAccountMessages.Deposit
  val Deposit = BankAccountMessages.Deposit
  type Withdraw = BankAccountMessages.Withdraw
  val Withdraw = BankAccountMessages.Withdraw
  type GetBalance = BankAccountMessages.GetBalance
  val GetBalance = BankAccountMessages.GetBalance
  type AccountCommand = BankAccountMessages.AccountCommand
  type DepositCmd = BankAccountMessages.DepositCmd
  val DepositCmd = BankAccountMessages.DepositCmd
  type WithdrawCmd = BankAccountMessages.WithdrawCmd
  val WithdrawCmd = BankAccountMessages.WithdrawCmd
  type GetBalanceCmd = BankAccountMessages.GetBalanceCmd
  val GetBalanceCmd = BankAccountMessages.GetBalanceCmd

  // Messages communs
  type OperationResult = CommonMessages.OperationResult
  type OperationSuccess = CommonMessages.OperationSuccess
  val OperationSuccess = CommonMessages.OperationSuccess
  type OperationFailure = CommonMessages.OperationFailure
  val OperationFailure = CommonMessages.OperationFailure
  type BalanceResponse = CommonMessages.BalanceResponse
  val BalanceResponse = CommonMessages.BalanceResponse

  // Messages Transaction
  type TransactionCommand = TransactionMessages.TransactionCommand
  type TransferRequest = TransactionMessages.TransferRequest
  val TransferRequest = TransactionMessages.TransferRequest
  type DebitResult = TransactionMessages.DebitResult
  val DebitResult = TransactionMessages.DebitResult
  type CreditResult = TransactionMessages.CreditResult
  val CreditResult = TransactionMessages.CreditResult
  type RollbackResult = TransactionMessages.RollbackResult
  val RollbackResult = TransactionMessages.RollbackResult
  type TransactionResult = TransactionMessages.TransactionResult
  type TransferSuccess = TransactionMessages.TransferSuccess
  val TransferSuccess = TransactionMessages.TransferSuccess
  type TransferFailure = TransactionMessages.TransferFailure
  val TransferFailure = TransactionMessages.TransferFailure

  // Messages Supervisor
  type SupervisorCommand = SupervisorMessages.SupervisorCommand
  type CreateAccount = SupervisorMessages.CreateAccount
  val CreateAccount = SupervisorMessages.CreateAccount
  type PerformDeposit = SupervisorMessages.PerformDeposit
  val PerformDeposit = SupervisorMessages.PerformDeposit
  type PerformWithdraw = SupervisorMessages.PerformWithdraw
  val PerformWithdraw = SupervisorMessages.PerformWithdraw
  type QueryBalance = SupervisorMessages.QueryBalance
  val QueryBalance = SupervisorMessages.QueryBalance
  type PerformTransfer = SupervisorMessages.PerformTransfer
  val PerformTransfer = SupervisorMessages.PerformTransfer
  type ListAccounts = SupervisorMessages.ListAccounts
  val ListAccounts = SupervisorMessages.ListAccounts
  type AccountList = SupervisorMessages.AccountList
  val AccountList = SupervisorMessages.AccountList

  // Messages Logging
  type LoggingCommand = LoggingMessages.LoggingCommand
  type LogTransaction = LoggingMessages.LogTransaction
  val LogTransaction = LoggingMessages.LogTransaction
  type LogAccountOperation = LoggingMessages.LogAccountOperation
  val LogAccountOperation = LoggingMessages.LogAccountOperation
  type GetTransactionLog = LoggingMessages.GetTransactionLog
  val GetTransactionLog = LoggingMessages.GetTransactionLog
  type TransactionLogResponse = LoggingMessages.TransactionLogResponse
  val TransactionLogResponse = LoggingMessages.TransactionLogResponse
}
