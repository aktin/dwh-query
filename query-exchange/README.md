Query Exchange
--------------
This project contains data structures and interface definitions
for the exchange of query definitions, updates and results.

All other modules which use queries need to include this project.


A query will always return a case count and patient count.
In addition to that, concepts for the cases can be returned either with
their raw data or aggregated.

Process:
1. DWH sendet an Broker status zu Anfragen. (nur query/id und data-warehouse) 
Nur die Anfragen werden übermittelt, die 
2. DWH sendet an Pool results, die noch nicht übermittelt wurden.

2. DWH fragt bei Broker nach Anfragen mit "Last-modified-since" header.
3. Broker antwortet mit allen Anfragen, die das DWH noch nicht abgerufen hat oder die sich seit dem letzten Abruf geändert haben.
Zusätzlich speichert der Broker für jede Anfrage den Zeitstempel der letzten Übermittlung an DWH.

```
<request>
	<id>unique request id</id>
	<date-reference>2016-04-11</date-reference>
	<query>
		<id>unique query id</id>
		<schedule type="single|repeating">
			<interval>daily</interval>
		</schedule>
		<description>
		
		<description>
		<principal>
			<name></name>
			<organisation></organisation>
			<location></location>
			<email></email>
			<phone></phone>
			<url></url>
		</principal>

		<concepts>
			<concept id="CEDIS30:XXX" type="raw"/>
			<concept id="XXX1" type="aggregate">
				<count group-by="fact.value">
			</concept>
			<concept id="XXX1" type="aggregate">
				<!-- by arrival time -->
				<count group-by="substr(8,10,fact.start)">
				<max/>
				<min/>
			</concept>
		</concepts>
		<definition xsi:type="sql">
			
		</definition>
	</query>
	<signature from="broker" algorithm="SHA256withRSA">...</signature>
	<signature from="me" algorithm="SHA256withRSA">...</signature>
	<broker>
		<last-modified>max timestamp der nachfolgenden</last-modified>
		<!-- timestamp the query was published by the broker -->
		<published>2015-12-01T18:30:14</published>
		<!-- later, the query can be either canceled or closed -->
		<closed>2015-12-02T18:30:00</closed>
		<canceled>2015-12-02T18:30:14</canceled>
	</broker>
	<data-warehouse>
		<last-modified>max timestampt der nachfolgenden</last-modified>
		<received>XXXtimestamp</received>
		<confirmation method="single|double|automatic">xxx</confirmation>
		<!-- confirmation or rejection -->
		<rejected></rejected>
		<comment></comment>
		<last-execution>
			<completed>XXXtimestamp</completed>
			<failed></failed>
			
		</last-execution>
		<last-contact>
		<result-submitted>XXX timestamp</result-submitted>
	</data-warehouse>
</query>


<query-result id-ref="">
</query-result>
```