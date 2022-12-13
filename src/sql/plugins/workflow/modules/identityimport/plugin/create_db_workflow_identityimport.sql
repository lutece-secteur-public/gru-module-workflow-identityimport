
DROP TABLE IF EXISTS workflow_task_identity_import_cf;
CREATE TABLE workflow_task_identity_import_cf (
	id_task int NOT NULL,
	id_state1 INT NOT NULL,
	id_state2 INT NOT NULL,
	id_state3 INT NOT NULL,
	id_worflow INT NOT NULL,
	CONSTRAINT workflow_task_identity_import_cf_pkey PRIMARY KEY (id_task)
);