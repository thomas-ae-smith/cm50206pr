killall java
java -jar lib/aslookup.jar 8080 &
sleep 2
java -jar lib/asstartup.jar -L http://localhost:8080 MtLookitthat &
sleep 3
java -jar lib/injector.jar agents/rover-monitor-0.0.1.jar MtLookitthat &
sleep 2
java -jar lib/injector.jar agents/rover-agent-0.0.1.jar MtLookitthat &
