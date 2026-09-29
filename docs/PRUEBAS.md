# Plan reproducible de pruebas

Usar nombres EDA_G11 y correos de example.invalid; no modificar usuarios reales. Anotar los IDs devueltos y limpiar solo esos registros.

1. Ejecutar `mvn test package` en Functions y `mvn test` en BFF usando Java 17.
2. GET /api/roles y GET /api/users mediante Azure (x-functions-key) y BFF (según configuración previa).
3. POST /api/roles con {"name":"EDA_G11_<marca>","description":"Prueba EDA","active":true}. Esperar 201.
4. POST /api/users con {"fullName":"Prueba EDA","email":"eda_<marca>@example.invalid","roleId":ID_ROL,"active":true}. Esperar 201.
5. GET de ambos IDs: 200. PUT de ambos: 200. DELETE rol con usuario asociado: 409.
6. DELETE usuario: 204; DELETE rol: 204. GET usuario borrado: 404.
7. Repetir por GraphQL: createRole, createUser, consultas role/user, updateRole/updateUser, deleteUser/deleteRole. HTTP 200 no basta: comprobar ausencia de errors y datos correctos.
8. Consultar eventos_outbox y auditoria_eventos por subject. Esperar publicación y procesamiento de seis eventos por circuito. Comparar event_id, no solo tiempos ni contadores.
9. Republicar un evento válido con el mismo ID. Confirmar una sola fila de auditoría y que no existe fallo de procesamiento. No cambiar su fecha ni su payload.
10. Ejecutar EventEvidence rollback: debe imprimir ROLLBACK_ATOMICITY_OK.

Consulta de correlación:
```sql
SELECT o.event_id,o.event_type,o.subject,o.published_at,a.processed_at
FROM eventos_outbox o LEFT JOIN auditoria_eventos a ON a.event_id=o.event_id
WHERE o.subject IN ('/roles/<ID_ROL>','/usuarios/<ID_USUARIO>')
ORDER BY o.occurred_at;
```

No registrar claves o datos personales en capturas. Los resultados efectivos se guardan en EVIDENCIA.md y evidencia-remota.json cuando exista.
