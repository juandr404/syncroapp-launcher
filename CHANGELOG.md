# Registro de cambios

Todos los cambios relevantes de SyncroApp Launcher. Formato basado en
[Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/); versiones segun
[SemVer](https://semver.org/lang/es/).

Cada entrada dice que cambio y, cuando la causa no es obvia, por que. Las mediciones son de un
Redmi Note 10 Pro (MIUI 14, Android 13, 1080x2400 @440dpi), que es el equipo de pruebas.

## [0.2.0] — 2026-07-29

Primera version usable a diario. El launcher paso de esqueleto funcional a algo que reemplaza
la pantalla de inicio sin extrañar la anterior.

### Agregado

- **Gestos de navegacion propios** (`ServicioGestosBorde`). Tres tiras invisibles en los bordes
  que ejecutan atras, inicio y recientes desde dentro de cualquier app. Resuelven que MIUI
  desactive los gestos del sistema al usar un launcher externo. Opt-in por accesibilidad.
  Ver [ADR-007](docs/adr/ADR-007-gestos-propios.md).
- **Ocultar la barra de navegacion**, con tres candados para no quedarse sin forma de navegar:
  solo se ofrece si los gestos propios estan activos, se niega a ocultarla si no lo estan, y al
  reanudar el launcher devuelve la barra sola si el servicio dejo de estar activo.
- **Dock inferior** de hasta cinco accesos en circulos de contorno, editables desde el menu
  contextual de cada app. Nace vacio: rellenarlo con apps adivinadas seria decidir por el usuario.
- **Poppins empaquetada** (pesos 200 a 500). Va como recurso y no como fuente descargable porque
  esas necesitan red y Google Play Services, y esta app no declara permiso de internet.
- **Estilo de reloj "reloj grande"**, ahora el predeterminado: la hora en peso 200, el dia
  completo con interletrado amplio y la fecha con año. El dia abreviado en contorno gigante
  queda como segundo estilo.
- **Iconos en las listas**: originales a color en el cajon, para reconocer una app cuyo nombre
  uno no recuerda; monocromos en el inicio.
- **Cerrar el cajon arrastrando hacia abajo**. Si la lista esta a media altura, el arrastre la
  sube primero y solo cierra cuando ya esta arriba.
- Tamaño del reloj configurable (pequeño, mediano, grande) y encabezado con "Volver" en las
  pantallas internas.
- Soporte de tecla Enter fisica en la busqueda: la accion "Ir" del teclado en pantalla cubria el
  caso normal, pero un teclado fisico o Bluetooth manda un Enter crudo que Compose no traduce.
- Analisis estatico con detekt (formato de ktlint embebido) y CI ampliado con detekt,
  `lintRelease` y `assembleRelease`.

### Corregido

- **El deslizamiento hacia arriba no abria el cajon.** El scroll vertical de la pantalla de
  inicio consumia el arrastre antes de que llegara al detector de gestos. Sin cajon tampoco
  habia forma de agregar favoritos, asi que un solo bug tapaba dos funciones.
- **El boton atras encerraba al usuario.** No hacia nada en el inicio incluso cuando la app no
  era el launcher del sistema. Ahora solo se bloquea cuando de verdad lo es.
- **Tocar la hora o la fecha no abria nada.** El filtrado de visibilidad de paquetes de
  Android 11+ impedia resolver los intents mientras la app no fuera el launcher predeterminado.
- **Los favoritos tardaban 9 segundos en aparecer** tras un arranque en frio, porque el inicio
  esperaba a que se enumeraran todas las apps del sistema. Ahora el nombre de cada favorito se
  guarda en la configuracion y la pantalla se dibuja completa en menos de 1 s, iconos incluidos.
- **Los iconos monocromos salian como cuadrados planos.** Pintar todos los pixeles opacos con
  `SRC_ATOP` borraba la forma; ahora se usa la capa `monochrome` tematizada si existe y, si no,
  se desatura solo la capa frontal del icono adaptativo.
- **El dock se dibujaba con circulos aplastados** cuando la lista era larga: al ser el ultimo
  hijo de la columna, el layout lo comprimia. Se paso a una capa propia anclada al fondo.
- **Cerrar el cajon con el gesto abria el panel de notificaciones**, porque el resto del
  arrastre caia sobre la pantalla de inicio. El cierre ahora se dispara al levantar el dedo.
- El efecto de estiramiento del borde de la lista consumia el arrastre sobrante y el gesto de
  cerrar nunca llegaba a dispararse.
- Los interruptores de Ajustes se salian de la pantalla con textos largos (faltaba `weight`) y
  usaban el color de acento de Material en vez de la escala de grises del launcher.
- Las reglas de respaldo excluian una ruta inexistente y bloqueaban la compilacion de release.
- Las tiras de gestos usaban pixeles crudos calibrados para un solo dispositivo; ahora se
  calculan desde dp para que el area sensible mida lo mismo al dedo en cualquier pantalla.

### Cambiado

- **Se dejo de forzar los gestos de MIUI.** La primera version encendia `force_fsg_nav_bar`
  para reactivarlos: los botones desaparecian, los gestos seguian muertos y el telefono quedaba
  sin ninguna forma de salir de una app. Se revirtio y se explico la causa real en la app.
- Migracion de configuracion con numero de version, para que actualizar no pierda ajustes.
- El aviso de "establecer como pantalla de inicio" salio del inicio y quedo solo en Ajustes.

### Pruebas

- 18 pruebas unitarias, detekt en 0 hallazgos, `lintRelease` y `assembleRelease` limpios.
- `ClaveDeFavoritoTest` existe por un bug real cometido en el mismo cambio: dos formatos de
  clave duplicados dejaron de coincidir en silencio, sin romper la compilacion.
- `AjustesSerializacionTest` fija el contrato de compatibilidad del archivo de configuracion,
  porque la alternativa es que una actualizacion borre los favoritos.
- APK de release 1.5 MB; 78 a 117 MB PSS en reposo; sin crashes en el APK minificado.
- Los gestos del sistema NO se pueden verificar con `adb shell input`: los eventos inyectados
  van a la ventana enfocada y no al monitor de gestos de SystemUI. Se comprobo que falla incluso
  con el launcher de Xiaomi predeterminado. Los gestos propios si se pueden probar asi, porque
  sus tiras son ventanas normales dentro de la cadena de eventos.

### Pendiente conocido

- Cobertura de pruebas concentrada en `:core:data`. Los ViewModels, la envoltura de
  `LauncherApps` y el servicio de gestos no tienen pruebas, y son el codigo de mas riesgo.
- Sin pruebas de captura de pantalla, pese a que estaban previstas.
- Los gestos propios no se apagan solos en Android puro, donde los nativos son mejores.
- Falta guia de autostart de MIUI para que el sistema no mate el servicio de gestos.
- Las tiras de borde no se pueden excluir por app.

## [0.1.0] — 2026-07-28

Primera version funcional: pantalla de inicio con reloj, favoritos, cajon de aplicaciones con
busqueda difusa, gestos basicos, temas, apps ocultas y perfil de trabajo.
