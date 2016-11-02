resources
=========

Die Inhalte des Reports werden primär über die Dateien report-content.xml und report-data.xml gesteuert.

Das Layout wird über die Datei fo-report.xsl gesteuert.

Feste Grafiken (Logos) werden bevorzugt als SVG-Grafik eingebunden.

Die Datei export-descriptor.xml beschreibt die Daten, die das R-Script als Eingabe erwartet.

Neben den .R-Dateien, die dynamische Grafiken und Tabellen erzeugen, gibt es noch einige CSV-Dateien, die ggf. angepasst werden müssen.

ICD-3Steller.csv - enthält eine Liste der ICD-Codes und der Bezeichnungen, die dazu im Report verwendet werden.

CEDIS.csv - enthält eine Liste der CEDIS Presenting Complaints Codes und der Bezeichnungen, die dazu im Report verwendet werden.

factors.csv - enthält einige Codierungslisten, die aus Encoding-Gründen aus einer UTF-8-CSV-Datei geladen werden.

Pflege der resources
--------------------

Die CEDIS Codes könnten sich (wie jede andere Codeliste, z.B. auch in factors.csv) ändern und müssen dann entsprechend manuell angepasst bzw. eine neue Liste hinterlegt werden. Feste Releasezyklen oder eine strukturierte Bezugsquelle sind nicht bekannt.

Zur ICD10GM gibt es jährlich ein neues Release des DIMDI.
Die entspechende XML-Datei mit den Daten kann man auf der Webseite http://www.dimdi.de/dynamic/de/klassi/downloadcenter/icd-10-gm/ herunterladen.
Benötigt wird aus dem Ordner "Systematik" die EDV-Fassung ClaML/XML
Der Dateiname lautet icd10gmYYYYsyst_claml_YYYYMMDD.xml
Die Transformation ICD2txt3-Steller.xsl (im Ordner resources/src) angewendet auf diese Datei liefert die benötigte Liste.
Das Encoding der CSV-Datei muss UTF-8 sein.

Damit im Report alle Bezeichnungen in einer Zeile ausgegeben werden können, dürfen die Bezeichnungen nur ca. 60 Zeichen lang sein (die exakte mögliche Länge lässt sich aus typografischen Gründen nicht angeben). Diese Kürzung muss (falls gewünscht) manuell erfolgen.
Das einfachste Vorgehen dazu ist vermutlich ein Diff der ICD-Versionen zu erstellen und die Änderungen manuell durchzuführen, so dass man in fast allen Fällen die alte, bereits fertig überarbeitete Liste, beibehalten kann.