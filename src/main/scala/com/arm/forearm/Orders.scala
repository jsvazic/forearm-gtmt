/**
 * Copyright (c) John Svazic, 2008-2014 - All Rights Reserved
 */
package com.arm.forearm

sealed trait OrderType
case object LongOrder extends OrderType
case object ShortOrder extends OrderType

trait OrderLike {
  val orderType: OrderType
  val pair: ForexPair
  val units: Long
  val sl: Double
  val tp: Double

  def id(): Option[String] = None
}

class Order(private val pendingOrder: PendingOrder, private val _id: String, val timestamp: Long, val price: Double) extends OrderLike {
  val orderType = pendingOrder.orderType
  val pair = pendingOrder.pair
  val units = pendingOrder.units
  val leverage = pendingOrder.leverage
  val sl = pendingOrder.sl
  val tp = pendingOrder.tp

  override def id(): Option[String] = Some(_id)

  val slPrice = orderType match {
    case LongOrder =>
      if (pendingOrder.sl > 0.0)
        price - (pendingOrder.sl / pendingOrder.pair.multiplier)
      else
        0.0
    case ShortOrder =>
      if (pendingOrder.sl > 0.0)
        price + (pendingOrder.sl / pendingOrder.pair.multiplier)
      else
        0.0
  }

  val tpPrice = orderType match {
    case LongOrder =>
      if (pendingOrder.tp > 0.0) price + (pendingOrder.tp / pendingOrder.pair.multiplier)
      else 0.0
    case ShortOrder =>
      if (pendingOrder.tp > 0.0) price - (pendingOrder.tp / pendingOrder.pair.multiplier)
      else 0.0
  }
}

object Order {
  def apply(pendingOrder: PendingOrder, id: String, timestamp: Long, price: Double) = {
    new Order(pendingOrder, id, timestamp, price)
  }

  def calculatePL(order: Order, close: Double): Double = {
    if (order.orderType == LongOrder) (close - order.price) * order.units
    else (order.price - close) * order.units
  }

  def calculateValue(order: Order, price: Double = 0.0): Double = {
    if (price != 0.0) order.units * price / order.leverage
    else order.units * order.price / order.leverage
  }
}

case class PendingOrder(val orderType: OrderType, val pair: ForexPair, val leverage: Int, val units: Long, val sl: Double, val tp: Double) extends OrderLike

case class TrainingOrder(private val pendingOrder: PendingOrder, private val _id: String, override val price: Double)
  extends Order(pendingOrder, _id, System.currentTimeMillis, price)
