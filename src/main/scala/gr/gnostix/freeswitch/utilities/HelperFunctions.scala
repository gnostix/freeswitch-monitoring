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

import gr.gnostix.freeswitch.actors.CallEnd
import gr.gnostix.freeswitch.model.{CompletedCallStatsByCountryAsr, CompletedCallStatsByCountryAcdRtpQuality, CompletedCallStatsByCountry}

import scala.util.Random

/**
 * Created by rebel on 13/1/15.
 */
object HelperFunctions {

  def sortAcdByCountry(li: List[Option[CompletedCallStatsByCountry]]) ={
    li.flatten.groupBy(_.country).map{
      case (c,v) =>
        CompletedCallStatsByCountryAcdRtpQuality(v.head.prefix.get, c.get,
          BigDecimal(v.map(_.billSec).sum.toDouble / v.size ).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble,
          BigDecimal(v.map(_.rtpQuality).sum.toDouble / v.size ).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble, v.size)
    }
  }.toList //BigDecimal( ).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

  def getAsrByCountry(compCallsStats: List[Option[CompletedCallStatsByCountry]], failedCalls: List[CallEnd]) = {
    compCallsStats.flatten.groupBy(_.country).map{
      case (x,y) =>
        val fCallsSizeByCountry = failedCalls.filter(_.country == x).size

        fCallsSizeByCountry match {
          case 0 =>
            // if the failed calls to this destination, is 0 then the ASR is 100%
            CompletedCallStatsByCountryAsr(y.head.prefix.get, x.get, y.size, fCallsSizeByCountry,100)

          case _ => CompletedCallStatsByCountryAsr(y.head.prefix.get, x.get, y.size, fCallsSizeByCountry,
            BigDecimal(y.size.toDouble / (fCallsSizeByCountry + y.size) * 100).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)
        }

    }
  }

  def randomAlphaNumericString(length: Int): String = {
    val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')
    randomStringFromCharList(length, chars)
  }

  private def randomStringFromCharList(length: Int, chars: Seq[Char]): String = {
    val sb = new StringBuilder
    for (i <- 1 to length) {
      val randomNum = new Random().nextInt(chars.length)
      sb.append(chars(randomNum))
    }
    sb.toString
  }

  // create a md5 hash from a string
  def sha1Hash(text: String): String = java.security.MessageDigest.getInstance("SHA").digest(text.getBytes()).map(0xFF & _).map {
    "%02x".format(_)
  }.foldLeft("") {
    _ + _
  }


  def doublePrecision1(num: Double): Double = {
    BigDecimal(num).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
}
