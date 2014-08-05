/**
 * Copyright (c) John Svazic, 2008-2014 - All Rights Reserved 
 */
package com.arm.forearm.indicator

import java.util.concurrent.TimeUnit
import scala.math.{ max, Ordering }
import com.arm.forearm.Candle

sealed trait TechnicalIndicator {
  val periods: Int
  def eval(candles: Vector[Candle]): Double
}

case class EMA(periods: Int) extends TechnicalIndicator {
  private val sf = 2.0 / (periods + 1.0)
  private val sfd = 1.0 - sf
  
  def eval(candles: Vector[Candle]): Double = {
    if (candles.size < periods) 0.0
    else {
      val ema: Double = (0.0 /: candles.take(periods))(_ + _.close) / periods
      (ema /: candles.drop(periods))((lastEMA: Double, c: Candle) => (sf * c.close) + (sfd * lastEMA))
    }
  }
  
  def evalDouble(closeVals: Vector[Double]): Double = {
    if (closeVals.size < periods) 0.0
    else {
      val ema: Double = (0.0 /: closeVals.take(periods))(_ + _) / periods
      (ema /: closeVals.drop(periods))((lastEMA: Double, close: Double) => (sf * close) + (sfd * lastEMA))
    }
  }
}

case class SMA(periods: Int) extends TechnicalIndicator {
  def eval(candles: Vector[Candle]): Double = {
    if (candles.size < periods) 0.0
    else (0.0 /: candles.takeRight(periods))(_+_.close) / periods
  }

  def evalDouble(closeVals: Vector[Double]): Double = {
    if (closeVals.size < periods) 0.0
    else (0.0 /: closeVals.take(periods))(_+_) / periods
  }
}

case class WMA(periods: Int) extends TechnicalIndicator {
  private val denom = (periods * (periods + 1)) / 2.0

  def eval(candles: Vector[Candle]): Double = {
    if (candles.size < periods) 0.0
    else (0.0 /: candles.takeRight(periods).zip(1 to periods))((v: Double, t: Tuple2[Candle, Int]) => v + (t._1.close * (t._2 / denom)))
  }
}

case class RSI(periods: Int) extends TechnicalIndicator {
  private def calcRSI(candles: Vector[Candle]): Double = {
    if (candles.size < periods) 0.0
    else {
      val initialAvgGain: Double = (0.0 /: candles.take(periods).map(gain(_)))(_+_) / periods
      val initialAvgLoss: Double = (0.0 /: candles.take(periods).map(loss(_)))(_+_) / periods
      
      val aGain = (initialAvgGain /: candles.drop(periods))((prev, candle) => ((prev * (periods - 1)) + gain(candle)) / periods)
      val aLoss = (initialAvgLoss /: candles.drop(periods))((prev, candle) => ((prev * (periods - 1)) + loss(candle)) / periods)
      
      100 - (100 / (1 + (aGain / aLoss)))
    }
  }

  def eval(candles: Vector[Candle]): Double = {
    if (candles.size < periods) 0.0
    else calcRSI(candles)
  }
  
  private def gain(candle: Candle): Double = max(0, candle.close - candle.open)
  private def loss(candle: Candle): Double = max(0, candle.open - candle.close)
}
