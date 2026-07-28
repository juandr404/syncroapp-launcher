# Decisiones de arquitectura (ADR)

Registro corto de cada decision estructural: que se decidio, por que, y que se sacrifico.
Existen para que cualquiera (incluido el autor dentro de seis meses) entienda el porque de
una decision sin tener que reconstruirla leyendo el codigo.

| ADR | Decision | Estado |
|---|---|---|
| [001](ADR-001-compose.md) | Jetpack Compose en vez de Views | Aceptado |
| [002](ADR-002-sdk.md) | minSdk 26 / targetSdk 35 | Aceptado |
| [003](ADR-003-modulos.md) | Cuatro modulos, con `:core:launcherapps` como frontera del sistema | Aceptado |
| [004](ADR-004-launcherapps.md) | `LauncherApps` como unica fuente de apps, sin `QUERY_ALL_PACKAGES` | Aceptado |
| [005](ADR-005-datastore.md) | DataStore tipado con JSON en vez de Proto | Aceptado |
| [006](ADR-006-sin-red.md) | Sin permiso de internet, sin rastreadores | Aceptado |
