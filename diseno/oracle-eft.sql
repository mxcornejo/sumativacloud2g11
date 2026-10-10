-- EFT: ejecutar UNA VEZ después de oracle-schema.sql y oracle-eventos.sql.
-- Detener temporalmente PublicarEventos y las escrituras durante la migración.
ALTER TABLE roles ADD (deleted_at TIMESTAMP, is_default NUMBER(1) DEFAULT 0 NOT NULL);
/
ALTER TABLE roles ADD CONSTRAINT ck_roles_default CHECK (is_default IN (0,1));
/
CREATE UNIQUE INDEX uk_roles_default ON roles(CASE WHEN is_default=1 THEN 1 END);
/
ALTER TABLE usuarios MODIFY (role_id NULL);
/
-- Los usuarios preexistentes no deben recibir efectos nuevos de eventos antiguos.
ALTER TABLE usuarios ADD (default_pending NUMBER(1) DEFAULT 0 NOT NULL);
/
ALTER TABLE usuarios ADD CONSTRAINT ck_users_pending CHECK (default_pending IN (0,1));
/
INSERT INTO roles(name,description,active,is_default) VALUES ('EFT_USUARIO','Rol por defecto protegido',1,1);
/
CREATE OR REPLACE TRIGGER trg_roles_outbox
AFTER INSERT OR UPDATE OR DELETE ON roles
FOR EACH ROW
DECLARE
 v_action VARCHAR2(16);
 v_id NUMBER;
BEGIN
 IF INSERTING THEN v_action := 'Created'; v_id := :NEW.id;
 ELSIF UPDATING THEN
  v_id := :NEW.id;
  IF :OLD.deleted_at IS NULL AND :NEW.deleted_at IS NOT NULL THEN v_action := 'Deleted';
  ELSE v_action := 'Updated'; END IF;
 ELSE v_action := 'Deleted'; v_id := :OLD.id;
 END IF;
 INSERT INTO eventos_outbox(event_id,event_type,subject,payload)
 VALUES (RAWTOHEX(SYS_GUID()), 'Roles.' || v_action, '/roles/' || TO_CHAR(v_id),
 JSON_OBJECT('entityId' VALUE v_id RETURNING CLOB));
END;
/
-- El bloqueo serializa asignaciones concurrentes con la eliminación lógica del rol.
CREATE OR REPLACE TRIGGER trg_usuario_rol_vigente
BEFORE INSERT OR UPDATE OF role_id ON usuarios
FOR EACH ROW
DECLARE
 v_deleted TIMESTAMP;
BEGIN
 IF :NEW.role_id IS NOT NULL THEN
  SELECT deleted_at INTO v_deleted FROM roles WHERE id=:NEW.role_id FOR UPDATE;
  IF v_deleted IS NOT NULL THEN RAISE_APPLICATION_ERROR(-20001,'El rol está eliminado'); END IF;
 END IF;
END;
/
-- La configuración por defecto no puede quedar inválida por otra ruta SQL.
CREATE OR REPLACE TRIGGER trg_proteger_rol_default
BEFORE UPDATE OR DELETE ON roles
FOR EACH ROW
BEGIN
 IF :OLD.is_default=1 THEN
  IF DELETING THEN RAISE_APPLICATION_ERROR(-20002,'Rol por defecto protegido');
  ELSIF :NEW.is_default<>1 OR :NEW.active<>1 OR :NEW.deleted_at IS NOT NULL THEN
   RAISE_APPLICATION_ERROR(-20002,'Rol por defecto protegido');
  END IF;
 END IF;
END;
/
COMMIT;
/
