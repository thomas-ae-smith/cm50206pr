@echo off
IF "%AGENTSCAPE_HOME%" == "" GOTO NOASHOME
:YESASHOME
java -jar "%AGENTSCAPE_HOME%\lib\app.jar" %1 %2
GOTO END
:NOASHOME
@ECHO Please set the AGENTSCAPE_HOME environment variable.
GOTO END
:END
