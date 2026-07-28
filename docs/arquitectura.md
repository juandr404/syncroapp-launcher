# Documento de Arquitectura — Launcher Android Minimalista

**Versión:** 1.0 — Estado: Propuesto
**Equipo:** 1 founder + Claude Code (par programador)
**Principio rector:** cada abstracción debe justificar su complejidad. Un equipo de una persona no puede mantener la arquitectura de un equipo de veinte.

---

## 1. Stack recomendado y justificación

### 1.1 Kotlin + Jetpack Compose (decisión: Compose, sin dudarlo)

| Criterio | Compose | Views (XML) |
|---|---|---|
| Velocidad de iteración con IA como par | Alta — todo es Kotlin, un solo lenguaje, fácil de generar y revisar | Baja — contexto partido entre XML, binding y código |
| UI tipográfica custom (reloj gigante, lista de texto) | Trivial — `Text` + `Canvas` | Requiere custom views |
| Animaciones/gestos | `Modifier.pointerInput`, APIs modernas | `GestureDetector` + código imperativo |
| Rendimiento en listas | `LazyColumn` bien usado ≈ RecyclerView | RecyclerView maduro |
| Riesgo | Overhead inicial de runtime (~2-4 MB APK, arranque) | Ninguno nuevo, pero deuda a futuro |

**Trade-off que aceptamos:** Compose agrega peso al APK y al cold start frente a un launcher 100% Views (Olauncher original es Views y pesa <1 MB). Lo compensamos con R8 full mode + Baseline Profiles (sección 6). Lo que ganamos —velocidad de desarrollo con un equipo de 1— vale más que 2 MB de APK.

**Dónde Compose NO aplica:** los widgets de terceros (`AppWidgetHostView`) son Views obligatoriamente. Se integran con `AndroidView` (interop). Es el único punto de contacto con el sistema de Views.

### 1.2 minSdk / targetSdk

- **minSdk = 26 (Android 8.0)**
- **targetSdk = 35** (obligatorio por política de Play a la fecha; mantener al día cada año)
- **compileSdk = 35**

Justificación de minSdk 26:

| Candidato | Qué gana | Qué pierde |
|---|---|---|
| minSdk 23 | ~1-2% más de dispositivos | `AdaptiveIconDrawable` no existe (<26), pin shortcuts no existen (<26), código legacy en iconos |
| **minSdk 26** | Adaptive icons garantizados, `ShortcutManager` completo, `LauncherApps.Callback` estable, notification channels | ~2% del mercado (dispositivos de 2016-) |
| minSdk 29 | `RoleManager` sin fallback | ~8-10% del mercado, innecesario: el fallback es barato |

Puntos de bifurcación por API que el código debe manejar explícitamente:

- **API 29+**: `RoleManager.createRequestRoleIntent(ROLE_HOME)` — flujo moderno para pedir ser launcher predeterminado. En 26-28, fallback a `Settings.ACTION_HOME_SETTINGS`.
- **API 33+**: `AdaptiveIconDrawable.getMonochrome()` — themed icons reales. En 26-32, fallback de tinte (sección 3.5).
- **API 30+**: package visibility filtering — mitigado porque los launchers con categoría HOME reciben exención automática (sección 3.2).

### 1.3 Gradle Version Catalogs

Obligatorio desde el día 1: `gradle/libs.versions.toml` como única fuente de verdad de versiones. Con Claude Code como par, el catálogo evita el bug clásico de versiones divergentes entre módulos generados en sesiones distintas. Convention plugins en `build-logic/` (composite build) para no repetir configuración de Compose/Hilt en cada módulo.

```
gradle/libs.versions.toml
build-logic/convention/   ← plugins: launcher.android.library, launcher.compose, launcher.hilt
```

---

## 2. Arquitectura de la app

### 2.1 Capas

```
UI (Compose + ViewModel)  →  Domain (use cases opcionales)  →  Data (repositorios + fuentes)
```

- **UI:** composables sin lógica; ViewModels exponen `StateFlow<UiState>` y reciben eventos.
- **Domain:** capa *delgada y opcional*. Solo se crean use cases cuando una operación combina 2+ repositorios (ej.: "lanzar app" = registrar uso en Room + verificar app lock + delegar a LauncherApps). Para lecturas simples, el ViewModel habla directo con el repositorio. Regla anti-astronautas: no crear `GetAppsUseCase` que solo delega una línea.
- **Data:** repositorios que unifican `LauncherApps`, DataStore, Room y cachés en memoria.

