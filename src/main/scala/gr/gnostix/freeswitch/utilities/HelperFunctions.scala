package gr.gnostix.freeswitch.utilities

import scala.util.Random

/**
 * Created by rebel on 13/1/15.
 */
object HelperFunctions {


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
