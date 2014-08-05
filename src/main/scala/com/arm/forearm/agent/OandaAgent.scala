/**
 * Copyright (c) John Svazic, 2008-2014 - All Rights Reserved 
 */
package com.arm.forearm.agent

import scala.language.postfixOps
import scala.math.{ abs, min, round }
import java.text.NumberFormat
import argonaut._, Argonaut._
import dispatch._, Defaults._
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants._
import org.slf4j.LoggerFactory
import com.arm.forearm.{ Candle, ForexPair, FXLiveAgent, Interval, LongOrder, Order, OrderLike, OrderType, Quote, ShortOrder }
import com.arm.forearm.{ OneMinute, FiveMinutes, FifteenMinutes, ThirtyMinutes, OneHour, OneDay }
import com.arm.forearm.agent.Oanda._
import org.joda.time.DateTimeConstants
import com.arm.forearm.FourHours

case class OandaAgent(val id: String, val token: String) extends FXLiveAgent {
  private val log = LoggerFactory.getLogger(classOf[OandaAgent])
  private val nf = NumberFormat.getInstance()
  private val apiHost = host("api-fxpractice.oanda.com").secure
	
  nf.setMaximumFractionDigits(5)
  nf.setMinimumFractionDigits(5)

  override def balance(): Double = {
    val svc = apiHost / "v1" / "accounts" / id <:< Map("Authorization" -> ("Bearer " + token))
    val r = Http(svc)
    val response = r()
    response.getStatusCode match {
      case 200 => {
        val json = response.getResponseBody
        val accountInfo: Option[OandaAccount] = Parse.decodeOption[OandaAccount](json)
        accountInfo match {
          case Some(a) => return a.marginAvailable
          case None => log.error(s"Failed to parse the account info from Oanda!\n\t$json")
        }
      }
      case code: Int => log.error(s"Failed to get account balance, HTTP Code: $code\n\t${response.getResponseBody}")
    }

    0.0
  }
  
  override def leverage(): Int = {
    val svc = apiHost / "v1" / "accounts" / id <:< Map("Authorization" -> ("Bearer " + token))
    val r = Http(svc)
    val response = r()
    response.getStatusCode match {
      case 200 => {
        val json = response.getResponseBody
        val accountInfo: Option[OandaAccount] = Parse.decodeOption[OandaAccount](json)
        accountInfo match {
          case Some(account) => return (1.0 / account.marginRate).toInt
          case None => log.error("Failed to parse the account details to find the leverage used!\n\t{}", json)
        }
      }
      case code: Int => log.error(s"Failed to get the account leverage, HTTP Code: $code\n\t${response.getResponseBody}")
    }
    
    1
  }
  
  override def history(pair: ForexPair, interval: Interval, count: Int): Vector[Candle] = {
    require(count > 0 && count <= 5000)
    
    val granularity = interval match {
      case OneMinute => "M1"
      case FiveMinutes => "M5"
      case FifteenMinutes => "M15"
      case ThirtyMinutes => "M30"
      case OneHour => "H1"
      case FourHours => "H4"
      case OneDay => "D"
    }
    
    val svc = apiHost / "v1" / "candles"  <:< Map("Authorization" -> ("Bearer " + token)) <<? 
        Map("instrument" -> pair.toString) <<? 
        Map("granularity" -> granularity) <<?
        Map("candleFormat" -> "midpoint") <<?
        Map("count" -> (count + 1).toString)
        
    val r = Http(svc)
    val response = r()
    response.getStatusCode match {
      case 200 => {
        val json = response.getResponseBody
        val history: Option[OandaHistory] = Parse.decodeOption[OandaHistory](json)
        history match {
          case Some(l) => return l.candles.toVector.map(_.toCandle)
          case None => log.error(s"Failed to parse the history response for $pair\n\t$json")
        }
      }
      case code: Int => log.error(s"Failed to get the history for $pair, HTTP Code: $code\n\t${response.getResponseBody}")
    }

    Vector[Candle]()
  }
  
  override def openOrder(pair: ForexPair): Option[OrderLike] = {
    val svc = apiHost / "v1" / "accounts" / id / "trades" <:< Map("Authorization" -> ("Bearer " + token)) <<? 
        Map("instrument" -> pair.toString)
        
    val r = Http(svc)
    val response = r()
    response.getStatusCode match {
      case 200 => {
        val json = response.getResponseBody
    	val orderList: Option[OandaTradeList] = Parse.decodeOption[OandaTradeList](json)
    	orderList match {
          case Some(l) if (l.trades.size > 0) => return Some(l.trades(0))
          case None => log.error(s"Failed to parse the open orders for pair: $pair\n\t$json")
          case _ => // Do nothing, we will return None at the end of the function
        }
      }
      case code: Int => log.error(s"Failed to get the open orders for $pair, HTTP Code: $code\n\t${response.getResponseBody}")
    }
    
    None
  }
  
  override def spread(pair: ForexPair): Double = {
    val svc = apiHost / "v1" / "prices"  <:< Map("Authorization" -> ("Bearer " + token)) <<? 
        Map("instruments" -> pair.toString)
        
    val r = Http(svc)
    val response = r()
    response.getStatusCode match {
      case 200 => {
        val json = response.getResponseBody
        val quoteList: Option[OandaQuoteList] = Parse.decodeOption[OandaQuoteList](json)
        quoteList match {
          case Some(l) if (l.prices.size > 0) => return abs(l.prices(0).bid - l.prices(0).ask)
          case Some(l) => log.error("No quotes found for pair: {}", Array[String](pair.toString))
          case None => log.error("Failed to parse the quote response for pair: {}\n\t{}", pair, json)
        }
      }
      case code: Int => log.error(s"Failed to get the spread for $pair, HTTP Code: $code\n\t${response.getResponseBody}")
    }
    
    25.0
  }

  override def close(tradeId: Option[String]): Double = {
    tradeId match {
      case None => return 0.0 
      case Some(tid) => {
    	val svc = apiHost / "v1" / "accounts" / id / "trades" / tid <:< Map("Authorization" -> ("Bearer " + token))
        val r = Http(svc DELETE)
        val response = r()
        response.getStatusCode match {
    	  case 200 => {
    		val json = response.getResponseBody
    		val closeTrade: Option[OandaClosedTrade] = Parse.decodeOption[OandaClosedTrade](json)
    		closeTrade match {
              case Some(t) => return t.profit
              case None => log.error(s"Failed to parse the closed trade response for trade: $tid\n\t$json")
            }
          }
          case code: Int => log.error(s"Failed to close trade $tid, HTTP Code: $code\n\t${response.getResponseBody}")
    	}
      }
    }
    
    0.0
  }
  
  override def execute(pair: ForexPair, orderType: OrderType, units: Long, stopLoss: Double, takeProfit: Double): Option[Long] = {
    quote(pair) match {
      case None => log.error(s"Failed to retrieve a quote for $pair!  No order placed!")
      case Some(quote) => {
	      val sl = orderType match {
	        case LongOrder => quote.ask - (stopLoss / pair.multiplier.toDouble)
	        case ShortOrder => quote.bid + (stopLoss / pair.multiplier.toDouble)
	      }
	
	      val tp = orderType match {
	        case LongOrder => quote.ask + (takeProfit / pair.multiplier.toDouble)
	        case ShortOrder => quote.bid - (takeProfit / pair.multiplier.toDouble)
	      }
	
	      val svc = apiHost / "v1" / "accounts" / id / "orders"  <:< Map("Authorization" -> ("Bearer " + token)) << 
	          Map("instrument" -> pair.toString) << 
	          Map("units" -> units.toString) << 
	          Map("side" -> (if (orderType == LongOrder) "buy" else "sell")) << 
	          Map("type" -> "market") << 
	          Map("stopLoss" -> sl.toString) << 
	          Map("takeProfit" -> tp.toString)
	        
	      val r = Http(svc)
	      val response = r()
	      response.getStatusCode match {
	        case 200 => // Success, no need to do anything else
          case code: Int => log.error(s"Failed to execute an order, HTTP Code: $code\n\t${response.getResponseBody}")
	      }
      }
    }

    None
  }
  
  override def quote(pair: ForexPair): Option[Quote] = {
    val svc = apiHost / "v1" / "prices"  <:< Map("Authorization" -> ("Bearer " + token)) <<? 
        Map("instruments" -> pair.toString)
        
    val r = Http(svc)
    val response = r()
    response.getStatusCode match {
      case 200 => {
        val json = response.getResponseBody
        val quoteList: Option[OandaQuoteList] = Parse.decodeOption[OandaQuoteList](json)
        quoteList match {
          case Some(l) if (l.prices.size > 0) => return Some(l.prices(0))
          case Some(l) => log.error("No quotes found for pair: " + pair.toString)
          case None => log.error(s"Failed to parse the quote response for pair: $pair\n\t$json")
        }
      }
      case code: Int => log.error(s"Failed to get a quote, HTTP Code: $code\n\t${response.getResponseBody}")
    }

    None
  }
}
