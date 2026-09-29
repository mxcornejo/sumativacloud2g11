# Sumativa 3 · Grupo 11 · Usuarios y Roles con Azure Event Grid

Leer [informe](docs/INFORME.md), [evidencia](docs/EVIDENCIA.md), [pauta](docs/MATRIZ_PAUTA.md) y [guion](docs/GUION_VIDEO.md).

Se conserva la base de Sumativas 1/2. La extensión añade una outbox Oracle, publicación Java temporizada y consumidor Java idempotente de auditoría. Ver diseno/arquitectura-sumativa-3.svg y su versión editable .drawio.

Compilar con Java 17 y Maven: `mvn test package` en azure-functions-usuarios-roles; `mvn test` en bff-usuarios-roles. Usar local.settings.sample.json como referencia, sin incorporar claves al control de versiones.

Repositorio de continuidad: https://github.com/mxcornejo/sumativacloud2g11 . Su contenido remoto debe verificarse antes de entregar; una copia local no acredita publicación ni colaboración de la pareja.
