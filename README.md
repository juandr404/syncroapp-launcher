# SyncroApp Launcher

Un launcher minimalista para Android. Pantalla negra, la hora en tipografia grande y fina, y sus
aplicaciones como una lista. Sin cuadriculas de iconos, sin widgets, sin feeds, sin publicidad.

**Sin rastreadores y sin permiso de internet.** Un launcher ve cada app que usted abre; este no
tiene forma tecnica de contarselo a nadie.

![Pantalla de inicio](docs/capturas/inicio.png)

---

## Estado

**v0.1.0 — funcional y en uso diario, todavia temprano.** Es un proyecto personal publicado como
software libre. No esta en Google Play y no hay planes de publicarlo alli: se instala desde el
APK de las releases o compilandolo usted mismo.

### Lo que ya funciona

- Pantalla de inicio en dos estilos: **reloj grande** (hora, dia completo espaciado y fecha) o
  **dia en contorno** (el dia abreviado a sangre). Tamano pequeno, mediano o grande.
- Se dibuja completa en **menos de un segundo**, iconos incluidos: los favoritos no esperan a que
  el sistema termine de enumerar las aplicaciones instaladas.
- Lista de favoritos (hasta 8), con reordenar, renombrar y quitar. Se autorrepara: un favorito
  cuya app se desinstalo desaparece en vez de quedar como fila muerta.
- Iconos: originales a color en el cajon (para reconocer una app cuyo nombre no recuerda) y
  monocromos en el inicio. O sin iconos, si prefiere solo texto.
- Cajon de aplicaciones con busqueda difusa: tolera erratas, ignora tildes ("camara" encuentra
  "Cámara"), busca por iniciales ("gm" encuentra "Google Maps") y Enter abre el primer resultado.
- Gestos: deslizar arriba abre el cajon, deslizar abajo despliega las notificaciones, mantener
  presionado el fondo abre Ajustes.
- **Recupera los gestos de navegacion que MIUI apaga** (ver mas abajo).
- Ocultar aplicaciones del cajon y de la busqueda.
- Aplicaciones de perfil de trabajo, diferenciadas.
- Temas: negro puro, oscuro suave, claro y segun el sistema. Alineacion izquierda/centro/derecha,
  tres densidades de lista, formato de 12 o 24 horas.
- Accesibilidad: descripciones para TalkBack, areas tactiles de 48 dp minimo, todo el texto en
  `sp` para respetar el escalado del sistema.

### Lo que falta

- Fuentes empaquetadas y selector de tipografia (hoy usa la del sistema).
- Conjunto propio de iconos de linea para las aplicaciones mas comunes.
- Bienestar digital: pausa consciente antes de abrir apps marcadas como distractoras.
- Bloqueo de apps con biometria, respaldo y restauracion de la configuracion.
- Gestos configurables (hoy son fijos).

## Instalacion

Descargue el APK de la seccion de releases e instalelo. Despues, en la app, toque **Establecer
como pantalla de inicio**.

> En MIUI, HyperOS y otras capas, el dialogo del sistema a veces no aparece. Para ese caso la app
> ofrece siempre un segundo boton que abre directamente los ajustes de pantalla de inicio.

## Los gestos de navegacion en Xiaomi

MIUI y HyperOS **desactivan los gestos de pantalla completa** cuando el launcher predeterminado no
es el suyo, y dejan el telefono en botones. No es un fallo de esta app: le ocurre a todos los
launchers externos. Y no lo hacen una sola vez: se verifico en un Redmi Note 10 Pro que MIUI
revierte el ajuste repetidamente, incluso despues de reactivarlo a mano por adb.

Esta app puede reponerlos sola cada vez que vuelve al inicio, pero para escribir un ajuste del
sistema necesita `WRITE_SECURE_SETTINGS`, un permiso que solo se concede por adb: no existe forma
de pedirlo con un dialogo, y ese requisito es justamente lo que lo hace seguro.

