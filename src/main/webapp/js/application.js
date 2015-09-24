$(function () {
    "use strict";

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
        //url: "ws://fs-moni.cloudapp.net:8080/fs-moni/live/events",
        url: "/fs-moni/live/events",
        //url: "the-chat",
        //url: "ws://localhost:8080/fs-moni/live/events",
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

        console.log("onMessage");
        var message = rs.responseBody;
        console.log("MESSAGE:::::" + message);
        try {
            var json = jQuery.parseJSON(message);
            console.log("Got a message");

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
            var cpuidle = chartCPU.get('cpuidle');

            //console.log(json);
            console.log("EVENTNAME:::::" + json.eventName);

            if (json.eventName === "HEARTBEAT") {
                $("#upTime").text(convertMillisecondsToDigitalClock(json.uptimeMsec).clock);
                $("#sessionPerSecond").text(json.sessionPerSecond);
                $("#cpuIdle").text(json.idleCPU);
                // Add points to graphs
                cpuidle.addPoint([Date.parse(json.eventDateTimestamp), json.idleCPU]);

            } else if (json.eventName === "BASIC_STATS") {
                console.log("--------------ADDING BASIC STATS");
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
        console.log(str);

        var hours = Math.floor(ms / 3600000); // 1 Hour = 36000 Milliseconds
        var minutes = Math.floor((ms % 3600000) / 60000); // 1 Minutes = 60000 Milliseconds
        var seconds = Math.floor(((ms % 360000) % 60000) / 1000);// 1 Second = 1000 Milliseconds
        console.log("hours" + hours);
        return {
            hours: hours,
            minutes: minutes,
            seconds: seconds,
            // clock: hours + ":" + minutes + ":" + seconds
            clock: str
        };
    }

});
