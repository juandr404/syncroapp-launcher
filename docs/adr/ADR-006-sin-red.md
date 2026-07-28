# ADR-006: Sin permiso de internet, sin rastreadores

**Estado:** Aceptado · **Fecha:** 2026-07-28

## Decision

La aplicacion no declara `android.permission.INTERNET` ni incluye ningun SDK de analitica,
publicidad o telemetria.

## Motivos

Un launcher ve cada aplicacion que usted abre y con que frecuencia. Es, de lejos, la app con
el perfil de datos mas sensible del telefono. La unica garantia solida frente a eso no es una
politica de privacidad: es que el codigo no tenga forma de enviar nada.

Sin el permiso de internet, la promesa es verificable por cualquiera desde los ajustes del
sistema o descompilando el APK, sin tener que confiar en el autor.

## Permisos que si se declaran

| Permiso | Para que | Nivel |
|---|---|---|
| `EXPAND_STATUS_BAR` | Gesto de deslizar hacia abajo | Normal, no requiere confirmacion |

## Consecuencia

Cualquier funcion futura que necesite red (clima, por ejemplo) tendra que vivir en un modulo
opcional aparte y explicar el permiso al activarse. El nucleo del producto se queda sin red
para siempre.
