/*
 * Copyright (c) 2015 Alexandros Pappas p_alx hotmail com
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package gr.gnostix.freeswitch.utilities

import java.sql.Timestamp
import java.util.Date

import org.joda.time.{DateTime, Days}
import org.slf4j.LoggerFactory


object DateUtils {
  val logger = LoggerFactory.getLogger(getClass)

  def findNumberOfDays(fromDate: DateTime, toDate: DateTime): Int = {
    try {
      val days = Days.daysBetween(fromDate, toDate).getDays
      logger.info("-----------------------> number of days between the two dates  " + fromDate + " " + toDate)
      logger.info("-----------------------> number of days between the two dates  " + days)
      days
    } catch {
      case e: Exception => println("-------------- exception in findNumberOfDays")
      0
    }
  }

  def sqlGrouByDateOra(numDays: Int): String = {
    numDays match {
      case 0 => "HH"
      case x if 0 until 31 contains x => "DD"
      case x if 31 until 91 contains x => "ww"
      case x if x >= 91 => "month"
      case _ => "month"
    }
  }

  def sqlGrouByDatePg(numDays: Int): String = {
    numDays match {
      case 0 => "hour"
      case x if 0 until 31 contains x => "day"
      case x if 31 until 91 contains x => "week"
      case x if x >= 91 => "month"
      case _ => "month"
    }
  }


  def checkExpirationDate(expiration: Timestamp): Boolean = {
    if (expiration.after(new Date())) {
      true
    } else {
      false
    }
  }

}
