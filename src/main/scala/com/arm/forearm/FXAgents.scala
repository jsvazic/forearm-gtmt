/**
 * Copyright (c) John Svazic, 2008-2014 - All Rights Reserved 
 */
package com.arm.forearm

import scala.math.{ round, floor }

sealed trait Interval

case object OneMinute extends Interval
case object FiveMinutes extends Interval
case object FifteenMinutes extends Interval
case object ThirtyMinutes extends Interval
case object OneHour extends Interval
case object FourHours extends Interval
case object OneDay extends Interval

trait FXAgent {
  val id: String

  def balance(): Double
  def history(pair: ForexPair, interval: Interval, count: Int): Vector[Candle]
  def leverage(): Int
  def openOrder(pair: ForexPair): Option[OrderLike]
  def spread(pair: ForexPair): Double

  /**
   * Try to find the number of available units for the given pair.
   *
   * @see http://fxtrade.oanda.com/account/units-calculator
   */
  def getMaxAvailableUnits(pair: ForexPair, price: Double): Long = {
    round(floor((balance * leverage.toDouble) / price))
  }
  
  /**
   * Method to retrieve the last action for the given pair.  If an order for the given pair is found, then
   * Buy is returned if it is a Long order, otherwise Sell is returned.  If no order is found, then Stay
   * is returned.
   * 
   * @param The last action for the given pair, one of Buy, Sell or Stay depending if a matching order is found.
   */
  def lastAction(pair: ForexPair) = {
	openOrder(pair) match {
      case Some(o) => o.orderType match {
        case LongOrder => Buy
        case ShortOrder => Sell
      }
      case None => Stay
    }
  }
}

trait FXLiveAgent extends FXAgent {
  def close(orderId: Option[String]): Double
  def execute(pair: ForexPair, orderType: OrderType, units: Long, stopLoss: Double, takeProfit: Double): Option[Long]
  def quote(pair: ForexPair): Option[Quote]
}