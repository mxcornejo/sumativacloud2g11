-- Ejecutar una sola vez en el esquema de Usuarios/Roles. No elimina datos.
CREATE TABLE eventos_outbox (
 event_id VARCHAR2(36) PRIMARY KEY, event_type VARCHAR2(64) NOT NULL,
 subject VARCHAR2(128) NOT NULL, occurred_at TIMESTAMP DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL,
 payload CLOB NOT NULL, published_at TIMESTAMP,
 CONSTRAINT outbox_json CHECK (payload IS JSON)
);
/
CREATE INDEX ix_outbox_pending ON eventos_outbox(published_at,occurred_at);
/
CREATE TABLE auditoria_eventos (
 event_id VARCHAR2(36) PRIMARY KEY, event_type VARCHAR2(64) NOT NULL,
 subject VARCHAR2(128) NOT NULL, occurred_at TIMESTAMP NOT NULL,
 payload CLOB NOT NULL, processed_at TIMESTAMP DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL,
 CONSTRAINT audit_json CHECK (payload IS JSON)
);
/
CREATE OR REPLACE TRIGGER trg_usuarios_outbox
AFTER INSERT OR UPDATE OR DELETE ON usuarios
FOR EACH ROW
DECLARE
 v_action VARCHAR2(16);
 v_id NUMBER;
BEGIN
 IF INSERTING THEN v_action := 'Created'; v_id := :NEW.id;
 ELSIF UPDATING THEN v_action := 'Updated'; v_id := :NEW.id;
 ELSE v_action := 'Deleted'; v_id := :OLD.id;
 END IF;
 INSERT INTO eventos_outbox(event_id,event_type,subject,payload)
 VALUES (RAWTOHEX(SYS_GUID()), 'Usuarios.' || v_action, '/usuarios/' || TO_CHAR(v_id),
 JSON_OBJECT('entityId' VALUE v_id RETURNING CLOB));
END;
/
CREATE OR REPLACE TRIGGER trg_roles_outbox
AFTER INSERT OR UPDATE OR DELETE ON roles
FOR EACH ROW
DECLARE
 v_action VARCHAR2(16);
 v_id NUMBER;
BEGIN
 IF INSERTING THEN v_action := 'Created'; v_id := :NEW.id;
 ELSIF UPDATING THEN v_action := 'Updated'; v_id := :NEW.id;
 ELSE v_action := 'Deleted'; v_id := :OLD.id;
 END IF;
 INSERT INTO eventos_outbox(event_id,event_type,subject,payload)
 VALUES (RAWTOHEX(SYS_GUID()), 'Roles.' || v_action, '/roles/' || TO_CHAR(v_id),
 JSON_OBJECT('entityId' VALUE v_id RETURNING CLOB));
END;
/
