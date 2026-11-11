# SBS Escaner PDF - piloto Android

Aplicacion Android nativa de prueba para verificar el ciclo **GitHub Actions -> APK**.

## Funciones del piloto
- Tomar foto con la camara del telefono o importar varias imagenes de la galeria.
- Revisar, navegar, girar y eliminar paginas (hasta 12).
- Recortar manualmente por margenes porcentuales (sin deteccion automatica de bordes).
- Mejorar contraste basico; no incluye perspectiva automatica ni OCR.
- Exportar un PDF de varias paginas en formato A4 a Descargas/SBSScannerPDF.
- Abrir y compartir el PDF desde otras aplicaciones.

## Seguridad y limites
No utiliza servidores, ni solicita permisos de red, ni incorpora SDK de terceros.
Es una version de prueba no validada en telefonos reales. Necesita Android 10 o superior.
El APK **debug** no es una version firmada para produccion.

## Construccion
Desde esta rama, GitHub Actions compila con Java 17, Gradle 8.9 y Android SDK 35:
`gradle -p scanner-pdf :app:assembleDebug`.
Descargar artifact `ScannerPDF-APK-Debug` de la ejecucion del workflow.

El proyecto anterior de procesamiento de escaneos se uso como referencia funcional;
este es un prototipo reconstruido, no el codigo fuente original.