### 2.2 MVVM con UDF (no MVI estricto)

**Decisión: MVVM con flujo de datos unidireccional** (un `UiState` inmutable por pantalla, eventos como funciones del ViewModel).

Por qué no MVI puro (reducers, sealed intents, middleware): un launcher tiene ~4 pantallas con estado mayormente derivado de fuentes reactivas (lista de apps, settings). El costo de MVI (boilerplate de intents/reducers, curva para razonar efectos) no compra nada aquí porque casi no hay transiciones de estado complejas. Lo que sí tomamos de MVI: **estado único e inmutable por pantalla** construido con `combine` de flows:

```kotlin
// HomeViewModel — el estado se DERIVA, no se muta
val uiState: StateFlow<HomeUiState> = combine(
    reposApps.appsFlow,          // caché reactiva de LauncherApps
    reposSettings.settingsFlow,  // Proto DataStore
    reloj.tickFlow,              // ticker del reloj
) { apps, settings, hora -> HomeUiState(...) }
 .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Cargando)
```

### 2.3 Módulos Gradle

**Trade-off central:** modularización fina (12+ módulos) da builds incrementales y límites estrictos, pero para 1 persona el overhead de navegación y wiring supera el beneficio. Propuesta: **7 módulos**, suficientes para aislar lo que de verdad se quiere aislar (el acceso al sistema Android y el diseño), sin fragmentar features diminutas.

```
:app                    ← MainActivity (la HOME activity), navegación, DI raíz
:core:ui                ← tema, tipografía, componentes (ItemApp, RelojGigante), tokens
:core:data              ← repositorios, DataStore, Room, caché de apps/iconos
:core:launcherapps      ← envoltorio de LauncherApps/RoleManager/ShortcutManager/WidgetHost
                          (TODO acceso a APIs de sistema vive aquí, detrás de interfaces)
:feature:home           ← pantalla principal: reloj, favoritos, gestos de entrada
:feature:drawer         ← lista de apps + búsqueda fuzzy
:feature:settings       ← configuración completa (la superficie más grande de UI)
```

Los gestos NO son un módulo (`:feature:gestures` sería astronautics): son un componente en `:core:ui` (`Modifier.gestosLauncher(...)`) consumido por home. Regla de dependencias: features → core, nunca feature → feature; `:app` conoce a todos.

**`:core:launcherapps` es el módulo más importante del proyecto.** Aísla todo lo hostil (APIs de sistema, bifurcaciones por versión, quirks de OEM) detrás de interfaces puras, lo que hace el resto de la app testeable con fakes.

### 2.4 Inyección de dependencias: Hilt

| | Hilt | Koin |
|---|---|---|
| Errores de grafo | Compile-time | Runtime (crash al resolver) |
| Boilerplate | Bajo con KSP | Bajo |
| Integración ViewModel/Compose/WorkManager | De primera clase | Buena |
| Costo de build | KSP agrega segundos | Cero codegen |

**Decisión: Hilt.** Con un par programador de IA generando código, los errores de wiring detectados en compilación valen oro: un grafo Koin roto se descubre al abrir la pantalla; uno de Hilt, al compilar. El costo de build es tolerable en un proyecto de este tamaño.

---

## 3. Especificidades de un launcher (lo crítico)

Esta sección es el corazón del documento: aquí es donde un launcher difiere de "una app Android normal".

### 3.1 Declararse launcher y pedir ser el predeterminado

