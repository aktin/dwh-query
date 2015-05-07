Verteilte Abfragen im DWH
===============================
Kommunikationsschnittstelle auf Datawarehouse-Seite. Nimmt verteilte Anfragen und geplante Anfragen entgegen und f�hrt diese aus.
Ergebnisse werden an den Pool geliefert.

Verteilte Abfragen die im DWH eingehen k�nnen folgende Stadien haben:
1. Ungelesen
2. Eingegangen (gelesen aber unbearbeitet)
3. Durchf�hrung abgelehnt
4. Durchf�hrung best�tig
5. Durchgef�hrt, Export unbest�tigt
6. Durchgef�hrt, Export abgelehnt
7. Durchgef�hrt, Export best�tigt
8. Durchgef�hrt, Export abgeschlossen (erfolgreich versendet)

Zus�tzlich ist hinterlegt, ob die Abfrage von zentraler Seite
1. offen
2. abgebrochen
3. beendet
ist.

Datenbankschema
---------------
Schema f�r Verwaltung verteilter Abfragen muss noch entwickelt werden.

Benutzeroberfl�che
------------------
Konfigurationm�glichkeit von
1. Emailadresse f�r Benachrichtigungen
2. Abfragedurchf�hrung automatisch/manuell
3. Ergebnis�bermittlung automatisch/manuell

Anzeige offener Anfragen mit M�glichkeit zur Best�tigung von Abfragedurchf�hrung/Ergebnis�bermittlungen.


Konfiguration
-------------
- dwh-id (interne ID zur Identifikation von Standort. ggf. durch Zertifikat ersetzen damit dies nicht durch Fremde ge�ndert werden kann)
- benachrichtigungs-email-adresse (bei neue Anfrage wird Benachrichtigung verschickt)
- pool-server (adresse/port an die Anfrageergebnisse gesendet werden)
- pool-certificate (public key f�r pool server zur Verifikation)
- 

Exportierte Schnittstellen
--------------------------
PUT /query/12345
Beauftragung einer neuen Anfrage (verteilt oder geplant)

DELETE /query/12345
L�schung einer Anfrage (d.h. nicht mehr durchf�hren, keine Daten mehr senden)

GET /query
Liste aller Anfragen

GET /query/12345
Informationen zu einer Anfrage


Kommunikation an Pool
---------------------
- Anfrage erhalten (ohne Interaktion)
- Anfrage abgelehnt (nach Interaktion)
- Anfrage genehmigt (nach Interaktion)
- Ergebnis von Anfrage (optional automatisch nach Interaktion/Ergebnispr�fung bzw. bei geplanten automatisch)

