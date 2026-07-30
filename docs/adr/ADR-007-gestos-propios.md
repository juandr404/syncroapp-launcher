# ADR-007 — Gestos de navegacion propios con un servicio de accesibilidad

**Fecha:** 2026-07-29
**Estado:** Aceptado

## Contexto

En MIUI y HyperOS, poner un launcher externo como predeterminado desactiva los gestos de
navegacion del sistema y deja el telefono en botones. El diagnostico, hecho sobre un Redmi
Note 10 Pro con Android 13:

```
$ adb shell cmd package query-services -a android.intent.action.QUICKSTEP_SERVICE
com.miui.home/.recents.TouchInteractionService
```

Ese servicio, propiedad del launcher de Xiaomi, es el UNICO proveedor del sistema para los
gestos de atras, inicio y recientes. Con otro launcher predeterminado SystemUI sigue enlazado
a el, pero deja de atender los gestos, y MIUI no incluye un proveedor de reemplazo. Android
puro si lo hace: Quickstep sigue sirviendo los gestos con cualquier launcher.

Xiaomi lo declara explicitamente en su propia pantalla de ajustes: *"No se pueden usar gestos
de pantalla completa con lanzadores de terceros."*

## Intento descartado

La primera implementacion forzaba el ajuste `force_fsg_nav_bar` a 1 para reactivarlos. Fue
peor que el problema: los botones desaparecian, los gestos seguian sin responder y el
telefono quedaba sin NINGUNA forma de salir de una aplicacion. Se revirtio por completo.

Se descartaron tambien dos hipotesis intermedias: que MIUI revirtiera el ajuste de inmediato
(aguanto minutos y un reinicio) y que hiciera falta escribir tambien `navigation_mode` (MIUI
lo ajusta solo).

## Decision

Un `AccessibilityService` propio con tres ventanas `TYPE_ACCESSIBILITY_OVERLAY` en los bordes,
que ejecuta `GLOBAL_ACTION_BACK`, `GLOBAL_ACTION_HOME` y `GLOBAL_ACTION_RECENTS`.

- `TYPE_ACCESSIBILITY_OVERLAY` no necesita el permiso de superposicion y vive por encima de
  cualquier app, asi que las tiras detectan el deslizamiento desde dentro de otras apps.
- El servicio no lee la pantalla: `canRetrieveWindowContent="false"`. Solo dibuja y ejecuta.
- Es opt-in. El permiso de accesibilidad no se puede conceder desde codigo, y eso es correcto.
- Borde inferior: rapido va al inicio, sostenido muestra las apps abiertas. El discriminador
  es el tiempo que el dedo sigue apoyado DESPUES de cruzar el umbral, no la duracion total:
  medir desde el umbral evita que un arranque titubeante cuente como sostener.

Con los gestos propios activos, ocultar la barra de navegacion vuelve a ser seguro y se ofrece
como opcion, con tres candados (ver `GuardianDeGestos` y `AjustesViewModel`). El mas importante:
al reanudar el launcher, si la barra esta oculta y el servicio no esta activo, la barra vuelve
sola. `am force-stop` sobre el propio paquete desactiva su servicio de accesibilidad, asi que
ese candado no es teorico.

## Consecuencias

**A favor**
- Devuelve atras, inicio y recientes en cualquier equipo donde el sistema los rompa.
- Portable: son APIs estandar de Android, no dependen del fabricante.

**En contra, aceptado**
- No replica la sensacion nativa: es una accion disparada por un deslizamiento, sin la
  animacion de arrastrar la app en vivo. Eso lo maneja el compositor y no se puede imitar.
- En Android puro conviene dejarlo APAGADO: los gestos nativos son mejores. Falta la
  deteccion automatica para no estorbar ahi.
- El permiso de accesibilidad da miedo y con razon; se compensa con una descripcion honesta
  en la pantalla del sistema y con no pedir acceso al contenido.
- MIUI puede matar el servicio. Pendiente: guia de autostart.
- Las tiras pueden estorbar en apps que usan deslizamientos de borde. Pendiente: exclusiones.