```xml
<activity
    android:name=".MainActivity"
    android:launchMode="singleTask"
    android:stateNotNeeded="true"
    android:excludeFromRecents="true"
    android:screenOrientation="nosensor"
    android:theme="@style/Theme.Launcher.Wallpaper"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

- `HOME` + `DEFAULT` son lo que hace que el sistema ofrezca la app como launcher.
- `launchMode="singleTask"`: presionar Home debe traer la instancia existente (vía `onNewIntent`), nunca crear otra.
- `stateNotNeeded="true"`: el sistema puede recrear la activity sin bundle de estado — patrón estándar de launchers; el estado real vive en DataStore/repositorios, no en `savedInstanceState`.

**Pedir ser predeterminado:**

```kotlin
// API 29+: diálogo del sistema, la vía correcta
val rm = context.getSystemService(RoleManager::class.java)
if (rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
    launcherDeResultado.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
}
// API 26-28: solo se puede abrir la pantalla de ajustes de Home
context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
```

Detalles de UX obligatorios: detectar en `onResume` si somos el launcher activo (comparar `resolveActivity` de un intent HOME) y mostrar un banner discreto si no lo somos; en MIUI/algunos OEM el diálogo de RoleManager puede no aparecer — siempre ofrecer el botón "Abrir ajustes de inicio" como plan B.

### 3.2 Listar apps: LauncherApps, no PackageManager

**Decisión: `LauncherApps` como única fuente.** Razones:

1. **Perfiles de trabajo:** `LauncherApps.getActivityList(null, userHandle)` iterando `UserManager.userProfiles` devuelve las apps del perfil personal Y del perfil de trabajo con el `UserHandle` correcto. `PackageManager.queryIntentActivities` solo ve el perfil actual.
2. **Package visibility (API 30+):** las apps con intent-filter `HOME` reciben **exención automática** del filtrado de visibilidad — no se necesita `QUERY_ALL_PACKAGES` (declararlo sin justificación es riesgo de rechazo en Play; con la exención de launcher, no declararlo). Nota: la exención aplica cuando el sistema nos reconoce como launcher; en desarrollo, si la lista sale vacía antes de ser default, agregar un `<queries>` con el intent MAIN/LAUNCHER como cinturón de seguridad.
3. **Badged icons y estado:** `LauncherActivityInfo` da íconos con badge de perfil, `isEnabled`, y funciona con apps suspendidas/quieted.

`PackageManager` queda solo para casos puntuales (resolver el launcher default actual, info de paquete propio).

### 3.3 Escuchar instalación/desinstalación: LauncherApps.Callback

**Decisión: `LauncherApps.registerCallback`, NO BroadcastReceiver de `PACKAGE_ADDED/REMOVED`.**

- El callback entrega `onPackageAdded/Removed/Changed`, `onPackagesSuspended`, y eventos de perfil de trabajo (`onProfileAvailable`), todo con `UserHandle` — el receiver de broadcasts no distingue perfiles y los broadcasts implícitos de paquete tienen restricciones desde API 26 para receivers de manifiesto.
- Registrar en el repositorio (scope de aplicación), no en la Activity: el callback debe vivir mientras el proceso viva, y al recibir cualquier evento **invalida la caché en memoria y re-emite el flow** — toda la UI se actualiza sola por el UDF de la sección 2.2.
- Complemento: `ACTION_MANAGED_PROFILE_ADDED/REMOVED` (receiver dinámico) para creación/eliminación del perfil de trabajo completo.

### 3.4 Lanzar apps y shortcuts

- **Lanzar:** `launcherApps.startMainActivity(componentName, userHandle, sourceBounds, opciones)` — respeta el perfil (crítico: lanzar la app de trabajo del usuario de trabajo). Envolver en `try/catch` de `ActivityNotFoundException` y `SecurityException` (la app pudo desinstalarse entre el render y el tap): capturar, refrescar caché, avisar con un toast sobrio.
- **Shortcuts (long-press):** `launcherApps.getShortcuts(query, userHandle)` con `FLAG_MATCH_DYNAMIC | FLAG_MATCH_MANIFEST | FLAG_MATCH_PINNED`, y `startShortcut(...)` para lanzarlos. **Restricción dura:** solo funciona si `launcherApps.hasShortcutHostPermission()` — es decir, solo siendo el launcher predeterminado. La UI del menú contextual debe degradar con elegancia (ocultar sección de shortcuts, dejar "Info de la app" / "Desinstalar") cuando no lo somos.
- **Desinstalar / info:** intents estándar `ACTION_DELETE` y `ACTION_APPLICATION_DETAILS_SETTINGS`; para apps del perfil de trabajo, `launcherApps.startAppDetailsActivity`.

### 3.5 Íconos monocromáticos / themed icons

Estrategia en cascada (esto define la estética del producto, merece precisión):

1. **API 33+ y la app trae capa monochrome:** `(icono as? AdaptiveIconDrawable)?.monochrome` → tintar con el color de texto del tema sobre fondo transparente. Es el resultado perfecto.
2. **API 33+ sin capa monochrome, o API 26-32:** fallback de tinte — render del ícono a bitmap, aplicar `ColorMatrix` de desaturación + `PorterDuff/ColorFilter` hacia el tono del tema. Resultado aceptable pero "sucio" en íconos con mucho detalle. Alternativa más limpia para adaptive icons: usar solo la capa `foreground` desaturada.
3. **Opción del usuario:** un ajuste "Íconos: ninguno / monocromo / originales". En un launcher de texto, **"ninguno" es el default recomendado** — resuelve el problema estético de raíz y es coherente con el producto.

Cachear el resultado del pipeline (sección 4.4): el procesamiento de bitmap por ícono no puede ocurrir en cada composición.

### 3.6 Widgets: AppWidgetHost (decisión: post-v1)

Soportar widgets es el subsistema más caro de un launcher:

- `AppWidgetHost(context, hostId)` + `AppWidgetManager`; ciclo de vida estricto: `startListening()` en start del proceso de UI, `stopListening()` en stop; persistir los `appWidgetId` asignados (`allocateAppWidgetId`) en Room; flujo de binding con `ACTION_APPWIDGET_BIND` cuando no se tiene permiso; manejar widgets con configuración (`ACTION_APPWIDGET_CONFIGURE`).
- Las `AppWidgetHostView` son Views → interop `AndroidView` dentro de Compose, con manejo manual de tamaño/padding.
- **Recomendación: excluir de v1.** El público de un launcher minimalista lo tolera (Olauncher vivió años sin widgets). Diseñar el modelo de datos de la pantalla home para que agregar "filas de widget" después no sea un rewrite (la home es una lista de elementos tipados: `Reloj | Favorito | (futuro) Widget`).

### 3.7 Wallpaper

- Tema de la activity con `android:windowShowWallpaper="true"` para dibujar sobre el wallpaper del sistema, **pero** el producto es "pantalla negra": el default es un fondo sólido propio (negro puro — bonus: ahorro real en OLED) y el wallpaper del sistema como opción.
- Nunca pedir `SET_WALLPAPER` ni gestionar wallpapers en v1: abrir el picker del sistema (`Intent.ACTION_SET_WALLPAPER`) si el usuario quiere cambiarlo.

### 3.8 Botón back y botón home

- **Back en la home:** un launcher **nunca** hace `finish()`. `BackHandler` de Compose: si hay drawer/búsqueda/settings abiertos → cerrarlos; si ya estás en la home → no-op (o colapsar teclado). Con gesture navigation, registrar el handler evita que el sistema intente cerrar la task.
- **Home:** llega como `onNewIntent` (por `singleTask`). Contrato: resetear al estado raíz — cerrar drawer, limpiar búsqueda, scroll a top. Es el "escape hatch" universal del usuario y debe ser instantáneo.

### 3.9 Proceso, batería y RAM

- El proceso del launcher vive casi siempre (el sistema mantiene el HOME resident y lo reinicia de inmediato si muere). Implicación doble: (a) el cold start importa menos de lo que parece, (b) **cada MB retenido es un MB permanente en el dispositivo del usuario** — presupuesto en sección 6.
- Cero foreground services, cero wakelocks, cero polling. El reloj se actualiza con un flow que emite por minuto **solo mientras la UI está en STARTED** (`repeatOnLifecycle` / `WhileSubscribed`) + receiver de `ACTION_TIME_TICK` dinámico registrado solo en foreground.
- WorkManager únicamente para tareas diferibles (recomputar estadísticas de uso, limpieza de caché).

### 3.10 OEMs agresivos (MIUI/HyperOS, Samsung, etc.)

Realidad a diseñar, no a descubrir en producción:

| OEM | Problema típico | Mitigación |
|---|---|---|
| Xiaomi (MIUI/HyperOS) | Diálogo de RoleManager suprimido; "optimización" mata procesos no-default; gestos del sistema interfieren; permisos extra ("mostrar sobre otras apps") para shortcuts | Fallback siempre visible a ajustes de Home; pantalla de ayuda por-OEM (detectar `Build.MANUFACTURER`) con pasos concretos; probar en dispositivo real Xiaomi |
| Samsung (One UI) | Botón home con gestos propios; Samsung "Good Lock" compite; al quitar el launcher default lo revierte a One UI Home sin preguntar | `stateNotNeeded` + restauración de estado desde repositorios hace la recreación invisible; probar gesture nav y button nav |
| Huawei/Honor, Oppo/Vivo | Listas blancas de batería, matan callbacks | Mientras seamos default HOME estamos protegidos; documentar en ayuda cómo excluir de optimización de batería |

**Regla de oro:** ser el launcher predeterminado es la mejor protección contra OEMs — el sistema no mata al HOME activo y lo resucita si muere. Toda la fricción se concentra en el estado "instalado pero no default": minimizar ese estado con buen onboarding.

---

## 4. Datos y estado

### 4.1 DataStore: Proto, no Preferences

**Decisión: Proto DataStore** para configuración. Un launcher "completo en configuraciones" tendrá 40-80 settings; Preferences DataStore con 60 keys string sueltas es un campo minado de typos y defaults inconsistentes. Proto da: esquema tipado (`ajustes.proto`), defaults en un solo lugar, migraciones explícitas, y el objeto de settings completo como un solo `Flow<Ajustes>` que alimenta el `combine` de los ViewModels. Costo aceptado: el paso de codegen de protobuf y aprender la sintaxis proto (una vez).

### 4.2 Room

Solo para datos relacionales/históricos que Proto no modela bien:

- `favoritos` (orden, alias de nombre, componente + userHandle serializado)
- `apps_ocultas`, `apps_bloqueadas`
- `uso_apps` (contadores propios de lanzamientos para ranking de búsqueda — independiente de UsageStatsManager, que requiere permiso especial)
- (futuro) `widgets` (appWidgetId, posición)

Sin Room no hay launcher serio; pero mantenerlo en 4-5 tablas. Exportar schema (`room.schemaLocation`) desde el día 1 para migraciones testeables.

### 4.3 Repositorios

```
RepositorioApps        ← LauncherApps + Callback + caché en memoria (fuente de verdad reactiva)
RepositorioAjustes     ← Proto DataStore
RepositorioFavoritos   ← Room
RepositorioUso         ← Room + UsageStatsManager (si hay permiso)
```

### 4.4 Cachés en memoria

- **Lista de apps:** `MutableStateFlow<List<AppInfo>>` en `RepositorioApps`, poblada en el arranque del proceso (una sola carga de `LauncherApps` en `Dispatchers.Default`, ordenada y normalizada para búsqueda), invalidada por el Callback. La UI jamás llama a `LauncherApps` directamente.
- **Íconos:** `LruCache<ClaveIcono, ImageBitmap>` (clave = componente + user + modo de tinte + tamaño), presupuesto ~1/16 de la memoria de la app. En un launcher de texto con íconos opcionales, este caché puede ser diminuto o vacío — otra ventaja del default "sin íconos".
- **Persistencia de derivados costosos:** no en v1. Si el procesamiento monocromo resultara caro, cachear bitmaps procesados en disco (`cacheDir`) con invalidación por versionCode del paquete.

---

## 5. Features técnicos

### 5.1 Búsqueda fuzzy

Sin librería: un scorer propio de ~80 líneas es suficiente y controlable. Normalizar (lowercase, sin tildes — clave para español: "cámara" debe matchear "camara"), y puntuar en cascada: prefijo exacto > prefijo de palabra > iniciales ("wa" → WhatsApp, "gm" → Google Maps) > subsecuencia contigua > subsecuencia dispersa; desempate por `uso_apps` (frecencia). Ejecutar sobre la caché en memoria con `debounce(50ms)` en el flow del query — a 300 apps es sub-milisegundo, no hay problema de rendimiento que resolver. Feature de lujo barato: "lanzar automáticamente si queda un solo resultado" (ajuste opcional, marca de la casa de estos launchers).

### 5.2 Gestos

- **Decisión: `Modifier.pointerInput` con `detectDragGestures`/`detectTapGestures` y `awaitEachGesture` para casos compuestos.** Nada de `GestureDetector` clásico (es de Views; mezclarlo con Compose crea dos sistemas de gestos peleando).
- Gestos de la home: swipe up (drawer), swipe down (panel de notificaciones — `expandNotificationsPanel` no es API pública confiable: usar el gesto solo si `StatusBarManager` accesible por reflexión funciona, y aceptar que en algunos OEM no; documentarlo), swipe left/right (acciones configurables: app, linterna, etc.), double-tap (bloquear pantalla — requiere `AccessibilityService`, ver trade-off abajo), long-press (settings del launcher).
- **Double-tap para apagar pantalla:** requiere un `AccessibilityService` (o Device Admin, peor). Es fricción de permisos y escrutinio de Play (hay que justificar accesibilidad en la ficha). Recomendación: implementarlo como opt-in claramente explicado, con el servicio deshabilitado por defecto, y declaración de accesibilidad en Play Console. Si Play lo objeta, es una feature amputable sin tocar el resto.
- Umbrales de gesto configurables en un solo lugar (`TokensGestos` en `:core:ui`) — los defaults de distancia/velocidad son de las cosas que más se ajustan en beta.

### 5.3 Límites de uso y estadísticas

- `UsageStatsManager` requiere `PACKAGE_USAGE_STATS` (permiso especial: `AppOpsManager.checkOpNoThrow` para verificar, `Settings.ACTION_USAGE_ACCESS_SETTINGS` para pedir). Flujo de onboarding explícito con pantalla propia que explique el porqué (transparencia = confianza en un producto "sin trackers").
- Límites de uso ("máximo 30 min de X al día"): el launcher solo puede *disuadir* (interceptar el lanzamiento desde el launcher, mostrar fricción), no bloquear de verdad una app abierta por otras vías, salvo con AccessibilityService. Ser honestos en el copy: es un límite suave. Los contadores gruesos ("hoy llevas N aperturas") salen de nuestra tabla `uso_apps` sin ningún permiso.

### 5.4 Badges de notificaciones

- Única vía real: `NotificationListenerService`. **Trade-off serio:** acceso de lectura a TODAS las notificaciones = el permiso más sensible que puede pedir esta app, en tensión directa con la promesa "sin trackers, privado".
- **Decisión: opt-in, apagado por defecto, y el servicio solo cuenta — nunca lee `extras` ni contenido.** Procesar `onNotificationPosted/Removed` a un mapa `paquete → tieneNotificacion` en memoria. En un launcher minimalista el badge es un punto sutil junto al nombre, no un contador. Declararlo explícitamente en la política de privacidad. Si el usuario nunca lo activa, el servicio nunca corre.

### 5.5 App lock con BiometricPrompt

- `androidx.biometric:biometric` con `BIOMETRIC_WEAK | DEVICE_CREDENTIAL` como authenticators (cubre PIN si no hay huella).
- Interceptar en el punto único de lanzamiento (`RepositorioApps.lanzar` / use case `LanzarApp`): si el paquete está en `apps_bloqueadas`, pedir biometría antes de `startMainActivity`. Ventana de gracia configurable (ej. 60 s tras autenticar).
- **Límite honesto:** protege el lanzamiento *desde el launcher*. Una app abierta desde recents o una notificación no pasa por nosotros. Documentarlo en el ajuste ("bloquea el acceso desde la pantalla de inicio").

### 5.6 Backup / restore

Doble vía, complementarias:

1. **Auto Backup** (`android:dataExtractionRules`): incluir DataStore y Room, excluir cachés. Gratis, transparente, cubre migración de dispositivo vía Google. Nota: es best-effort y opaco — no depender solo de él.
2. **Export/import manual a JSON** vía SAF (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT` — cero permisos de storage). Serializar con `kotlinx.serialization` un `BackupV1` con campo `version` desde el día 1 (la migración de backups viejos es inevitable; el campo de versión cuesta una línea hoy y salva el futuro). Los favoritos referencian componentes que pueden no existir en el dispositivo destino: importar tolerante, reportando "3 apps del respaldo no están instaladas".

