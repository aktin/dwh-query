Kommunikationsschnittstelle auf Datawarehouse-Seite. Nimmt verteilte Anfragen und geplante Anfragen entgegen und führt diese aus.
Ergebnisse werden an den Pool geliefert.

Konfiguration:
- dwh-id (interne ID zur Identifikation von Standort. ggf. durch Zertifikat ersetzen damit dies nicht durch Fremde geändert werden kann)
- benachrichtigungs-email-adresse (bei neue Anfrage wird Benachrichtigung verschickt)
- pool-server (adresse/port an die Anfrageergebnisse gesendet werden)
- pool-certificate (public key für pool server zur Verifikation)
- 

=== Exportierte Schnittstellen ===
PUT /query/12345
Beauftragung einer neuen Anfrage (verteilt oder geplant)

DELETE /query/12345
Löschung einer Anfrage (d.h. nicht mehr durchführen, keine Daten mehr senden)

GET /query
Liste aller Anfragen

GET /query/12345
Informationen zu einer Anfrage


=== Kommunikation an Pool ===
- Anfrage erhalten (ohne Interaktion)
- Anfrage abgelehnt (nach Interaktion)
- Anfrage genehmigt (nach Interaktion)
- Ergebnis von Anfrage (optional automatisch nach Interaktion/Ergebnisprüfung bzw. bei geplanten automatisch)

