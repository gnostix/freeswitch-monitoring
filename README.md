# Freeswitch-monitoring
Project to monitor FreeSwitch status and health in Real-Time.


## Description

Freeswitch-monitoring is a distributed application based in the actor-based paradigm. As events are comming from the Freeswitch ESL connection, these messages are routed and processed by the Actor system, keeping in this way the main statistics data ready in memory for fast access. These data are also served in the front client through a WS  or/and an REST API.
The Scala, Scalatra ,Atmosphere and Akka.io are the tools used to achive the goal of this project.



# Design stack

### Application stack

![image](http://vieras.eu/wp-content/uploads/2015/09/Application-Diagram.png)



### Actor System

![image](http://vieras.eu/wp-content/uploads/2015/09/Actor-System.png)



## How to run

First have a Freeswitch instance and configure the connection from the java class MyEslConnection.

*	./sbt
*	compile
*	container:start

After the last command the container will start on localhost:8080

### Demo live

This is [FS-Moni](http://fs-moni.cloudapp.net:8080 "FS-Moni") demo app link.

### Basic events arriving in the web socket

- New Call
- End Call
- Failed Call
- Freeswitch Heartbeat
- Basic Stats (ACD, ASR, Live Calls..)



### HTTP/WS routes

1. ws://localhost:8080/fs-moni/live/events
2. http://localhost:8080/actors/GetCompletedCalls
3. http://localhost:8080/actors/GetConcurrentCalls
4. http://localhost:8080/actors/GetTotalConcurrentCalls
5. http://localhost:8080/actors/GetFailedCalls
6. http://localhost:8080/actors/GetTotalFailedCalls
7. http://localhost:8080/actors/call/:callid
7. http://localhost:8080/actors/call/:callUuid/channel/:channelUuid
8. http://localhost:8080/actors/lastHeartbeat
9. http://localhost:8080/actors/allHeartbeats
10. http://localhost:8080/actors/stats/GetBasicStatsTimeSeries