---

## 6. Rendimiento

- **Cold start:** aunque el HOME rara vez arranca en frío (sección 3.9), el primer arranque tras boot/actualización es la primera impresión y el OEM puede matar el proceso. Meta: **TTID < 400 ms en gama media**. Tácticas: la home renderiza primero con settings de DataStore (rápido) y favoritos; la carga completa de `LauncherApps` corre en paralelo y puebla el drawer — el usuario ve reloj + favoritos de inmediato. Cero inicialización eager en `Application` fuera de Hilt + DataStore; `App Startup` para lo demás.
- **Baseline Profiles:** plugin `androidx.baselineprofile` con módulo `:baselineprofile` (Macrobenchmark) recorriendo el flujo crítico: arranque → swipe drawer → búsqueda → lanzar app. Reduce el jank de las primeras ejecuciones post-instalación, que es exactamente cuando el usuario decide si se queda.
- **R8 full mode + resource shrinking** en release desde el primer release (no "al final" — los problemas de keep rules se encuentran de a uno, no de a cincuenta). Meta de APK: **< 5 MB**.
- **Jank en la lista:** `LazyColumn` con `key = componente+user`; `UiState` con clases estables (`kotlinx.collections.immutable` para listas); lambdas de click no capturantes o `remember`izadas; el reloj gigante en su propio composable para que el tick por minuto no recomponga la lista; verificación con Layout Inspector (recomposition counts) y compose compiler metrics en CI cuando haya sospecha.
- **Presupuesto de RAM (proceso residente): < 120 MB PSS en reposo, ideal ~80 MB.** Medir con `dumpsys meminfo` en la matriz de dispositivos en cada release. Los sospechosos habituales: caché de íconos (limitar/vaciar en `onTrimMemory`), bitmaps de wallpaper (no retener), fugas de listeners (los Callbacks de LauncherApps se registran una vez, en scope singleton).

