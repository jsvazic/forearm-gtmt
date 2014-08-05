/**
 * Copyright (c) John Svazic, 2008-2014 - All Rights Reserved
 */
package com.arm.forearm
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.DateTimeConstants.{ SATURDAY, SUNDAY }
import org.quartz.{ DisallowConcurrentExecution, Job, JobExecutionContext, PersistJobDataAfterExecution }
import org.slf4j.LoggerFactory
import com.arm.forearm.indicator.{ EMA, SMA, RSI }

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
class ForearmJob extends Job {
  import ForearmJob._ // Import the "static" constants

  override def execute(ctx: JobExecutionContext) {
    val dataMap     = ctx.getJobDetail.getJobDataMap
    val interval    = dataMap.get("fx.interval").asInstanceOf[Interval]
    val agent       = dataMap.get("fx.agent").asInstanceOf[FXLiveAgent]
    val pairs       = dataMap.get("fx.pairs").asInstanceOf[List[ForexPair]]
    var lastActions = dataMap.get("fx.last.actions").asInstanceOf[Map[ForexPair, Action]]

   	if (checkMarket(new DateTime().withZone(DateTimeZone.forID("Australia/Sydney")), 8, 16) ||
   		checkMarket(new DateTime().withZone(DateTimeZone.forID("Asia/Tokyo")), 8, 16) ||
   		checkMarket(new DateTime().withZone(DateTimeZone.forID("Europe/London")), 8, 16) ||
   		checkMarket(new DateTime().withZone(DateTimeZone.forID("America/New_York")), 8, 16)) {

      var pairCandleMap = Map[ForexPair, Vector[Candle]]()
   	  // Get all the history
   	  for (pair <- pairs) {
        pairCandleMap += (pair -> agent.history(pair, interval, 150))
   	  }
      
   	  // Evaluate each pair separately.
   	  for (pair <- pairs) {
   	    // TODO: Clean up this logic, just passing in options instead of nulls
   	    process(pair, lastActions.get(pair).orNull, pairCandleMap.get(pair).orNull, agent) match {
   	      case Some(action) => lastActions += (pair -> action)
   	      case None => // Do nothing - either no new action has been set or there was an error.
   	    }
   	  }
   	  
   	  // Update the action map
   	  dataMap.put("fx.last.actions", lastActions)
    }
  }
  
  private def process(pair: ForexPair, lastAction: Action, candles: Vector[Candle], agent: FXLiveAgent): Option[Action] = {
    if (lastAction == null || candles == null) {
      log.error(s"$lastAction == null or $candles == null")
      return None
    }
    
    try {
      // Get some data to evaluate and decide what to do next.
      agent.quote(pair) match {
        case None => log.error(s"Failed to retrieve a quote for: $pair")
        case Some(quote) => {
          // Take the last 150 candles to evaluate.  This is helpful for calculating the EMA and RSI.
          val actionPLTuple = StrategyEvaluator.evaluate(agent, pair, candles, 
              EMA(ForearmSettings.EMA_PERIODS), 
              SMA(ForearmSettings.SMA_PERIODS), 
              RSI(ForearmSettings.RSI_PERIODS), 
              lastAction)
                
          if (actionPLTuple._1 != Stay && actionPLTuple._1 != lastAction) {
            return Some(actionPLTuple._1)
          }
        }
      }
    } catch {
      case e: Exception => log.error(s"Failed to process the pair $pair!", e)
    }
    
    None
  }
}

object ForearmJob {
  private val log = LoggerFactory.getLogger(classOf[ForearmJob])

  private def checkMarket(dt: DateTime, openingHour: Int, closingHour: Int) = {
    val dayOfWeek = dt.getDayOfWeek
    val hourOfDay = dt.getHourOfDay
    dayOfWeek != SATURDAY && dayOfWeek != SUNDAY && hourOfDay >= openingHour && hourOfDay <= closingHour
  }
}