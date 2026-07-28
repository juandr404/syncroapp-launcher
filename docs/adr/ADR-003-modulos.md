# ADR-003: Cuatro modulos, con `:core:launcherapps` como frontera del sistema

**Estado:** Aceptado · **Fecha:** 2026-07-28

## Decision

```
:app                  MainActivity (la actividad HOME), navegacion, pantallas y ViewModels
:core:ui              tema, tipografia, componentes (RelojGigante, FilaApp), gestos
:core:data            modelo de ajustes, DataStore, buscador difuso, reloj del sistema
:core:launcherapps    LauncherApps, RoleManager, acciones del sistema
```

Reglas: las funcionalidades dependen de `core`, nunca entre ellas; `:app` conoce a todos.

## Motivos

`:core:launcherapps` es el modulo importante. Todo lo hostil —APIs de sistema, diferencias por
version de Android, rarezas de cada fabricante— vive detras de interfaces (`FuenteApps`), lo que
deja al resto de la app testeable con dobles de prueba y concentra los parches por OEM en un
solo sitio.

## Por que cuatro y no siete

El plan original proponia separar tambien `:feature:home`, `:feature:drawer` y `:feature:settings`.
Para una persona sola, ese nivel de division cuesta mas en navegacion y cableado de lo que
devuelve en tiempos de compilacion incremental. Las pantallas viven como paquetes dentro de
`:app` y se extraen a modulos propios cuando alguna crezca lo suficiente para justificarlo.

Los gestos tampoco son un modulo: son un `Modifier` en `:core:ui`.
