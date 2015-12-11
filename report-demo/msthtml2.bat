@echo off
ECHO Exportiere Fakten
msxsl.exe report-content.xml html-report2.xsl -xw > html-report2.html
pause