Query Exchange
--------------
This project contains data structures and interface definitions
for the exchange of query definitions, updates and results.

All other modules which use queries need to include this project.


A query will always return a case count and patient count.
In addition to that, concepts for the cases can be returned either with
their raw data or aggregated.
```
<query type="repeating|single">
	<id>unique id</id>
	<description>
	
	<description>
	<recipient>
		<name></name>
		<organisation></organisation>
		<location></location>
		<email></email>
		<phone></phone>
		<url></url>
	</recipient>
	<concepts>
		<concept id="CEDIS30:XXX" type="raw"/>
		<concept id="XXX1" type="aggregate">
			<count group-by="fact.value">
			<max/>
		</concept>
		<concept id="XXX1" type="aggregate">
			<!-- by arrival time -->
			<count group-by="substr(8,10,fact.start)">
			<max/>
			<min/>
		</concept>
	</concepts>
	<definition type="sql">
		
	</definition>
	<broker>
		<!-- timestamp the query was published by the broker -->
		<published>2015-12-01T18:30:14</published>
		<!-- later, the query can be either canceled or closed -->
		<closed>2015-12-02T18:30:00</closed>
		<canceled>2015-12-02T18:30:14</canceled>
	</broker>
	<data-warehouse>
		<received>XXXtimestamp</received>
		<confirmation method="single|double|automatic">xxx</confirmation>
		<!-- confirmation or rejection -->
		<rejected></rejected>
		<comment></comment>
		<last-execution>
			<completed>XXXtimestamp</completed>
			<failed></failed>
			
		</last-completed>
	</data-warehouse>
</query>


<query-result id-ref="">
</query-result>
```