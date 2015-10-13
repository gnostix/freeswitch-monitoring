package gr.gnostix.freeswitch.utilities

import gr.gnostix.freeswitch.model.{CompletedCallStatsByCountryAcdRtpQuality, CompletedCallStatsByCountry}

import scala.util.Random

/**
 * Created by rebel on 13/1/15.
 */
object HelperFunctions {

  def sortAcdByCountry(li: List[Option[CompletedCallStatsByCountry]]) ={
    li.flatten.groupBy(_.country).map{
      case (c,v) =>
        CompletedCallStatsByCountryAcdRtpQuality(c.get, v.head.prefix.get, v.map(_.acd).sum / v.size,
          v.map(_.rtpQuality).sum / v.size, v.size)
    }
  }.toList

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
