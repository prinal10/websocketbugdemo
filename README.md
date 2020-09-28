# websocketbugdemo
demo to reproduce websocket bug.

You can simple use any small websocket stomp client online and connect to "ws://localhost:8093/secured/user"
and then send a subscribe request on the same connection with a header key as "destination" and any string as value since its getting override on the server anyway for keeping the test simple.
