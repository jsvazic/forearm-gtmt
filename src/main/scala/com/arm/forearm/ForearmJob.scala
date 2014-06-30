/**
 * Copyright (c) John Svazic, 2008-2014 - All Rights Reserved
 */
package com.arm.forearm
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.DateTimeConstants.{ SATURDAY, SUNDAY }
import org.quartz.{DisallowConcurrentExecution, Job, JobExecutionContext, PersistJobDataAfterExecution}
import org.slf4j.LoggerFactory
import com.arm.forearm.indicator.{ EMA, SMA, RSI }

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
class ForearmJob extends Job {
  import ForearmJob._ // Import the "static" constants

  override def execute(ctx: JobExecutionContext) {
    val dt = new DateTime
    val dataMap = ctx.getJobDetail.getJobDataMap
    val agent = dataMap.get("fx.agent").asInstanceOf[FXLiveAgent]
    val pair = dataMap.get("fx.pair").asInstanceOf[ForexPair]
    val evalInterval = dataMap.get("fx.eval.interval").asInstanceOf[Interval]
    val trainingInterval = dataMap.get("fx.training.interval").asInstanceOf[Interval]

   	if (checkMarket(new DateTime().withZone(DateTimeZone.forID("Australia/Sydney")), 8, 16) ||
   		checkMarket(new DateTime().withZone(DateTimeZone.forID("Asia/Tokyo")), 8, 16) ||
   		checkMarket(new DateTime().withZone(DateTimeZone.forID("Europe/London")), 8, 16) ||
   		checkMarket(new DateTime().withZone(DateTimeZone.forID("America/New_York")), 8, 16)) {

      try {
        // Get some data to evaluate and decide what to do next.
        val agent: FXLiveAgent = dataMap.get("fx.agent").asInstanceOf[FXLiveAgent]
        val pair = dataMap.get("fx.pair").asInstanceOf[ForexPair]
        agent.quote(pair) match {
          case None => log.error(s"Failed to retrieve a quote for: $pair")
          case Some(quote) => {
            // Take the last 100 candles to evaluate.  This is helpful for calculating the EMA and RSI.
            val candles = agent.history(pair, evalInterval, 100)
            val lastAction = dataMap.get("fx.last.action").asInstanceOf[Action]
            val actionPLTuple = StrategyEvaluator.evaluate(agent, pair, candles, 
                EMA(ForearmSettings.EMA_PERIODS), 
                SMA(ForearmSettings.SMA_PERIODS), 
                RSI(ForearmSettings.RSI_PERIODS), 
                lastAction)
                
            if (actionPLTuple._1 != Stay && actionPLTuple._1 != lastAction) {
              dataMap.put("fx.last.action", actionPLTuple._1)
            }
          }
        }
      } catch {
        case e: Exception => log.error("Failed to execute the job!", e)
      }
    }
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