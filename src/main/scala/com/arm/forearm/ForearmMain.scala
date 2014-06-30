/**
 * Copyright (c) John Svazic, 2008-2014 - All Rights Reserved
 */
package com.arm.forearm

import java.util.TimeZone
import org.joda.time.DateTime
import org.quartz.CronScheduleBuilder._
import org.quartz.DateBuilder._
import org.quartz.JobBuilder._
import org.quartz.TriggerBuilder._
import org.quartz.{ JobDataMap, JobDetail, Trigger }
import org.quartz.impl.StdSchedulerFactory

import org.slf4j.LoggerFactory

import com.arm.forearm.agent.OandaAgent

class ForearmMain

object ForearmMain extends App {
  private val log = LoggerFactory.getLogger(classOf[ForearmMain])
  try {
    log.info("Welcome to Forearm!  Let the good times grow!")
    runLive
  } catch {
    case t: Throwable => log.error("Caught a top-level exception!", t)
  }
  
  private def scheduleBuilder(interval: Interval) = interval match {
    case OneMinute => "0 0/1 * ? * *"
    case FiveMinutes => "0 0/5 * ? * *"
    case FifteenMinutes => "0 0/15 * ? * *"
    case ThirtyMinutes => "0 0/30 * ? * *"
    case OneHour => "0 0 * ? * *"
    case FourHours => "0 0 0/4 ? * *"
    case OneDay => "0 0 0 ? * *"
  }

  private def runLive() {
    val agent = OandaAgent(ForearmSettings.ACCOUNT_ID, ForearmSettings.API_TOKEN)
    val pair = new ForexPair(ForearmSettings.PAIR)
    val evalInterval = ForearmSettings.INTERVAL
    val trainingInterval = ForearmSettings.INTERVAL
    val cronSched = scheduleBuilder(evalInterval)

    val jobDataMap = new JobDataMap
    jobDataMap.put("fx.agent", agent)
    jobDataMap.put("fx.pair", pair)
    jobDataMap.put("fx.eval.interval", evalInterval)
    jobDataMap.put("fx.training.interval", trainingInterval)
    jobDataMap.put("fx.last.action", agent.lastAction(pair))

    val forearmJob = newJob(classOf[ForearmJob])
      .withIdentity("forearmJob", "mainGroup")
      .usingJobData(jobDataMap)
      .build

    val trigger = newTrigger
      .withIdentity("forearmTrigger", "mainGroup")
      .withSchedule(cronSchedule(cronSched))
      .build

    val scheduler = StdSchedulerFactory.getDefaultScheduler
    scheduler.scheduleJob(forearmJob, trigger)
    scheduler.start
  }
}