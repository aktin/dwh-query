@echo off
ECHO Erstelle HTML Bericht
..\..\tools\msxsl.exe report-content.xml html-report2.xsl -xw > target\html-report2.html
pause