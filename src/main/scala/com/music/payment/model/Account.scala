package com.music.payment.model

/**
 * Représente un compte bancaire avec un identifiant, un propriétaire et un solde.
 * Le solde ne doit jamais être négatif (invariant métier).
 */
case class Account(
  id: String,
  ownerName: String,
  balance: Double
) {
  require(balance >= 0, s"Le solde du compte $id ne peut pas être négatif: $balance")

  def deposit(amount: Double): Account = {
    require(amount > 0, "Le montant du dépôt doit être positif")
    copy(balance = balance + amount)
  }

  def withdraw(amount: Double): Either[String, Account] = {
    if (amount <= 0) Left("Le montant du retrait doit être positif")
    else if (amount > balance) Left(s"Fonds insuffisants: solde=$balance, demandé=$amount")
    else Right(copy(balance = balance - amount))
  }
}
