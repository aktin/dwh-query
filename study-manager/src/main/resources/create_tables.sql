CREATE TABLE optinout_studies (
	id VARCHAR(16) NOT NULL PRIMARY KEY,
	title VARCHAR(128) NOT NULL,
	description VARCHAR(255) NOT NULL,
	created_ts TIMESTAMP NOT NULL,
	closed_ts TIMESTAMP NOT NULL,
	options VARCHAR(128) NOT NULL,
	sic_validate VARCHAR(128) NULL,
	sic_generate VARCHAR(128) NULL,
	sic_generator_state VARCHAR(64) NULL
);

INSERT INTO optinout_studies (id,title,description,created_ts,options) VALUES ('AKTIN','AKTIN','Notaufnahmeregister','2018-01-01 00:00:00', 'OPT=O');
INSERT INTO optinout_studies (id,title,description,created_ts,options) VALUES ('ENQUIRE','ENQuIRE','Evaluierung der Qualitätsindikatoren von Notaufnahmen auf Outcome-Relevanz für den Patienten','2019-01-01 00:00:00', 'OPT=I');

