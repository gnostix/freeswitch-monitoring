package gr.gnostix.freeswitch.utilities

import java.io.File

import gr.gnostix.freeswitch.actors.ServletProtocol.{ApiReplyError, ApiResponse, ApiReplyData, ApiReply}
import org.scalatra.servlet.FileItem
import org.slf4j.LoggerFactory

/**
 * Created by rebel on 10/10/15.
 */
object FileUtilities {

  val log =  LoggerFactory.getLogger(getClass)

  def processCsvFileItem(fileName: String, file: FileItem): ApiResponse = {
    var dialCodesMap = scala.collection.SortedMap.empty[String, String]
    val csv = io.Source.createBufferedSource(file.getInputStream)
    val (itr1, itr2) = csv.getLines().duplicate
    val chkFile = checkFile(itr1)

    chkFile.size match {
      case 0 =>
        // proccess file  country,code
        itr2.toList.map {
          l => val cols = if (l.contains(";")) l.split(";").map(_.trim) else l.split(",").map(_.trim)
            dialCodesMap += (cols(1) -> cols(0))
        }
        csv.close()
        ApiReplyData(200, "File uploaded successfully", Map(fileName -> dialCodesMap))
      case _ => // errors in file
        csv.close()
        ApiReplyError(400, s"Error in lines: ${chkFile.mkString}")
    }

  }

  def processResourcesCsvFile(): ApiResponse = {
    val csv = io.Source.fromURL(getClass.getResource("/dialcodes.csv")) //io.Source.fromFile("resources/dialcodes.csv")
    var dialCodesMap = scala.collection.SortedMap.empty[String, String]
    val (itr1, itr2) = csv.getLines().duplicate
    val chkFile = checkFile(itr1)    //log.debug("----------> " + csv.getLines().size)

    chkFile.size match {
      case 0 =>
        // proccess file  country,code
        itr2.toList.map {
          l => val cols = if (l.contains(";")) l.split(";").map(_.trim) else l.split(",").map(_.trim)
            dialCodesMap += (cols(1) -> cols(0))
        }
        csv.close()
        ApiReplyData(200, "File resources/dialcodes.csv uploaded successfully", Map("default" -> dialCodesMap))
      case _ => // errors in file
        csv.close()
        ApiReplyError(400, s"Error uploading the default dialcodes.csv file from resources in lines: ${chkFile.mkString}")
    }

  }
  
  private def checkFile(itr: Iterator[String]) = {
    // country,code
    var errorLines: List[Int] = List()
    itr.toList.zipWithIndex.map {
      case (l, i) => val cols = if (l.contains(";")) l.split(";").map(_.trim) else l.split(",").map(_.trim)
        try {
          cols(1).toLong
        } catch {
          case e: Exception => errorLines ::= i + 1
        }
    }

    errorLines.reverse
  }
}