---

## 7. Testing

Pirámide adaptada al riesgo real de un launcher (el riesgo está en la integración con el sistema, no en la lógica pura):

| Nivel | Herramientas | Qué cubre |
|---|---|---|
| Unit | JUnit + kotlinx-coroutines-test + Turbine | Scorer de búsqueda fuzzy (el más testeable y el que más lo agradece), ViewModels con fakes de repositorios, migraciones de backup JSON, lógica de tinte de íconos |
| UI Compose | `createComposeRule` + fakes de `:core:launcherapps` | Drawer filtra al escribir, long-press abre menú contextual, back cierra drawer, settings togglean estado |
| Screenshot | **Roborazzi** (sobre Robolectric) | Home, drawer, settings en light/dark, tamaños de fuente, locales es/en. Para un producto cuya propuesta de valor ES la estética tipográfica, los screenshot tests son tests de producto, no de infraestructura |
| Instrumented | Room migrations test, Macrobenchmark | Migraciones reales de DB; startup/jank en device farm o dispositivo local |
| E2E manual | Checklist en dispositivos reales | Flujo de default launcher, perfil de trabajo, reboot, actualización de app, kill del proceso |

**Roborazzi sobre Paparazzi:** ambos renderizan sin emulador, pero Roborazzi corre dentro de Robolectric — convive con tests que necesitan `Context`/recursos Android y permite interacción antes del snapshot. Paparazzi es más rápido pero no comparte source set con tests Robolectric, y este proyecto va a necesitar Robolectric de todos modos (DataStore, Room in-memory).

