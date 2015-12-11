@echo off
call ..\fop-2.0\fop -xml report-content.xml -xsl fo-report-fertig.xsl fo-report-fertig.pdf 
pause
