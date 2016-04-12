Data Warehouse Gateway
----------------------
Communication of the data warehouse to the outside world. 
1. Retrieves queries from the query broker.
2. Submits query results to the central pool.

The gateway can function via the following channels:
1. Polling: Temporary connections are established at 
regular intervals.

2. Email: A IMAP/POP3 mailbox is checked at regular itervals. 
New queries are submitted by the broker via email. Status updates
are mailed back to the broker. Results are mailed to the pool.

3. Direct communication between RESTful interfaces if a persistent
VPN connection is available.

Configuration Parameters
------------------------

Configuration parameters are stored in a relational database table
in the AKTIN database.

Implements Java interface to read/write all values
Implements Restful interface which cannot read (WO) values, but write them


tls.keystore.path (R) keystore containing key and certificates for TLS
local.name (W) local name for this site/clinic, 
local.contact.name (W)
local.contact.email (W)

i2b2.project (R) i2b2 project id "Demo"
i2b2.crc.ds (R) i2b2 jndi datasource "java:/QueryToolDemoDS"
i2b2.lastimport (R) timestamp of last import

smtp.server (W)
smtp.port (W)
smtp.user (W)
smtp.password (WO)
smtp.auth (W) [plain|ssl|...]

query.notification.email (W) list of email addresses to receive notifications for queries
query.result.dir (R)
exchange.lastcontact (R) timestamp of last contact to broker via direct connection or received email timestamp
exchange.method (W) https|email
exchange.https.interval (W) interval in hours between polling connections to broker
exchange.https.broker (W) server name of the AKTIN broker
exchange.https.pool (W) server name of AKTIN pool
exchange.inbox.address (W) email address to receive queries
exchange.inbox.interval (W) interval in hours between checking for new emails
exchange.inbox.server (W) server configuration to check for query emails
exchange.inbox.port (W)
exchange.inbox.protocol (W) [imap|pop3]
exchange.inbox.user (W)
exchange.inbox.password (WO)

Query Database
---------------

CREATE TABLE config(
	property	VARCHAR(32)	NOT NULL,
	user_access	VARCHAR(4)	NOT NULL, -- R(eadable) W(riteable) WO(write only) H(hidden)
	type	VARCHAR(16) NOT NULL,
	value	TEXT,	
	PRIMARY KEY(property)
);

-- for param names, see dwh-gateway/README.md
