/**
 * Copyright (c) John Svazic, 2008-2014 - All Rights Reserved
 */
package com.arm.forearm

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

trait Quote {
  val timestamp: Long
  val bid: Double
  val ask: Double
}

case class Candle(timestamp: Long, open: Double, high: Double, low: Double, close: Double) {
  require(timestamp > 0)
  require(open > 0.0)
  require(high > 0.0)
  require(low > 0.0)
  require(close > 0.0)
  require(high >= open && high >= close && high >= low)
  require(low <= open && low <= high && low <= close)

  override def toString() = {
    val fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm")
    s"Candle(${fmt.print(new DateTime(timestamp))}, O:$open, H:$high, L:$low, C:$close)"
  }
}