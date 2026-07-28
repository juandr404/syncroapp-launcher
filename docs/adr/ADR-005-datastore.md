# ADR-005: DataStore tipado con JSON en vez de Proto

**Estado:** Aceptado · **Fecha:** 2026-07-28

## Contexto

El launcher apunta a tener muchas opciones de configuracion. Guardarlas como claves sueltas
(Preferences DataStore) seria un campo minado de erratas y valores por defecto repetidos en
cada punto de lectura.

## Decision

Un unico objeto `AjustesLauncher` serializado con `kotlinx.serialization` sobre
`DataStore<AjustesLauncher>`.

## Motivos

Se conservan las ventajas que motivaban usar Proto —esquema tipado, valores por defecto en un
solo lugar, migraciones explicitas— sin agregar la generacion de codigo de protoc al build.
El archivo resultante ademas es legible a simple vista, lo que ayuda a depurar.

Detalles que importan:

- `ignoreUnknownKeys`: un archivo escrito por una version mas nueva no rompe una version vieja.
- `encodeDefaults`: el archivo siempre queda completo.
- Ante corrupcion se restauran los valores por defecto en vez de fallar: quedarse sin pantalla
  de inicio es el peor fallo posible de este producto.

## Cuidado al evolucionar el modelo

Agregar campos o valores nuevos a un enum es seguro. **Quitar un valor de un enum no lo es**:
un archivo viejo que lo contenga fallara al leerse y la configuracion se restablecera.
