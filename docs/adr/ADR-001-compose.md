# ADR-001: Jetpack Compose en vez de Views

**Estado:** Aceptado · **Fecha:** 2026-07-28

## Contexto

El launcher es casi todo tipografia: un reloj gigante en contorno y listas de texto. El equipo
es una persona con dedicacion parcial y una IA como par programador.

## Decision

Kotlin + Jetpack Compose para toda la interfaz.

## Motivos

- El efecto de contorno del dia se resuelve con `TextStyle(drawStyle = Stroke(...))`, una linea.
  Con Views habria que escribir una vista custom con dos `TextPaint`.
- Un solo lenguaje (Kotlin) en vez de repartir el contexto entre XML, binding y codigo: con un
  par programador de IA, cada archivo menos que abrir es velocidad real.
- Gestos y animaciones con APIs modernas (`Modifier.pointerInput`) en vez de `GestureDetector`.

## Lo que se sacrifica

Compose agrega entre 2 y 4 MB al APK y algo de arranque en frio frente a un launcher hecho
100% con Views. Se compensa con R8 en modo completo y Baseline Profiles. El unico punto donde
Compose no aplica son los widgets de terceros (`AppWidgetHostView` es una View obligatoriamente),
que entrarian por interoperabilidad con `AndroidView`.
