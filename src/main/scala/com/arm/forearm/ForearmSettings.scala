/**
 * Copyright (c) John Svazic, 2008-2014 - All Rights Reserved
 */
package com.arm.forearm

/**
 * Base class containing all constants that can be manipulated for the program.
 */
object ForearmSettings {
  // Oanda Settings
  val ACCOUNT_ID = "" // TODO: Specify the Oanda Account ID here
  val API_TOKEN  = "" // TODO: Specify the Oanda API Token here

  // Generic settings
  val PAIRS = List[ForexPair](ForexPair("NZD/USD"))
  val INTERVAL = ThirtyMinutes
  
  // Technical Indicator Settings
  val EMA_PERIODS = 11
  val SMA_PERIODS = 19
  val RSI_PERIODS = 9
  val MOVING_AVERAGE_MIN_DELTA = 3.0
  val MIN_RSI_VALUE = 20.0  
  val MAX_RSI_VALUE = 80.0

  // Order settings
  val ORDER_UNIT_SIZE = 20000
  val SL_PIPS = 30.0
  val TP_PIPS = 60.0
}