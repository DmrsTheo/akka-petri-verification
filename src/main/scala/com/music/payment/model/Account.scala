package com.music.payment.model

/**
 * Represents a bank account with an identifier, an owner and a balance.
 * The balance must never be negative (business invariant).
 */
case class Account(
  id: String,
  ownerName: String,
  balance: Double
) {
  require(balance >= 0, s"Account $id balance cannot be negative: $balance")

  def deposit(amount: Double): Account = {
    require(amount > 0, "Deposit amount must be positive")
    copy(balance = balance + amount)
  }

  def withdraw(amount: Double): Either[String, Account] = {
    if (amount <= 0) Left("Withdrawal amount must be positive")
    else if (amount > balance) Left(s"Insufficient funds: balance=$balance, requested=$amount")
    else Right(copy(balance = balance - amount))
  }
}
