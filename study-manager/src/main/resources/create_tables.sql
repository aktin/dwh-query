CREATE TABLE optinout_studies (
	id VARCHAR(16) NOT NULL PRIMARY KEY,
	title VARCHAR(128) NOT NULL,
	description VARCHAR(255) NOT NULL,
	created_ts TIMESTAMP NOT NULL,
	closed_ts TIMESTAMP NULL,
	options VARCHAR(128) NOT NULL,
	sic_validate VARCHAR(128) NULL,
	sic_generate VARCHAR(128) NULL,
	sic_generator_state VARCHAR(64) NULL
);

--INSERT INTO optinout_studies (id,title,description,created_ts,options) VALUES ('AKTIN','AKTIN','Notaufnahmeregister','2018-01-01 00:00:00', 'OPT=O');
INSERT INTO optinout_studies (id,title,description,created_ts,options,sic_generate) VALUES ('ENQUIRE','ENQuIRE','Evaluierung der Qualitätsindikatoren von Notaufnahmen auf Outcome-Relevanz für den Patienten','2019-01-01 00:00:00', 'OPT=I','SEQUENCE(1000,1)');

CREATE TABLE optinout_patients (
	study_id VARCHAR(16) NOT NULL,
	pat_ref VARCHAR(16) NOT NULL,
	pat_root VARCHAR(128) NOT NULL,
	pat_ext VARCHAR(128) NOT NULL,
	pat_psn VARCHAR(64) NOT NULL,
	create_user VARCHAR(64) NOT NULL,
	create_timestamp TIMESTAMP NOT NULL,
	optinout CHAR(1) NOT NULL,
	study_subject_id VARCHAR(128) NULL,
	comment VARCHAR(255) NULL,
	i2b2_patient_num INTEGER NULL,
	i2b2_last_visit TIMESTAMP NULL,
	i2b2_sync_time TIMESTAMP NULL,
	PRIMARY KEY(study_id, pat_ref, pat_root, pat_ext)
);

CREATE INDEX optinout_patients_study_id ON optinout_patients(study_id);

CREATE TABLE optinout_audittrail (
	study_id VARCHAR(16) NOT NULL,
	pat_ref VARCHAR(16) NOT NULL,
	pat_root VARCHAR(128) NOT NULL,
	pat_ext VARCHAR(128) NOT NULL,
	action_user VARCHAR(64) NOT NULL,
	action_timestamp TIMESTAMP NOT NULL,
	action CHAR(2) NOT NULL,
	study_subject_id VARCHAR(128) NULL,
	comment VARCHAR(255) NULL
);
CREATE INDEX optinout_audittrail_study_id ON optinout_audittrail(study_id);

