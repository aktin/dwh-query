resources
=========

Die Inhalte des Reports werden prim�r �ber die Dateien report-content.xml und report-data.xml gesteuert.

Das Layout wird �ber die Datei fo-report.xsl gesteuert.

Feste Grafiken (Logos) werden bevorzugt als SVG-Grafik eingebunden.

Die Datei export-descriptor.xml beschreibt die Daten, die das R-Script als Eingabe erwartet.

Neben den .R-Dateien, die dynamische Grafiken und Tabellen erzeugen, gibt es noch einige CSV-Dateien, die ggf. angepasst werden m�ssen.

ICD-3Steller.csv - enth�lt eine Liste der ICD-Codes und der Bezeichnungen, die dazu im Report verwendet werden.

CEDIS.csv - enth�lt eine Liste der CEDIS Presenting Complaints Codes und der Bezeichnungen, die dazu im Report verwendet werden.

factors.csv - enth�lt einige Codierungslisten, die aus Encoding-Gr�nden aus einer UTF-8-CSV-Datei geladen werden.

Pflege der resources
--------------------

Die CEDIS Codes k�nnten sich (wie jede andere Codeliste, z.B. auch in factors.csv) �ndern und m�ssen dann entsprechend manuell angepasst bzw. eine neue Liste hinterlegt werden. Feste Releasezyklen oder eine strukturierte Bezugsquelle sind nicht bekannt.

Zur ICD10GM gibt es j�hrlich ein neues Release des DIMDI.
Die entspechende XML-Datei mit den Daten kann man auf der Webseite http://www.dimdi.de/dynamic/de/klassi/downloadcenter/icd-10-gm/ herunterladen.
Ben�tigt wird aus dem Ordner "Systematik" die EDV-Fassung ClaML/XML
Der Dateiname lautet icd10gmYYYYsyst_claml_YYYYMMDD.xml
Die Transformation ICD2txt3-Steller.xsl (im Ordner resources/src) angewendet auf diese Datei liefert die ben�tigte Liste.
Das Encoding der CSV-Datei muss UTF-8 sein.

Damit im Report alle Bezeichnungen in einer Zeile ausgegeben werden k�nnen, d�rfen die Bezeichnungen nur ca. 60 Zeichen lang sein (die exakte m�gliche L�nge l�sst sich aus typografischen Gr�nden nicht angeben). Diese K�rzung muss (falls gew�nscht) manuell erfolgen.
Das einfachste Vorgehen dazu ist vermutlich ein Diff der ICD-Versionen zu erstellen und die �nderungen manuell durchzuf�hren, so dass man in fast allen F�llen die alte, bereits fertig �berarbeitete Liste, beibehalten kann.