package gr.gnostix.freeswitch.model

import java.sql.Timestamp

/**
 * Created by rebel on 13/10/15.
 */
case class CompletedCallStats(acd: Int, rtpQuality: Double, callerChannelHangupTime: Timestamp)
case class CompletedCallStatsByCountry(prefix: Option[String], country: Option[String], acd: Int, rtpQuality: Double, callerChannelHangupTime: Timestamp)
case class CompletedCallStatsByCountryAcdRtpQuality(prefix: String, country: String, acd: Int, rtpQuality: Double, callsNum: Int)

