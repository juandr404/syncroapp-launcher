# ADR-002: minSdk 26 / targetSdk 35

**Estado:** Aceptado · **Fecha:** 2026-07-28

## Decision

`minSdk = 26` (Android 8.0), `targetSdk = compileSdk = 35`.

## Motivos

minSdk 26 es el punto donde dejan de existir las ramas de codigo legacy mas molestas:

- `AdaptiveIconDrawable` existe siempre (por debajo de 26 no hay iconos adaptativos).
- `ShortcutManager` y `LauncherApps.Callback` estan completos y estables.
- `java.time` esta disponible sin desugaring.

Bajar a 23 sumaria uno o dos puntos de cuota de mercado a cambio de codigo de compatibilidad
en la parte mas visible del producto. Subir a 29 (para tener `RoleManager` sin alternativa)
costaria cerca del 10% del parque para ahorrarse un `if`.

## Bifurcaciones que el codigo maneja explicitamente

| API | Que cambia | Alternativa por debajo |
|---|---|---|
| 29+ | `RoleManager.createRequestRoleIntent(ROLE_HOME)` | `Settings.ACTION_HOME_SETTINGS` |
| 30+ | Filtrado de visibilidad de paquetes | Exencion automatica de launchers (ver ADR-004) |
| 33+ | Capa `monochrome` de los iconos adaptativos | Tintado manual del icono |
