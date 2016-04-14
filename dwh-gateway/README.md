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

