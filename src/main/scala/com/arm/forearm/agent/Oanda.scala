/**
 * Copyright (c) John Svazic, 2008-2014 - All Rights Reserved 
 */
package com.arm.forearm.agent

import argonaut._, Argonaut._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import com.arm.forearm.{ Candle, ForexPair, LongOrder, OrderLike, Quote, ShortOrder }

object Oanda {
 
  case class OandaAccount(accountId: Long, accountName: String, balance: Double, marginRate: Double, marginAvailable: Double)
  implicit def OandaAccountCodecJson = casecodec5(OandaAccount.apply, OandaAccount.unapply)("accountId", "accountName", "balance", "marginRate", "marginAvail") 
  
  case class OandaCandle(time: String, openMid: Double, highMid: Double, lowMid: Double, closeMid: Double, volume: Long) {
    
    def toCandle() = Candle(new DateTime(time).getMillis, openMid, highMid, lowMid, closeMid)
    override def toString() = {
      val fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm")
      s"Candle(${fmt.print(new DateTime(time))},$openMid,$highMid,$lowMid,$closeMid)"
    }
  }
  implicit def OandaCandleCodecJson = casecodec6(OandaCandle.apply, OandaCandle.unapply)("time", "openMid", "highMid", "lowMid", "closeMid", "volume")
    
  case class OandaHistory(instrument: String, granularity: String, candles: List[OandaCandle]) {
    val pair = ForexPair(instrument)
  }
  implicit def OandaHistoryCodecJson = casecodec3(OandaHistory.apply, OandaHistory.unapply)("instrument", "granularity", "candles")

  case class OandaTrade(_id: Long, instrument: String, units: Long, side: String, time: String, 
      price: Double, tp: Double, sl: Double) extends OrderLike {
    
    val pair = ForexPair(instrument)
    val orderType = if ("buy".equalsIgnoreCase(side)) LongOrder else ShortOrder
    override def id() = Some(_id.toString)
  }
  
  implicit def OandaOrderCodecJson = casecodec8(OandaTrade.apply, OandaTrade.unapply)("id", "instrument", "units", "side", 
      "time", "price", "takeProfit", "stopLoss")

  case class OandaTradeList(trades: List[OandaTrade])
  implicit def OandaOrderListCodecJson = casecodec1(OandaTradeList.apply, OandaTradeList.unapply)("trades")
      
  case class OandaQuote(instrument: String, time: String, bid: Double, ask: Double) extends Quote {
    val timestamp = new DateTime(time).getMillis
    val pair = ForexPair(instrument)
  }
  implicit def OandaQuoteCodecJson = casecodec4(OandaQuote.apply, OandaQuote.unapply)("instrument", "time", "bid", "ask")

  case class OandaQuoteList(prices: List[OandaQuote])
  implicit def OandaQuoteListCodecJson = casecodec1(OandaQuoteList.apply, OandaQuoteList.unapply)("prices")
  
  case class OandaClosedTrade(_id: Long, price: Double, instrument: String, profit: Double, side: String, time: String) {
    val pair = ForexPair(instrument)
    val timestamp = new DateTime(time).getMillis
    val orderType = if ("buy".equalsIgnoreCase(side)) LongOrder else ShortOrder
    def id() = Some(_id.toString)
  }
  implicit def OandaClosedTradeCodecJson = casecodec6(OandaClosedTrade.apply, OandaClosedTrade.unapply)("id", "price", "instrument", "profit", "side", "time")
}