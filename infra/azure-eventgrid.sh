#!/usr/bin/env bash
set -euo pipefail
# Ejecutar después de desplegar y verificar las seis funciones Java.
: "${AZURE_RESOURCE_GROUP:?Definir grupo de recursos}"
: "${FUNCTION_APP:?Definir aplicación existente}"
: "${EVENT_GRID_TOPIC:?Definir Topic}"
TOPIC_ID=$(az eventgrid topic show -g "$AZURE_RESOURCE_GROUP" -n "$EVENT_GRID_TOPIC" --query id -o tsv)
APP_ID=$(az functionapp show -g "$AZURE_RESOURCE_GROUP" -n "$FUNCTION_APP" --query id -o tsv)
az eventgrid event-subscription create --name auditoria-usuarios-roles \
 --source-resource-id "$TOPIC_ID" --endpoint-type azurefunction \
 --endpoint "$APP_ID/functions/AuditarEvento" \
 --included-event-types Usuarios.Created Usuarios.Updated Usuarios.Deleted Roles.Created Roles.Updated Roles.Deleted \
 --max-delivery-attempts 30 --event-ttl 1440 --output none
