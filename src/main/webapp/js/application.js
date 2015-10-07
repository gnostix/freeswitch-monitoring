$(function () {
    "use strict";
	
	//paths for local tests and server
	//var path='http://fs-moni.cloudapp.net:8080';
	var path='';
	
    var detect = $("#detect");
    var header = $('#header');
    var messages = $('#messages');
    var input = $('#input');
    var inputText = $("#intxt");
    var status = $('#status');
    var myName = true;
    var author = "Alex";
    var logged = true;
    var socket = $.atmosphere;
    var subSocket;
//  var transport = 'long-polling';
    var transport = 'websocket';


    var request = {

      //  url: "ws://fs-moni.cloudapp.net:8080/fs-moni/live/events",
        url: "/fs-moni/live/events",
        //url: "the-chat",
        contentType: "application/json",
        logLevel: 'debug',
        transport: transport,
        fallbackTransport: 'long-polling'
    };

    request.onOpen = function (response) {
        console.log("onOpen");
        addSysMessage('Atmosphere connected using ' + response.transport);

        transport = response.transport;
        if (response.transport == "local") {
            console.log("----> local");
            subSocket.pushLocal("Name?");
        }
    };

    request.onReconnect = function (rq, rs) {
        console.log("onReconnect");
        socket.info("Reconnecting");
    };


    request.onMessage = function (rs) {

     //   console.log("onMessage");
        var message = rs.responseBody;
       // console.log("MESSAGE:::::" + message);
        try {
            var json = jQuery.parseJSON(message);
         //   console.log("Got a message");

            //get the chart by id
            var chart = $('#heartbeat').highcharts();
            //get series by id
            var asr = chart.get('asr');
            var acd = chart.get('acd');
            var rtp = chart.get('rtp');

            //get the chart by id
            var chartBasic = $('#basicstats').highcharts();
            //get series by id
            var concurrentCalls = chartBasic.get('concurrentCalls');
            var failedCalls = chartBasic.get('failedCalls');

            //get the chart by id
            var chartCPU = $('#cpu').highcharts();
            //get series by id
            var cpuUsage = chartCPU.get('cpuUsage');

            //console.log(json);
           // console.log("EVENTNAME:::::" + json.eventName);

            //console.log(json);
          //  console.log("EVENTNAME:::::" + json.eventName);

            if (json.eventName === "HEARTBEAT") {
                $("#upTime").text(convertMillisecondsToDigitalClock(json.uptimeMsec).clock);
                $("#sessionPerSecond").text(json.sessionPerSecond);
                $("#cpuUsage").text(json.cpuUsage + '%');
                // Add points to graphs
                cpuUsage.addPoint([Date.parse(json.eventDateTimestamp), json.cpuUsage]);

            } else if (json.eventName === "BASIC_STATS") {
               // console.log("--------------ADDING BASIC STATS");
                $("#concCallsNum").text(json.concCallsNum);
                $("#failedCallsNum").text(json.failedCallsNum);
                $("#acd").text(json.acd);
                $("#asr").text(json.asr);
                $("#rtpQualityAvg").text(json.rtpQualityAvg);

                // Add points to graphs
                asr.addPoint([Date.parse(json.dateTime), json.asr]);
                acd.addPoint([Date.parse(json.dateTime), json.acd]);
                rtp.addPoint([Date.parse(json.dateTime), json.rtpQualityAvg]);

                //add points to graph
                //var milliSeconds = Date.parse(json.dateTime);
                //console.log("adding basic:"+milliSeconds);
                concurrentCalls.addPoint([Date.parse(json.dateTime), json.concCallsNum]);
                failedCalls.addPoint([Date.parse(json.dateTime), json.failedCallsNum]);
            } else if (json.eventName === "CHANNEL_HANGUP_COMPLETE") {
                var currentValue = $("#concCallsNum").text();
                var newValue = parseInt(currentValue) - 1;
                $("#concCallsNum").text(newValue);
            } else if (json.eventName === "CHANNEL_ANSWER") {
                var currentValue = $("#concCallsNum").text();
                var newValue = parseInt(currentValue) + 1;
                $("#concCallsNum").text(newValue);
            } else if (json.eventName === "FAILED_CALL") {
                var currentValue = $("#failedCallsNum").text();
                var newValue = parseInt(currentValue) + 1;
                $("#failedCallsNum").text(newValue);
            }


        } catch (e) {
            console.log('This doesn\'t look like a valid JSON object: ', message.data);
            //console.log('e:::::::::::::: ', e);
            return;
        }
    };

    request.onClose = function (rs) {
        console.log("onClose");
    };

    request.onError = function (rs) {
        console.log("onError");
        messages
            .add('li')
            .addClass('list-group-item')
            .text('Sorry, but there\'s some problem with your socket or the server is down');
    };

    //socket.unsubscribe();
    console.log("socket.subscribe(request)");
    subSocket = socket.subscribe(request);


    function addSysMessage(msg) {
        //console.log("addSysMessage");
        addMessage(
            {
                type: 'info',
                text: 'System'
            },
            msg
        );
    }

    function addMessage(label, msg, datetime) {
        //console.log("aaddMessage");
        if (datetime == null) {
            datetime = new Date();
        }
        var time = (datetime.getHours() < 10 ? '0' + datetime.getHours() : datetime.getHours()) + ':' + (datetime.getMinutes() < 10 ? '0' + datetime.getMinutes() : datetime.getMinutes());
        if (label != null) {
            messages
                .append("<li class='list-group-item'><span class='label label-" + label.type + "'>" + label.text + "</span> [" + time + "]: " + msg + "</li>");
        } else {
            messages
                .append("<li class='list-group-item'>[" + time + "]: " + msg + "</li>");
        }
    }


    // CONVERT MILLISECONDS TO DIGITAL CLOCK FORMAT
    function convertMillisecondsToDigitalClock(ms) {
        var date = new Date(ms);
        var str = '';
        str += date.getUTCDate() - 1 + " days, ";
        str += date.getUTCHours() + " hours, ";
        str += date.getUTCMinutes() + " minutes, ";
        str += date.getUTCSeconds() + " seconds ";
//str += date.getUTCMilliseconds() + " millis";
       // console.log(str);

        var hours = Math.floor(ms / 3600000); // 1 Hour = 36000 Milliseconds
        var minutes = Math.floor((ms % 3600000) / 60000); // 1 Minutes = 60000 Milliseconds
        var seconds = Math.floor(((ms % 360000) % 60000) / 1000);// 1 Second = 1000 Milliseconds
       // console.log("hours" + hours);
        return {
            hours: hours,
            minutes: minutes,
            seconds: seconds,
            // clock: hours + ":" + minutes + ":" + seconds
            clock: str
        };
    }

    // CONVERT MILLISECONDS TO custom format
    function convertMillisecondsToCustomFormat(ms) {
        var date = new Date(ms);
        var str = date.getUTCFullYear() + '-' +
            (date.getUTCMonth() + 1) + '-' +
            (date.getUTCDate()) + ' ' +
            addZero(date.getUTCHours()) + ':' +
            addZero(date.getUTCMinutes()) + ':' +
            addZero(date.getUTCSeconds()) + ' ';
       // console.log(str);

        var hours = Math.floor(ms / 3600000); // 1 Hour = 36000 Milliseconds
        var minutes = Math.floor((ms % 3600000) / 60000); // 1 Minutes = 60000 Milliseconds
        var seconds = Math.floor(((ms % 360000) % 60000) / 1000);// 1 Second = 1000 Milliseconds
     //   console.log("hours" + hours);
        return {
            hours: hours,
            minutes: minutes,
            seconds: seconds,
            // clock: hours + ":" + minutes + ":" + seconds
            clock: str
        };
    }

    function addZero(i) {
        if (i < 10) {
            i = "0" + i;
        }
        return i;
    }

    

    $('#viewCCalls').click(function () {
        var table = $('#example').DataTable();

        table.clear().draw();

        getDetails(table);
        //open dialog
        $('#dialog').dialog('open');
    });


    $('#viewFCalls').click(function () {
        var table = $('#failedcallstable').DataTable();

        table.clear().draw();

        getFailedCallsDetails(table);
        //open dialog
        $('#failedcallsdialog').dialog('open');

    });
	
	$('#viewASR').click(function () {
        var table = $('#asracdstable').DataTable();

        table.clear().draw();

        getASRACDDetails(table);
        //open dialog
        $('#asracddialog').dialog('open');

    });
	
	$('#viewACD').click(function () {
        var table = $('#asracdstable').DataTable();

        table.clear().draw();

        getASRACDDetails(table);
        //open dialog
        $('#asracddialog').dialog('open');

    });

	 
	 /*
	 $('#viewACD').click(function () {
       // var table = $('#asracdstable').DataTable( {
		//"deferRender": true
	  //	} );
		$('#asracdstableP').DataTable({
        "ajax": {
		"url":"http://fs-moni.cloudapp.net:8080/actors/completed/calls/details",
        "dataSrc": "payload",
		"columns": [
            { "data": "readCodec" },
            { "data": "fromUser" },
            { "data": "eventName" }
        ]}
		});

       // table.clear().draw();

       // getASRACDDetails(table);
        //open dialog
       // $('#asracddialog').dialog('open');

      });
	  */

	
    jQuery(document).ready(function () {

           //init results and charts
	        $.ajax({
            type: 'GET', // define the type of HTTP verb we want to use (POST for our form)
           // url: '/actors/initialize/dashboard', // the url where we want to POST
             url: path+'/actors/initialize/dashboard/heartbeat',
            
            dataType: 'json', // what type of data do we expect back from the server
            contentType: "application/json",
            encode: true
            })
            // using the done promise callback
            .done(function (result) {
			//console.log("SUCCESS:__________________ "+JSON.stringify(result));
             
			

            //get the chart by id
            var chartCPU = $('#cpu').highcharts();
            //get series by id
            var cpuUsage = chartCPU.get('cpuUsage');

           
               
				 if (result.payload.length > 0) {
	
					//add last value on container
					 $("#upTime").text(convertMillisecondsToDigitalClock(result.payload[0].uptimeMsec).clock);
					 $("#sessionPerSecond").text(result.payload[0].sessionPerSecond);
					 $("#cpuUsage").text(result.payload[0].cpuUsage + '%');
					 
                    $.each(result.payload, function (i, n) {
                       //  console.log("Sensor Index: " + i + ", cpu usage: " + n.cpuUsage );

                        // for (var i = 0; i < n.length ; i++) {
                        // alert(n.uuid + " - ---- -- ");
                        cpuUsage.addPoint([Date.parse(n.eventDateTimestamp), n.cpuUsage]);
				
                        // }

                    });
                }

            })
            // using the fail promise callback
            .fail(function (data) {
                // show any errors
                // best to remove for production
                
                console.log("on fail " + JSON.stringify(data));
            });
			
			
			//basicstats
			 $.ajax({
            type: 'GET', // define the type of HTTP verb we want to use (POST for our form)
           // url: '/actors/initialize/dashboard', // the url where we want to POST
             url: path+'/actors/initialize/dashboard/basicstats',
            
            dataType: 'json', // what type of data do we expect back from the server
            contentType: "application/json",
            encode: true
            })
            // using the done promise callback
            .done(function (result) {
			//console.log("SUCCESS:__________________ "+JSON.stringify(result));
             //get the chart by id
            var chartBasic = $('#basicstats').highcharts();
			 
			 //get the chart by id
            var chart = $('#heartbeat').highcharts();
            //get series by id
            var asr = chart.get('asr');
            var acd = chart.get('acd');
            var rtp = chart.get('rtp');

           
            //get series by id
            var concurrentCalls = chartBasic.get('concurrentCalls');
            var failedCalls = chartBasic.get('failedCalls');

              
			 if (result.payload.length > 0) {
				$("#concCallsNum").text(result.payload[0].concCallsNum);
				$("#failedCallsNum").text(result.payload[0].failedCallsNum);
				$("#acd").text(result.payload[0].acd);
				$("#asr").text(result.payload[0].asr);
				$("#rtpQualityAvg").text(result.payload[0].rtpQualityAvg);

                    $.each(result.payload, function (i, n) {
                        // console.log("Sensor Index: " + i + ", cpu usage: " + n.cpuUsage );

                     
						// Add points to graphs
						rtp.addPoint([Date.parse(n.dateTime), n.rtpQualityAvg]);
						asr.addPoint([Date.parse(n.dateTime), n.asr]);
						acd.addPoint([Date.parse(n.dateTime), n.acd]);
						
						//add points to graph
						//var milliSeconds = Date.parse(json.dateTime);
						//console.log("adding basic:"+milliSeconds);
						concurrentCalls.addPoint([Date.parse(n.dateTime), n.concCallsNum]);
						failedCalls.addPoint([Date.parse(n.dateTime), n.failedCallsNum]);

                    });
                }

            })
            // using the fail promise callback
            .fail(function (data) {
                // show any errors
                // best to remove for production
                
                console.log("on fail " + JSON.stringify(data));
            });
			
	  
		/*
		//using datatable custo columns
		$('#asracdstableP').DataTable({
         "serverSide": true,
        "ajax": {
		"url":"http://fs-moni.cloudapp.net:8080/actors/completed/calls/details",
        "dataSrc": function (json) {
			//  alert(json);
			
			var return_data = new Array();
                    $.each(json.payload, function (i, n) {
                       //  console.log("Sensor Index: " + i + ", Sensor Name: " + n.uuid );

                      return_data.push({
				'readCodec': n.readCodec
				})
                    });
			
			var return_data = new Array();
			for(var i=0;i< json.payload.length; i++){
				return_data.push({
				'readCodec': json.payload[i].readCodec,
				'fromUser'  :json.payload[i].fromUser,
				'eventName' : json.payload[i].eventName
				})
			 }
			//  console.log("return_data: " +JSON.stringify(return_data));
			 return return_data;
			}
		 // "dataSrc": "payload",
		"columns": [
            { "data": "readCodec" },
            { "data": "fromUser" },
            { "data": "eventName" }
        ]
		}
		});
		*/
		 // dialog properties
         $("#dialog").dialog({
            minWidth: 800,
            maxHeight: 600,
            autoOpen: false,
            modal: true
         });

         $("#failedcallsdialog").dialog({
            minWidth: 900,
            maxHeight: 600,
            autoOpen: false,
            modal: true
         });
		
		 $("#asracddialog").dialog({
            minWidth: 1000,
            maxHeight: 600,
            autoOpen: false,
            modal: true
         });

    });


    function getDetails(table) {
        console.log("getDetails --------------------------------");
        var pop = "";
        var message = "";
        var formData = {
            "ip": ""
        };
        $.ajax({
            type: 'GET', // define the type of HTTP verb we want to use (POST for our form)
           // url: '/actors/concurrent/calls/details', // the url where we want to POST
             url: path+'/actors/concurrent/calls/details', // the url where we want to POST
           // url: "ws://fs-moni.cloudapp.net:8080/actors/concurrent/calls/details",
            // url: 'http://10.5.50.249:8080/actors/concurrent/calls/details', // the url where we want to POST
            //data: JSON.stringify(formData), // our data object
            dataType: 'json', // what type of data do we expect back from the server
            contentType: "application/json",
            encode: true
        })
            // using the done promise callback
            .done(function (result) {


                // log data to the console so we can see
                //console.log("SUCCESS:__________________ "+JSON.stringify(result));
                //alert(result.payload.length);
                // message = result.message;


                //console.log("on message " + message);
                //$( "div.popcontainer" )
                //.html( pop );
                if (result.payload.length > 0) {


                    $.each(result.payload, function (i, n) {
                        // alert("Sensor Index: " + i + ", Sensor Name: " + n.uuid );

                        // for (var i = 0; i < n.length ; i++) {
                        // alert(n.uuid + " - ---- -- ");
                        // $('#example > tbody:last').append('<tr><td>'+n.uuid+'</td>' +
                        // '<td>'+n.fromUser+'</td>' +
                        // '<td>'+n.toUser+'</td></tr>');
                        // }
                        table.row.add([n.fromUser, n.toUser, n.fromUserIP,n.toUserIP, convertMillisecondsToCustomFormat(Date.parse(n.callerChannelAnsweredTime)).clock, n.freeSWITCHHostname]).draw();

                    });
                }


                /*
                 $(jQuery.parseJSON(JSON.stringify(data))).each(function() {

                 //  console.log(this.ip + " - ---- -- " + this.port);
                 $('#tableC > tbody:last').append('<tr><td>'+this.ip+'</td>' +
                 '<td>'+this.port+'</td>' +
                 '<td>'+JSON.stringify(data)+'</td></tr>');
                 });
                 */
                //return pop;
                //return message;

                //make table's pagination and style
                //$('#example').DataTable();
            })
            // using the fail promise callback
            .fail(function (data) {
                // show any errors
                // best to remove for production
                pop = "JSON.stringify(data)";
                console.log("on fail " + JSON.stringify(data));
            });


    }


    function getFailedCallsDetails(table) {
        console.log("getFailedCallsDetails --------------------------------");
        var pop = "";
        var message = "";
        var formData = {
            "ip": ""
        };
        $.ajax({
            type: 'GET', // define the type of HTTP verb we want to use (POST for our form)
          //  url: '/actors/failed/calls/details', // the url where we want to POST
            url: path+'/actors/failed/calls/details', // the url where we want to POST
            //url: "ws://fs-moni.cloudapp.net:8080/actors/concurrent/calls/details",
            // url: 'http://10.5.50.249:8080/actors/concurrent/calls/details', // the url where we want to POST
            //data: JSON.stringify(formData), // our data object
            dataType: 'json', // what type of data do we expect back from the server
            contentType: "application/json",
            encode: true
        })
            // using the done promise callback
            .done(function (result) {

                if (result.payload.length > 0) {


                    $.each(result.payload, function (i, n) {
                        table.row.add([n.fromUser, n.toUser, n.fromUserIP, convertMillisecondsToCustomFormat(Date.parse(n.callerChannelHangupTime)).clock, n.hangupCause, n.freeSWITCHHostname]).draw();

                    });
                }

            })
            // using the fail promise callback
            .fail(function (data) {
                // show any errors
                // best to remove for production
                pop = "JSON.stringify(data)";
                console.log("on fail " + JSON.stringify(data));
            });


    }
	
	function getASRACDDetails(table) {
        console.log("getASRACDDetails --------------------------------");
        var pop = "";
        var message = "";
        var formData = {
            "ip": ""
        };
        $.ajax({
            type: 'GET', // define the type of HTTP verb we want to use (POST for our form)

           // url: '/actors/completed/calls/details', // the url where we want to POST
            url: path+'/actors/completed/calls/details', // the url where we want to POST

            dataType: 'json', // what type of data do we expect back from the server
            contentType: "application/json",
            encode: true
        })
            // using the done promise callback
            .done(function (result) {

                if (result.payload.length > 0) {


                    $.each(result.payload, function (i, n) {
                        table.row.add([n.fromUser, n.toUser, n.readCodec, n.writeCodec, n.fromUserIP, n.toUserIP, +
						convertMillisecondsToCustomFormat(Date.parse(n.callerChannelAnsweredTime)).clock,convertMillisecondsToCustomFormat(Date.parse(n.callerChannelHangupTime)).clock, +
						n.freeSWITCHHostname, n.freeSWITCHIPv4, n.hangupCause, n.billSec, n.rtpQualityPerc]).draw();

                    });
                }

            })
            // using the fail promise callback
            .fail(function (data) {
                // show any errors
                // best to remove for production
                pop = "JSON.stringify(data)";
                console.log("on fail " + JSON.stringify(data));
            });


    }


})
;
