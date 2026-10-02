#!/usr/bin/env bash
set -euo pipefail
PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
JAVA_BIN="$(/usr/libexec/java_home -v 17)/bin/java"
LIB_DIR="$PROJECT_DIR/azure-functions-usuarios-roles/target/azure-functions/usuarios-roles-functions/lib"
if [[ ! -d "$LIB_DIR" ]]; then
  echo "Faltan dependencias: ejecutar mvn package en azure-functions-usuarios-roles con Java 17 antes del video." >&2
  exit 1
fi
exec "$JAVA_BIN" "-Dproyecto.root=$PROJECT_DIR" -cp "$LIB_DIR/*" "$PROJECT_DIR/infra/VerAuditoria.java" "$@"