1. Active **Depuracion USB (ajustes de seguridad)** en Opciones de desarrollador.
2. Con el telefono conectado, ejecute una sola vez:

```bash
adb shell pm grant dev.syncroapp.launcher android.permission.WRITE_SECURE_SETTINGS
```

3. En la app, active **Ajustes → Gestos del sistema → Mantener los gestos activos**.

Para revocarlo, el mismo comando con `revoke` en lugar de `grant`. Sin conceder el permiso la app
funciona igual: la opcion queda desactivada y Ajustes muestra el comando con un boton para
copiarlo. En equipos que no son Xiaomi la seccion no aparece.

## Compilar

Necesita el SDK de Android y un JDK 17 o superior (sirve el que trae Android Studio).

```bash
git clone https://github.com/juandr404/syncroapp-launcher.git
cd syncroapp-launcher
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/`. Para instalarlo en un dispositivo conectado:

```bash
./gradlew installDebug
```

Pruebas unitarias:

```bash
./gradlew test
```

Analisis estatico (detekt, con las reglas de formato de ktlint embebidas):

```bash
./gradlew detekt
```

Para que corrija el estilo automaticamente en lugar de solo reportarlo:

```bash
./gradlew detekt -PdetektAutoCorrect
```

Antes de publicar conviene compilar la variante de release: aplica R8 y descubre problemas de
`keep rules` que la de depuracion no puede ver.

```bash
./gradlew assembleRelease
```

## Arquitectura

Kotlin + Jetpack Compose, `minSdk 26`, `targetSdk 35`.

```
:app                  actividad HOME, navegacion, pantallas y ViewModels
:core:ui              tema, tipografia, componentes y gestos
:core:data            ajustes (DataStore), buscador difuso, reloj del sistema
:core:launcherapps    frontera con el sistema: LauncherApps, RoleManager, acciones
```

El modulo importante es `:core:launcherapps`. Todo lo que depende del sistema operativo —listar
y lanzar aplicaciones, pedir ser el launcher predeterminado, las diferencias entre versiones de
Android y entre fabricantes— vive detras de interfaces. El resto de la app no sabe que existe
`LauncherApps`, lo que la hace testeable y concentra los parches por fabricante en un solo sitio.

El estado se deriva de flujos reactivos: si instala una app, cambia un ajuste o pasa un minuto,
la interfaz se recalcula sola. No hay estado mutable repartido por las pantallas.

Documentacion:

- [Decisiones de arquitectura (ADR)](docs/adr/README.md) — que se decidio, por que y que se sacrifico.
- [Sistema de diseno](docs/diseno.md) — tipografia, color, espaciado, motion, accesibilidad.
- [Arquitectura completa](docs/arquitectura.md) — incluye lo que aun no esta implementado.

## Privacidad

- No declara el permiso de internet. Verificable en los ajustes del sistema o descompilando el APK.
- Sin analitica, sin publicidad, sin SDK de terceros.
- Toda la configuracion se guarda solo en su dispositivo.

Permisos declarados:

- `EXPAND_STATUS_BAR` — nivel normal, para el gesto de deslizar hacia abajo.
- `WRITE_SECURE_SETTINGS` — **no se puede conceder desde la app**; requiere adb. Se usa
  unicamente para devolver los gestos de navegacion en Xiaomi (ver arriba) y solo si usted lo
  concede y activa la opcion. Si no lo hace, la app nunca lo tiene.
- Un bloque `<queries>` para poder listar las aplicaciones instaladas.

No se declara `QUERY_ALL_PACKAGES`: los launchers estan exentos del filtrado de visibilidad de
paquetes de Android 11+, asi que no hace falta.

## Licencia

[GPL-3.0](LICENSE). Puede usarlo, estudiarlo, modificarlo y redistribuirlo; los trabajos derivados
deben mantenerse bajo la misma licencia.