**Los fakes de `:core:launcherapps` son la inversión de testing #1:** un `RepositorioAppsFake` con lista controlable y emisión manual de eventos "paquete instalado/removido" habilita el 80% de los tests de UI y ViewModel sin emulador.

**Matriz mínima de dispositivos físicos** (comprar usados, es la inversión más rentable del proyecto):

1. Pixel (stock, gesture nav) — referencia
2. Samsung gama media (One UI, button nav + gesture nav) — mayor base instalada
3. Xiaomi/Redmi (MIUI/HyperOS) — el OEM más hostil
4. Un gama baja API 26-28 (2 GB RAM) — presupuesto de RAM y fallbacks pre-RoleManager

---

## 8. CI/CD

### 8.1 GitHub Actions

Dos workflows:

**`ci.yml`** (push a `main` + PRs):
1. `gradle/actions/setup-gradle` con caché
2. `detekt` (con `detekt-formatting` que embebe ktlint — **una sola herramienta de estilo, no dos**; detekt con formatting rules y baseline inicial, sin ktlint standalone)
3. Unit tests + tests Robolectric/Roborazzi (`verifyRoborazziDebug` — falla el PR si un screenshot cambia sin actualizar el golden)
4. `assembleDebug` + `lintDebug`

**`release.yml`** (tag `v*`):
1. Todo lo anterior
2. `bundleRelease` firmado — keystore y credenciales en GitHub Secrets (base64), **respaldo del keystore en dos lugares fuera de GitHub**: perder el keystore de upload es recuperable con Play App Signing (activarlo desde el primer release, no negociable), perder el de firma sin Play App Signing es fatal
3. Subida a Play con `r0adkll/upload-google-play` (service account JSON en Secrets) al track `internal`
4. Adjuntar APK universal al GitHub Release (usuarios sin Play / F-Droid futuro)

