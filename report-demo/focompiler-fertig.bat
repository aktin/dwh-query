@echo off
call ..\..\tools\fop-2.0\fop -xml report-content.xml -xsl fo-report-fertig.xsl target\fo-report-fertig.pdf 
pause
