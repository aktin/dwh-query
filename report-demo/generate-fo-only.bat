@echo off
ECHO Nur transformation zu XML-FO ohne Erzeugung von PDF
ECHO Das ist hilfreich zur Fehlersuche
..\..\tools\msxsl.exe report-content.xml fo-report-fertig.xsl > target\fo-report-fertig-fo.xml 
pause
