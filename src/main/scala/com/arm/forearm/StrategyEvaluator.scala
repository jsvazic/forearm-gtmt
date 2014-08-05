/**
 * Copyright (c) John Svazic, 2008-2014 - All Rights Reserved
 */
package com.arm.forearm

import scala.math.{ abs, round }
import com.arm.forearm.indicator.{ TechnicalIndicator, RSI }

import org.slf4j.LoggerFactory
import java.text.NumberFormat

sealed trait Action
case object Stay extends Action
case object Buy extends Action
case object Sell extends Action

class StrategyEvaluator

object StrategyEvaluator {
  private val minDelta = ForearmSettings.MOVING_AVERAGE_MIN_DELTA
  private val minRSI = ForearmSettings.MIN_RSI_VALUE
  private val maxRSI = ForearmSettings.MAX_RSI_VALUE
  private val slPips = ForearmSettings.SL_PIPS
  private val tpPips = ForearmSettings.TP_PIPS
  private val nf = NumberFormat.getNumberInstance
  private val maNF = NumberFormat.getNumberInstance
  private val log = LoggerFactory.getLogger(classOf[StrategyEvaluator])

  nf.setMinimumFractionDigits(2)
  nf.setMaximumFractionDigits(2)
  maNF.setMinimumFractionDigits(5)
  maNF.setMaximumFractionDigits(5)
  
  def evaluate(client: FXLiveAgent,
               pair: ForexPair, 
               candles: Vector[Candle],
               fast: TechnicalIndicator,
               slow: TechnicalIndicator,
               rsi: RSI,
               lastAction: Action): Tuple2[Action, Double] = {

    require(candles.size > 0)
    
    val candle = candles.last
    val fastVal = fast.eval(candles)
    val slowVal = slow.eval(candles)
    val rsiVal = rsi.eval(candles)
    val delta = abs(fastVal - slowVal) * pair.multiplier
    
    // Close any open orders based on the last candle value.
    /*
    val earlyClosePL: Double = lastAction match {
      case Buy if (candle.close < slowVal) => client.openOrder(pair) match {
        case Some(order) => client.close(order.id)
        case _ => 0.0
      }
      case Sell if (candle.close > slowVal) => client.openOrder(pair) match {
        case Some(order) => client.close(order.id)
        case _ => 0.0
      }
      case _ => 0.0
    }
    */
    
    log.info(s"\nDetermining action for ${pair.toString} based on:" + 
        s"\n\t${candle.toString}" +
        s"\n\t${fast.toString} = ${maNF.format(fastVal)}" +
        s"\n\t${slow.toString} = ${maNF.format(slowVal)}" +
        (if (fastVal > slowVal) "\n\tfast > slow" else "\n\tfast < slow") +
        s"\n\tDelta: ${maNF.format(delta)} >= $minDelta -> ${delta >= minDelta}" +
        s"\n\tRSI  : " + (if (fastVal > slowVal) s"50.0 < ${nf.format(rsiVal)} < $maxRSI" else s"$minRSI < ${nf.format(rsiVal)} < 50.0"))
        
    val action = determineAction(pair, candle, fastVal, slowVal, rsiVal)
    log.info(s"\n\tDetermined action: $action\n")
    
    if (action != Stay && action != lastAction) {
      log.info(s"*** Last action: $lastAction != $action - Executing! ***")
      // Close any open orders for the same pair as this one.
      val pl = client.openOrder(pair) match {
        case Some(order) => client.close(order.id)
        case None => 0.0 // Do nothing.
      }

      // Calculate the number of units for the order
      val availableUnits = client.getMaxAvailableUnits(pair, candle.close)
      val orderUnits = ForearmSettings.ORDER_UNIT_SIZE 

      // Execute a new order
      action match {
        // For GA evaluation, we do not have a SL/TP trigger.
        case Buy => client.execute(pair, LongOrder, orderUnits, slPips, tpPips)
        case Sell => client.execute(pair, ShortOrder, orderUnits, slPips, tpPips)
      }
      
      return (action, pl)
    }
    
    (action, 0.0)
  }
  
  def determineAction(pair: ForexPair, candle: Candle, fastVal: Double, slowVal: Double, rsiVal: Double) = {
    val delta = abs(fastVal - slowVal) * pair.multiplier

    if (slowVal == 0.0 || fastVal == 0.0) Stay
    else {
      if (delta < minDelta) Stay
      else {
        if (fastVal > slowVal && (rsiVal > 50.0 && rsiVal < maxRSI)) Buy
        else if (fastVal < slowVal && (rsiVal > minRSI && rsiVal < 50.0)) Sell
        else Stay
      }
    }
  }
}
