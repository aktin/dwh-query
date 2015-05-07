Verteilte Abfragen im DWH
===============================
Kommunikationsschnittstelle auf Datawarehouse-Seite. Nimmt verteilte Anfragen und geplante Anfragen entgegen und führt diese aus.
Ergebnisse werden an den Pool geliefert.

Verteilte Abfragen die im DWH eingehen können folgende Stadien haben:
1. Ungelesen
2. Eingegangen (gelesen aber unbearbeitet)
3. Durchführung abgelehnt
4. Durchführung bestätig
5. Durchgeführt, Export unbestätigt
6. Durchgeführt, Export abgelehnt
7. Durchgeführt, Export bestätigt
8. Durchgeführt, Export abgeschlossen (erfolgreich versendet)

Zusätzlich ist hinterlegt, ob die Abfrage von zentraler Seite
1. offen
2. abgebrochen
3. beendet
ist.

Datenbankschema
---------------
Schema für Verwaltung verteilter Abfragen muss noch entwickelt werden.

Benutzeroberfläche
------------------
Konfigurationmöglichkeit von
1. Emailadresse für Benachrichtigungen
2. Abfragedurchführung automatisch/manuell
3. Ergebnisübermittlung automatisch/manuell

Anzeige offener Anfragen mit Möglichkeit zur Bestätigung von Abfragedurchführung/Ergebnisübermittlungen.


Konfiguration
-------------
- dwh-id (interne ID zur Identifikation von Standort. ggf. durch Zertifikat ersetzen damit dies nicht durch Fremde geändert werden kann)
- benachrichtigungs-email-adresse (bei neue Anfrage wird Benachrichtigung verschickt)
- pool-server (adresse/port an die Anfrageergebnisse gesendet werden)
- pool-certificate (public key für pool server zur Verifikation)
- 

Exportierte Schnittstellen
--------------------------
PUT /query/12345
Beauftragung einer neuen Anfrage (verteilt oder geplant)

DELETE /query/12345
Löschung einer Anfrage (d.h. nicht mehr durchführen, keine Daten mehr senden)

GET /query
Liste aller Anfragen

GET /query/12345
Informationen zu einer Anfrage


Kommunikation an Pool
---------------------
- Anfrage erhalten (ohne Interaktion)
- Anfrage abgelehnt (nach Interaktion)
- Anfrage genehmigt (nach Interaktion)
- Ergebnis von Anfrage (optional automatisch nach Interaktion/Ergebnisprüfung bzw. bei geplanten automatisch)

