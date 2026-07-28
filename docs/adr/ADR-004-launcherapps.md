# ADR-004: `LauncherApps` como unica fuente de apps, sin `QUERY_ALL_PACKAGES`

**Estado:** Aceptado · **Fecha:** 2026-07-28

## Decision

Las aplicaciones se leen con `LauncherApps.getActivityList()` recorriendo los perfiles de
`UserManager.userProfiles`, y los cambios llegan por `LauncherApps.registerCallback()`.
No se declara el permiso `QUERY_ALL_PACKAGES`.

## Motivos

1. **Perfiles de trabajo.** `LauncherApps` devuelve las apps de cada perfil con su `UserHandle`
   correcto. `PackageManager.queryIntentActivities` solo ve el perfil actual, y lanzar una app
   corporativa con el usuario equivocado falla.
2. **Visibilidad de paquetes.** Desde Android 11 una app no ve el resto de paquetes instalados,
   pero las apps con `intent-filter` de HOME quedan exentas. Pedir `QUERY_ALL_PACKAGES` seria
   pedir un permiso que no se necesita. Como red de seguridad para el momento en que la app esta
   instalada pero todavia no es el launcher predeterminado, el manifiesto declara un bloque
   `<queries>` con el intent MAIN/LAUNCHER.
3. **Eventos mas precisos.** El callback distingue instalado, desinstalado, cambiado, suspendido
   y eventos de perfil, todo con usuario. Los broadcasts de paquete no distinguen perfiles y
   ademas estan restringidos para receptores declarados en el manifiesto desde Android 8.

## Consecuencia

`FuenteApps` mantiene la lista en memoria como unica fuente de verdad. La interfaz nunca consulta
al sistema directamente: cualquier evento invalida la cache y todo se redibuja solo.