### 8.2 Tracks de Play y cadencia

```
internal (founder + 5 amigos, minutos)  →  closed beta (50-200 usuarios, 1-2 semanas)
→ production con staged rollout 10% → 50% → 100%
```

Open testing: opcional; para un launcher, closed beta con un canal de feedback (grupo de Telegram/Discord) rinde más que open testing anónimo. Nunca saltar de internal a production: los bugs de OEM solo aparecen con diversidad de dispositivos de la beta.

### 8.3 Versionado

- **Usuario:** SemVer simplificado `MAJOR.MINOR.PATCH` (`1.4.2`) en `versionName`.
- **`versionCode`:** entero monotónico manual en `libs.versions.toml` (o derivado del run number de Actions). Evitar esquemas derivados de git "ingeniosos": con releases de baja frecuencia, un número que se incrementa a mano en el PR de release es más simple y auditable.
- Changelog por release en `fastlane/metadata` (formato que Play y F-Droid entienden) — prepara el terreno para F-Droid sin costo extra.

---

## 9. Riesgos técnicos top 5 y mitigaciones

| # | Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|---|
| 1 | **Fragmentación OEM** — MIUI/Samsung rompen el flujo de default launcher, matan callbacks, o interfieren gestos; los bugs solo aparecen en dispositivos reales | Alta | Alto (reviews de 1 estrella concentradas por OEM) | Matriz física de 4 dispositivos (sección 7); pantalla de ayuda por-OEM; closed beta con diversidad de dispositivos antes de production; `:core:launcherapps` concentra los workarounds en un solo módulo |
| 2 | **Política de Google Play** — permisos sensibles (Usage Access, Notification Listener, Accessibility) generan rechazos o suspensiones en revisiones automatizadas | Media | Muy alto (bloquea distribución) | Todos los permisos sensibles son opt-in y amputables (feature flags); declaraciones completas en Play Console; formularios de permisos justificados con video; el core del producto (launcher + búsqueda + settings) funciona con CERO permisos especiales |
| 3 | **Deriva de plataforma Android** — cada targetSdk anual cambia reglas (package visibility, broadcasts, foreground); un launcher toca más superficie de sistema que una app normal | Alta | Medio (trabajo anual obligado) | minSdk 26 reduce ramas; toda API de sistema detrás de interfaces en `:core:launcherapps` con bifurcaciones por versión centralizadas; presupuestar 2-4 semanas/año para el bump de targetSdk |
| 4 | **Presupuesto de RAM/rendimiento como proceso residente** — un launcher pesado se percibe como "el teléfono va lento" y el OEM lo castiga | Media | Alto (churn silencioso) | Presupuesto explícito (<120 MB PSS) medido por release; default "sin íconos" minimiza la caché de bitmaps; `onTrimMemory` implementado; Macrobenchmark en releases |
| 5 | **Bus factor = 1** — todo el conocimiento vive en el founder y en sesiones de Claude Code | Alta | Alto | ADRs en `docs/adr/` para cada decisión de este documento; CLAUDE.md del repo con las reglas críticas (módulos, presupuestos, patrones de LauncherApps); módulos con interfaces claras hacen cada sesión de IA productiva sin contexto tribal; screenshot tests como documentación ejecutable de la UI |

