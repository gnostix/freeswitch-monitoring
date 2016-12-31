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

### Dashboard pic

![image](http://vieras.eu/wp-content/uploads/2016/04/fsmoni-dashboard.png)

## How to run

First have a Freeswitch instance. You can install the war file in two ways:

a) Clone the project and from the application git folder, run bellow commands.

*	./sbt
*	compile
*	jetty:start
* jetty:stop

After the last command the container will start on localhost:8080

b)
You can also [download](http://fs-moni.cloudapp.net/freeswitchop_2.11-0.1.0-SNAPSHOT.war) a ready build war file.

*	download file from location
*	have installed in a server java oracle 7 or 8
*	have installed a jetty server 9 between versions 9.2.1.v20140609 or 9.2.10.v20150310 
*	add war file as root app in the Jetty webapps folder. We do that by copying the freeswitch-monitoring.war file to root.war file inside the webapps folder. e.g. in linux `cp /war/location/freeswitch-monitoring.war /path/to/jetty/webapps/root.war`
*	start jetty and connect your browser to url jetty-ip:8080

After the installation is complete then:

The application will try to connect to default hostname fs-instance.com and password ClueCon. You can add to your machine in /etc/hosts file the correct entry for fs-instance.com OR you can just navigate through the interface and go to the configuration and add there your Freeswitch credentials.


### Demo live

This is [FS-Moni](http://fs-moni.cloudapp.net:8080/dashboard.html# "FS-Moni") demo app link. For login use credentials **user: admin** and **password: admin**

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

## Contact: p_alx at hotmail dot com
