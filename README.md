# freeswitch-monitoring
Project to monitor FreeSwitch status and health

## How to run

*	./sbt
*	compile
*	container:start

After the last command the container will start at port 8080.


### Test routes

1. ws://localhost:8080/live/events - With client(chrome ws) connect to WS and wait for messages or send.
2.  http://localhost:8080/ko/brd - To send a message to the WebSocket connection
3.  http://localhost:8080/actors/GetCalls - To get the current calls(uuid provided)
4.  http://localhost:8080/  for the chat test app in JS. Calls are also forwarded there.