---

## Apéndice A — Orden de construcción sugerido (v1)

1. Esqueleto: módulos + version catalog + convention plugins + Hilt + CI verde
2. `:core:launcherapps`: listar apps + lanzar + Callback + fakes de test
3. Home mínima: reloj + lista de favoritos + intent-filter HOME + flujo de default launcher
4. Drawer + búsqueda fuzzy + long-press (info/desinstalar/shortcuts)
5. Settings (Proto DataStore) + Room (favoritos, ocultas, uso)
6. Gestos + themed icons/tinte + backup JSON
7. Baseline Profiles + Roborazzi + beta cerrada
8. Post-v1: badges (opt-in), app lock, límites de uso, widgets

## Apéndice B — Decisiones registradas (resumen ADR)

- **ADR-001:** Compose sobre Views — velocidad de iteración > 2 MB de APK. *Aceptado.*
- **ADR-002:** minSdk 26 / targetSdk 35. *Aceptado.*
- **ADR-003:** MVVM + UDF, sin MVI estricto. *Aceptado.*
- **ADR-004:** 7 módulos, `:core:launcherapps` como frontera con el sistema. *Aceptado.*
- **ADR-005:** Hilt sobre Koin — errores de grafo en compile-time. *Aceptado.*
- **ADR-006:** LauncherApps como única fuente de apps; sin `QUERY_ALL_PACKAGES`. *Aceptado.*
- **ADR-007:** Proto DataStore sobre Preferences. *Aceptado.*
- **ADR-008:** Widgets fuera de v1; modelo de home extensible. *Aceptado.*
- **ADR-009:** Notification Listener opt-in, solo conteo, apagado por defecto. *Aceptado.*
- **ADR-010:** Roborazzi para screenshot tests. *Aceptado.*
