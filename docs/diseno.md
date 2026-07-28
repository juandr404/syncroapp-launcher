# Documento de Diseño — Launcher Android Minimalista
**Nombre de trabajo:** "Mínimo" (placeholder)
**Versión:** 1.0 — Julio 2026
**Plataforma objetivo:** Android 10+ (optimizado para Android 13+ por themed icons)
**Estética de referencia:** pantalla negra pura, reloj tipográfico gigante con el día de la semana en outline, lista vertical de apps como texto con iconos de línea.

---

## 1. Principios de diseño

**1. El contenido es la interfaz.**
No hay chrome, cards, sombras ni contenedores. El texto ES el botón, el reloj ES el wallpaper. Si un elemento necesita un fondo o un borde para entenderse, está mal diseñado.

**2. Tipografía como arquitectura.**
La jerarquía se construye únicamente con tamaño, peso y trazo (fill vs. outline). Nunca con color de acento, nunca con iconos grandes, nunca con posición decorativa. El día en outline gigante es el techo de la jerarquía; todo lo demás vive por debajo.

**3. Negro es negro (#000000).**
El fondo por defecto es negro puro OLED. Esto no es solo estética: es ahorro de batería real y desaparición física de la pantalla. El launcher debe sentirse como tinta flotando en vidrio apagado.

**4. Cero fricción hacia la app.**
El usuario abre el launcher para irse de él. Máximo 2 gestos para llegar a cualquier app instalada (favoritos: 1 tap; cualquier otra: swipe + primeras letras). Nada de widgets promocionales, feeds, ni "descubrimiento".

**5. Configurable, no personalizable.**
Se ofrecen opciones estructurales (alineación, densidad, tema, fuentes dentro de un set curado), no libertad infinita. Cada opción del panel de ajustes debe defender su existencia. Si el 90 % de usuarios nunca la cambiaría, no existe.

---

## 2. Sistema tipográfico

### 2.1 Fuentes recomendadas (Google Fonts / OFL, aptas para empaquetar en APK)

| Rol | Fuente principal | Alternativa | Justificación |
|---|---|---|---|
| Día de la semana gigante (outline) | **Archivo** (variable: wght 100–900, wdth 62–125) | **Anton** / **Big Shoulders** | Grotesca de trazos uniformes: el stroke outline queda parejo, sin adelgazamientos que se rompan al vaciar la letra. El eje `wdth` permite ensanchar "WED" para llenar el ancho. |
| Hora (10:17) | **Archivo** (peso 500–600, tabular) | **Space Grotesk** | Misma familia que el día = cohesión. Requiere cifras tabulares (`tnum`) para que el reloj no "baile" al cambiar dígitos. |
| Fecha (10 DEC) | **Archivo** peso 400, tracking amplio | **Space Mono** | Versalitas simuladas (mayúsculas + letter-spacing 0.08–0.12em). |
| Lista de apps | **Inter** (variable) | **IBM Plex Sans** | Legibilidad máxima en tamaños medios, x-height alta, excelente rendering en Android, soporte multilenguaje amplio. |
| Opción "mono" (ajuste de usuario) | **JetBrains Mono** | **Space Mono** | Para el usuario que quiere estética terminal. Aplica a lista y fecha, no al día gigante. |

Todas OFL / Apache 2.0: se pueden empaquetar como recursos `font/` sin dependencia de red ni de Google Play Services.

### 2.2 Jerarquía de tamaños (en sp)

| Elemento | Tamaño | Peso | Tracking | Notas |
|---|---|---|---|---|
| Día de la semana (outline) | **120–160 sp** (auto-escala al ancho disponible, ~92 % del ancho útil) | 800–900 (el stroke se aplica sobre peso alto) | -0.02em | Mayúsculas. Se dimensiona con `AutoSize` acotado: min 96 sp, max 172 sp. |
| Hora | 34 sp | 600 | 0 | `tnum` activado. |
| Fecha | 16 sp | 400 | +0.10em | Mayúsculas. |
| Ítem de lista de apps | 22 sp (densidad media) | 450–500 | 0 | Ver densidades en §4.5. |
| Etiquetas secundarias (contadores, badges de notificación como texto) | 13 sp | 400 | +0.04em | Opcional, apagado por defecto. |
| Títulos en Ajustes | 20 sp | 600 | 0 | |
| Cuerpo en Ajustes | 15 sp | 400 | 0 | |
| Microcopy / hints | 13 sp | 400 | +0.02em | |

Regla: **nunca usar `dp` para texto.** Todo texto en `sp` para respetar el escalado del sistema (ver §8.3 los límites).

### 2.3 Cómo lograr el efecto outline del día

**Opción recomendada (Jetpack Compose ≥ 1.4):** `drawStyle = Stroke` en el `TextStyle`.

```kotlin
// Día de la semana en outline: trazo sin relleno
Text(
    text = "WED",
    style = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Black,   // peso alto = outline robusto
        fontSize = 140.sp,
        drawStyle = Stroke(
            width = 3.dp.toPx(),         // grosor del trazo en dp (no sp): constante visual
            join = StrokeJoin.Round,     // uniones suaves, sin picos
            cap = StrokeCap.Round
        ),
        color = colorScheme.onBackground // el trazo hereda el token de texto primario
    )
)
```

Reglas del outline:
- **Grosor del trazo: 2–3.5 dp**, escalando levemente con el tamaño de fuente (aprox. `fontSize/48`). Un trazo demasiado fino desaparece en pantallas de baja densidad; demasiado grueso cierra los ojales de la A/R/O.
- Aplicar el stroke sobre un **peso 800–900**: al vaciar una letra fina, el interior colapsa y quedan dos líneas casi pegadas.
- `StrokeJoin.Round` evita púas en vértices de W y N.
- **Nunca combinar outline + fill con sombra:** el efecto es outline puro, sin glow ni doble capa (excepto el "modo sobre wallpaper", §3.3, que añade un scrim, no una sombra al texto).
- En Views clásicas (si aplica): dos `TextPaint`, o `paint.style = Paint.Style.STROKE` con `strokeWidth`.

---

## 3. Sistema de color

### 3.1 Paleta base

| Token | Valor | Uso |
|---|---|---|
| `ink.black` | `#000000` | Fondo OLED puro |
| `ink.night` | `#101114` | Fondo "dark suave" |
| `ink.paper` | `#F6F6F3` | Fondo light (blanco cálido, no #FFF puro: menos deslumbrante) |
| `gray.900` | `#191A1D` | Superficies elevadas en dark (hoja de ajustes) |
| `gray.700` | `#3A3C40` | Divisores dark, trazos deshabilitados |
| `gray.500` | `#7D8085` | Texto terciario dark / secundario light |
| `gray.300` | `#B9BBBE` | Texto secundario dark |
| `gray.100` | `#E8E8E6` | Divisores light |
| `white.pure` | `#FFFFFF` | Texto primario dark (opcional, ver nota) |
| `white.soft` | `#EDEEEF` | Texto primario dark por defecto (evita halo en OLED) |
| `black.text` | `#141518` | Texto primario light |

Nota: sobre negro puro, el blanco `#FFFFFF` al 100 % produce "smearing" en algunos paneles OLED al hacer scroll. Por defecto el texto primario es `white.soft` (#EDEEEF); el usuario puede subirlo a puro en Ajustes → Pantalla → "Blanco máximo".

**No hay color de acento.** El único "acento" permitido es la inversión (texto en fill vs. outline) y el peso tipográfico. Los estados de error en Ajustes usan `gray` + texto explícito, no rojo.

### 3.2 Tokens semánticos

```
bg.primary          → fondo de Home y drawer
bg.surface          → hojas modales / ajustes
text.primary        → hora, nombres de apps, títulos
text.secondary      → fecha, subtítulos de ajustes
text.tertiary       → hints, metadatos
text.display-stroke → trazo del día gigante (= text.primary por defecto)
icon.default        → iconos de línea (= text.primary con alpha 0.92)
divider             → líneas separadoras (solo en Ajustes; en Home no hay divisores)
state.pressed       → overlay al presionar: text.primary al 8 % de alpha
scrim.wallpaper     → scrim del modo wallpaper (negro 40–60 %)
```

### 3.3 Temas soportados

| Tema | bg.primary | text.primary | Notas |
|---|---|---|---|
| **Dark puro** (default) | `#000000` | `#EDEEEF` | El tema insignia. |
| **Dark suave** | `#101114` | `#E8E8E6` | Para paneles LCD o quien encuentra duro el negro absoluto. |
| **Light** | `#F6F6F3` | `#141518` | Misma estructura, tinta invertida. El día gigante sigue en outline. |
| **Monocromo sobre wallpaper** | wallpaper del usuario + `scrim.wallpaper` | `#FFFFFF` o `#000000` según luminancia media del wallpaper (cálculo con `Palette` API) | El scrim garantiza contraste; si aun con scrim al 60 % no se alcanza 4.5:1, el launcher fuerza el scrim necesario y lo comunica en Ajustes. |

- Modo automático: sigue el dark/light del sistema (default: siempre dark puro; el seguimiento del sistema es opt-in).
- Sin Material You dinámico en superficie: el launcher es monocromo por identidad. (Los themed icons de Android 13 sí se tiñen con `icon.default`, no con el color dinámico del sistema.)

---

## 4. Layout y espaciado

### 4.1 Grid y unidad base

- Unidad base: **4 dp**. Escala de espaciado: 4, 8, 12, 16, 24, 32, 48, 64.
- Sin grid de columnas: el layout es una **pila vertical de una sola columna**. La única decisión horizontal es la alineación (§4.4).
- Margen lateral de contenido: **24 dp** (compacto: 20 dp; amplio: 32 dp).

### 4.2 Safe areas

- Respetar `WindowInsets` siempre: status bar, gesto de navegación inferior, display cutout.
- El bloque del reloj arranca a **min(12 % de la altura útil, 96 dp)** desde el borde superior del área segura.
- La lista de apps termina a **≥ 48 dp** del borde inferior útil, para no colisionar con el gesto de home del sistema.
- En landscape: reloj a la izquierda (40 % del ancho), lista a la derecha; misma lógica de insets.

### 4.3 Anatomía del Home

```
┌──────────────────────────────┐
│  (safe area top)             │
│                              │
│  10:17   W E D    10 DEC     │  ← bloque reloj: hora y fecha flanquean
│          (outline gigante,   │     el día, alineadas a la línea base
│           ocupa el ancho)    │     media del día gigante
│                              │
│  ── espacio 48–64 dp ──      │
│                              │
│  ○ Phone                     │  ← lista de favoritos (4–8 ítems)
│  ○ Photos                    │
│  ○ Spotify                   │
│  ○ YouTube                   │
│                              │
│  (resto: vacío intencional)  │
│  (safe area bottom)          │
└──────────────────────────────┘
```

- Bloque reloj: la hora (izquierda) y la fecha (derecha) se alinean verticalmente al **centro óptico** del día gigante, separadas de él por 12 dp. Si el día ocupa todo el ancho (configuración "full-bleed"), hora y fecha pasan a una línea propia encima del día, alineadas a los extremos.
- El espacio vacío bajo la lista **no se rellena**. El vacío es parte del diseño.

### 4.4 Alineaciones configurables

Ajuste global "Alineación": **Izquierda (default) / Centro / Derecha.**
- Afecta reloj, lista y drawer de forma solidaria (no se permite reloj centrado + lista izquierda: fragmenta la retícula).
- En alineación izquierda/derecha, los iconos de la lista van del lado de la alineación; en centro, los iconos se ocultan por defecto (una lista centrada con iconos laterales queda coja) y puede activarse "icono arriba del texto" — no recomendado, pero disponible.

### 4.5 Densidad de la lista de apps

| Densidad | Alto de fila | Texto | Icono | Gap icono–texto |
|---|---|---|---|---|
| Compacta | 44 dp | 19 sp | 18 dp | 12 dp |
| **Media (default)** | 52 dp | 22 sp | 20 dp | 16 dp |
| Amplia | 64 dp | 25 sp | 22 dp | 16 dp |

- Aún en "compacta", el target táctil de la fila completa es ≥ 48 dp de alto efectivo (44 dp de fila + inset de toque extendido). Ver §8.4.
- Máximo de favoritos en Home: 8 (con densidad amplia: 6). Más que eso, el Home deja de ser un Home y se convierte en un drawer.

---

## 5. Iconografía

### 5.1 Sistema de iconos de línea

- **Set recomendado: Lucide** (ISC license, ~1500 iconos, stroke 2 px sobre grid 24, estética neutra y consistente). Alternativa igualmente válida: **Material Symbols Outlined** (Apache 2.0, variable: ejes de weight y grade, integración natural con Android).
- Especificación de uso:
  - Grid 24 dp, renderizado a 18–22 dp según densidad (§4.5).
  - Stroke: 1.75–2 dp, cap y join redondeados.
  - Color: `icon.default` (monocromo, hereda del tema). Nunca color propio.
  - Un icono por app de sistema/popular mapeado manualmente (Phone → `phone`, Photos → `image`, Spotify → `music`, YouTube → `play`, etc.): mantener una tabla curada de ~200 mapeos paquete → icono para las apps más comunes.

### 5.2 Themed icons de Android 13+

- Si la app declara `monochrome` en su adaptive icon (Android 13+), usarlo tintado con `icon.default`. Es la fuente de mayor fidelidad de marca dentro del monocromo.
- Prioridad de resolución del icono de una fila:
  1. Mapeo curado a Lucide/Material Symbols (consistencia máxima de trazo).
  2. Capa `monochrome` del adaptive icon (Android 13+), tintada.
  3. Fallback (§5.3).
- Ajuste "Estilo de iconos": *Set del launcher (default) / Iconos del sistema (themed) / Sin iconos.*

### 5.3 Fallback cuando no hay icono monocromático

En orden:
1. **Glifo inicial:** círculo de línea (stroke 1.75 dp, 20 dp) con la primera letra del nombre de la app en Inter 500, 11 sp, mismo color. Es el fallback por defecto: mantiene ritmo visual y ayuda al escaneo alfabético.
2. Si el usuario activó "Sin iconos": nada — la lista es solo texto (opción totalmente soportada, no un estado degradado).
3. **Nunca** mostrar el icono full-color original como fallback: rompe el contrato monocromo. Si el usuario quiere iconos a color, este no es su launcher (y está bien).

---

## 6. Especificación de pantallas clave

### 6.1 Home

- Contenido: bloque reloj + lista de favoritos. Nada más. Sin dock, sin buscador visible, sin puntos de página (es una sola página).
- Interacciones:
  - Tap en app → abre (con la animación mínima de §7).
  - Tap en la hora → abre la app de reloj del sistema. Tap en la fecha → calendario. Tap en el día gigante → sin acción (es ornamento estructural; asignarle acción lo convierte en un botón gigante accidental).
  - Long-press en app → menú contextual (§6.5).
  - Long-press en zona vacía → acceso rápido a Ajustes + "Editar favoritos".
- Notificaciones: opcionalmente (off por defecto), un punto de 4 dp o un contador textual (13 sp, `text.tertiary`) a la derecha del nombre de la app. Sin badges de color.

### 6.2 App drawer / búsqueda

- **Apertura:** swipe up en cualquier zona del Home. El drawer sube como una capa del mismo fondo (no es una hoja con esquinas redondeadas: es una continuación de la pantalla).
- **Estructura:** campo de búsqueda arriba (una línea de texto con cursor, sin caja: solo un hint "Buscar" en `text.tertiary` y una línea base de 1 dp), debajo la lista alfabética completa con la misma anatomía de fila del Home.
- **Teclado:** se abre automáticamente al abrir el drawer (ajuste "Teclado automático", on por defecto). Con teclado abierto, la lista muestra resultados filtrados desde la primera letra.
- **Resultados:**
  - Coincidencia por prefijo de palabra primero, luego substring, luego fuzzy ligero. Sin resultados web, sin sugerencias de tienda, sin "acciones sugeridas". Solo apps instaladas (+ contactos y ajustes del sistema como opt-in en Ajustes → Búsqueda).
  - Si hay exactamente un resultado, **Enter lo abre**. Objetivo: "y-o-u + Enter" abre YouTube en menos de un segundo.
  - Sin resultados: "No hay resultados para '{consulta}'." — nada más, sin ilustración.
- Índice alfabético: scroll rápido por letra en el borde de la alineación activa (aparece solo durante el scroll).
- Cerrar: swipe down, botón back, o abrir una app.

### 6.3 Ajustes

Estructura plana, máximo dos niveles. Navegación tipo lista, misma tipografía del sistema del launcher.

```
Ajustes
├─ Pantalla
│   ├─ Tema (Dark puro / Dark suave / Light / Sobre wallpaper / Según sistema)
│   ├─ Alineación (Izquierda / Centro / Derecha)
│   ├─ Densidad de lista (Compacta / Media / Amplia)
│   └─ Blanco máximo (toggle)
├─ Reloj
│   ├─ Formato de hora (12 h / 24 h / según sistema)
│   ├─ Mostrar día gigante (toggle)
│   ├─ Idioma del día (según sistema / inglés) — "MIÉ" vs "WED"
│   └─ Grosor del trazo (Fino / Medio / Grueso)
├─ Apps
│   ├─ Favoritos del Home (editor de lista, arrastrar para ordenar)
│   ├─ Estilo de iconos (Set del launcher / Del sistema / Sin iconos)
│   ├─ Apps ocultas
│   └─ Puntos de notificación (toggle, off)
├─ Búsqueda
│   ├─ Teclado automático (toggle, on)
│   └─ Incluir contactos y ajustes (toggle, off)
├─ Gestos (ver §6.5)
└─ Acerca de
    ├─ Versión, licencias de fuentes e iconos
    └─ Establecer como launcher predeterminado
```

Componentes de Ajustes: filas de texto con el valor actual en `text.secondary` a la derecha; toggles como interruptor estándar de Android tintado en escala de grises. Sin iconos en las filas de ajustes (el texto basta).

### 6.4 Onboarding

Tres pantallas máximo, mismas reglas visuales del launcher (el onboarding ES la primera demostración del producto):

1. **Bienvenida:** el reloj gigante en vivo (hora real del usuario) + una línea: "Su teléfono, en silencio." Botón de texto: "Comenzar".
2. **Launcher predeterminado:** explicación de una línea + botón que dispara `RoleManager.ROLE_HOME` (Android 10+) o el selector del sistema. Estado claro si el usuario declina: "Puede cambiarlo luego en Ajustes." Nunca bloquear el uso por no ser predeterminado.
3. **Favoritos iniciales:** lista prellenada con detecciones razonables (teléfono, mensajes, cámara, navegador) que el usuario confirma o edita. Permisos: solo se pide lo estrictamente necesario en el momento de uso (contactos únicamente si activa búsqueda de contactos; notificaciones únicamente si activa los puntos). **Cero permisos en el onboarding** más allá del rol de launcher.

### 6.5 Gestos

| Gesto | Acción (default) | Configurable a |
|---|---|---|
| Swipe up | Abrir drawer/búsqueda | — (fijo: es el gesto estructural) |
| Swipe down | Panel de notificaciones del sistema | Nada / pantalla de bloqueo |
| Swipe izquierda / derecha | Nada (default) | Abrir app específica / linterna / cámara |
| Long-press en app | Menú contextual: Renombrar, Ocultar, Quitar de favoritos, Info de la app, Desinstalar | — |
| Long-press en vacío | Editar favoritos / Ajustes | — |
| Doble tap en vacío | Apagar pantalla (requiere permiso de administrador o accesibilidad — explicar con claridad antes de pedirlo) | Nada |

El menú contextual de long-press es una lista de texto flotante sobre `bg.surface`, sin iconos, anclada a la fila presionada.

---

## 7. Motion y animaciones

**Filosofía: el movimiento confirma, no entretiene.**

### Qué se anima
| Transición | Duración | Curva |
|---|---|---|
| Presión de fila (overlay `state.pressed`) | 80 ms in / 150 ms out | linear |
| Apertura del drawer (traslación + fade de la lista) | 220 ms | `EmphasizedDecelerate` (Material: cubic-bezier(0.05, 0.7, 0.1, 1.0)) |
| Cierre del drawer | 180 ms | `EmphasizedAccelerate` |
| Filtrado de resultados de búsqueda (fade cruzado de filas) | 120 ms | standard |
| Cambio de minuto en el reloj | fade cruzado 250 ms del dígito que cambia | linear |
| Cambio de día (medianoche) | fade cruzado 400 ms del día gigante | standard |
| Apertura de app | la transición estándar del sistema; el launcher solo hace fade-out de su contenido en 120 ms | — |

### Qué NO se anima (decisión explícita)
- Sin parallax, sin springs exagerados, sin stagger en la lista (las filas aparecen juntas).
- Sin animación de "reordenar" decorativa en favoritos: el arrastre es 1:1 con el dedo, con un solo desplazamiento de 150 ms para las filas que ceden espacio.
- Sin loaders ni skeletons: el launcher nunca debe tener un estado de carga visible. Si la lista de apps tarda, se muestra vacía los ~100 ms que tome, no un spinner.
- Sin animación del outline (nada de trazos que se "dibujan"): el día gigante simplemente está.
- Respeto total a "Eliminar animaciones" del sistema (`Settings.Global.ANIMATOR_DURATION_SCALE` y preferencia de accesibilidad): todas las duraciones colapsan a 0 o fades de 50 ms.

---

## 8. Accesibilidad

### 8.1 Contraste
- `text.primary` sobre `bg.primary`: #EDEEEF sobre #000000 = ~18.5:1. #141518 sobre #F6F6F3 = ~15:1. Ambos superan WCAG AAA.
- `text.secondary` y `text.tertiary` deben mantener ≥ 4.5:1 en todos los temas (gray.500 #7D8085 sobre negro = 4.6:1: es el piso; no usar grises más oscuros para texto informativo).
- El día en outline es decorativo-informativo: como su legibilidad con trazo fino es menor, la fecha textual (10 DEC) y la hora siempre acompañan y son la fuente accesible de la misma información. Ajuste "Grosor del trazo: Grueso" disponible para baja visión.
- Modo wallpaper: contraste garantizado por scrim dinámico (§3.3), nunca delegado al azar del fondo elegido.

### 8.2 TalkBack
- Cada fila de app: `contentDescription` = nombre de la app + estado ("Spotify. En favoritos. Doble toque para abrir. Mantenga presionado para más opciones.").
- Bloque reloj: un solo nodo semántico que anuncia "Miércoles 10 de diciembre, 10:17" (el día outline, la hora y la fecha no se leen como tres elementos sueltos).
- El día gigante marcado como decorativo a nivel visual pero su información incluida en el nodo del reloj.
- Drawer: anunciar "Búsqueda de aplicaciones" al abrirse; los resultados anuncian el conteo ("3 resultados").
- Orden de foco: reloj → favoritos en orden visual → (en drawer) campo de búsqueda → resultados.
- Menú contextual navegable por foco y cerrable con back.

### 8.3 Tamaños dinámicos
- Todo texto en `sp`: la app respeta el escalado del sistema hasta 200 %.
- El día gigante escala con un tope (max 172 sp de base ya autoescalada; con font scale > 1.3 se permite que pase a dos líneas o reduzca su ancho antes que romper el layout).
- A escalas grandes, la lista pasa automáticamente a densidad "Amplia" y el máximo de favoritos visible se reduce con scroll habilitado en el Home (excepción única a "sin scroll en Home").

### 8.4 Targets táctiles
- Toda fila interactiva: ≥ 48 × 48 dp de área táctil efectiva (en densidad compacta, la zona de toque se extiende 2 dp arriba y abajo de la fila de 44 dp).
- Ancho de toque de una fila = ancho completo del contenido con márgenes, no solo el texto.
- Separación mínima entre targets: 8 dp.
- El índice alfabético del drawer: 32 dp de ancho táctil aunque dibuje solo 16 dp.

---

## 9. Microcopy

### 9.1 Tono
- **Sobrio, breve, declarativo.** Sin exclamaciones, sin humor forzado, sin "¡Genial!", sin emojis en ningún texto de la interfaz.
- Español: **usted formal, LATAM neutro** (consistente con la línea editorial del equipo). Inglés: neutro, sin contracciones coloquiales excesivas.
- Frases de una línea. Si un texto necesita dos oraciones, la segunda sobra o pertenece a un enlace "Más información".
- Nunca culpar al usuario; nunca antropomorfizar la app ("no pude", "lo siento") — la app describe estados, no emociones.

### 9.2 Glosario ES / EN

| Contexto | Español | English |
|---|---|---|
| Onboarding, bienvenida | Su teléfono, en silencio. | Your phone, quieted. |
| Onboarding, CTA | Comenzar | Begin |
| Set default | Establezca Mínimo como launcher predeterminado. | Set Mínimo as your default launcher. |
| Set default, declinado | Puede cambiarlo luego en Ajustes. | You can change this later in Settings. |
| Búsqueda, hint | Buscar | Search |
| Búsqueda, vacío | No hay resultados para "{q}". | No results for "{q}". |
| Favoritos, editor | Mantenga presionado y arrastre para ordenar. | Hold and drag to reorder. |
| Ocultar app | Ocultar {app}. Podrá encontrarla con la búsqueda. | Hide {app}. You can still find it in search. |
| Permiso notificaciones | Para mostrar puntos de notificación, permita el acceso a notificaciones. | To show notification dots, allow notification access. |
| Doble tap apagar | Esta función requiere el permiso de accesibilidad. Solo se usa para apagar la pantalla. | This feature needs the accessibility permission. It is used only to turn off the screen. |
| Tema sobre wallpaper, aviso | Se aplicó un oscurecimiento para mantener el texto legible. | A dim layer was applied to keep text readable. |
| Error genérico | Algo falló. Intente de nuevo. | Something went wrong. Try again. |
| Acerca de | Mínimo, por diseño. | Minimal, by design. |

### 9.3 Reglas de escritura
- Nombres de apps: siempre el nombre que declara la app, nunca truncado con "…" antes de 24 caracteres (elipsis solo si excede el ancho).
- Fechas y horas: formato del sistema del usuario; el "10 DEC" del reloj respeta el locale ("10 DIC" en español) salvo que el usuario fije "Idioma del día: inglés".
- Botones: verbo en infinitivo o imperativo formal, una palabra cuando sea posible ("Comenzar", "Guardar", "Ocultar").

---

## Anexo — Checklist de handoff a desarrollo

1. Fuentes empaquetadas: Archivo (variable), Inter (variable), JetBrains Mono. Verificar `tnum` en la hora.
2. Outline vía `Stroke` en Compose ≥ 1.4; probar en densidades hdpi–xxxhdpi que el trazo de 2–3.5 dp no aliasea.
3. Tokens de color como `ColorScheme` propio (no Material dinámico); 4 temas + cálculo de scrim para wallpaper.
4. Insets: probar con cutout, navegación por gestos y por botones, y font scale 200 %.
5. Tabla curada paquete → icono Lucide (arrancar con top 200 apps por instalación en LATAM/global).
6. Auditoría TalkBack completa antes de la primera beta: los launchers minimalistas viven o mueren por accesibilidad, porque su público incluye a quien busca menos estímulo visual por necesidad, no solo por gusto.

**Estado:** especificación v1.0 completa; pendiente validación de prototipo en dispositivo OLED real (smearing del blanco, grosor del trazo).
