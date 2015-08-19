# freeswitch-monitoring
Project to monitor FreeSwitch status and health in Real-Time.

## Description

Freeswitch-monitoring is a distributed application based in the actor-based paradigm. As events are comming from the Freeswitch ESL connection, these messages are routed and processed by the Actor system, keeping in this way the main statistics data ready in memory for fast access. These data are also served in the front client though a WS  or/and an REST API.
The Scalatra and akka.io scala API are the tools used to achive the goal of this project.

## How to run

First have a Freeswitch instance and configure the connection from the java class MyEslConnection.

*	./sbt
*	compile
*	container:start

After the last command the container will start at port 8080.


### Test routes

1. ws://localhost:8080/live/events - With client(chrome ws) connect to WS and wait for messages or send.
2.  http://localhost:8080/ko/brd - To send a message to the WebSocket connection
3.  http://localhost:8080/actors/GetCalls - To get the current calls(uuid provided)
4.  http://localhost:8080/  for the chat test app in JS. Calls are also forwarded there.