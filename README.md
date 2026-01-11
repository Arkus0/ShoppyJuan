# 🛒 ShoppyJuan - Android App

Aplicación nativa de Android desarrollada en Kotlin para gestión de listas de compras y recetas con colaboración en tiempo real.

> **Migración completa** de la PWA original a Kotlin/Android con Jetpack Compose.

## ✨ Características Implementadas

### 🎯 Funcionalidades Core
- ✅ **Gestión de listas**: Crear, editar, eliminar y archivar listas de compras
- ✅ **Items inteligentes**: Añadir artículos con cantidad, unidad y categoría automática
- ✅ **Recetas**: Guardar y buscar recetas con ingredientes exportables a listas
- ✅ **Búsqueda online**: Integración con TheMealDB API (1000+ recetas)
- ✅ **Categorización automática**: 700+ productos con emojis y 10 categorías
- ✅ **Entrada por voz**: Añadir items hablando con SpeechRecognizer
- ✅ **Escaneo de códigos**: CameraX + ML Kit para escanear códigos de barras
- ✅ **Autenticación**: Login/Register con Supabase Auth
- ✅ **Colaboración en tiempo real**: Supabase Realtime para listas compartidas
- ✅ **Notificaciones push**: Firebase Cloud Messaging
- ✅ **Material 3**: Diseño moderno con soporte de tema oscuro

### 🏗️ Arquitectura

```
MVVM + Clean Architecture
├── Presentation (UI)
│   ├── Jetpack Compose
│   └── Material 3
├── Domain (Business Logic)
│   ├── Models
│   └── Use Cases
└── Data (Sources)
    ├── Local: Room Database
    ├── Remote: Supabase + TheMealDB
    └── Device: Camera, Voice, Storage
```

**Stack Técnico:**
- Kotlin 1.9.20
- Jetpack Compose (UI declarativa)
- Room (Base de datos local)
- Supabase (Backend, Auth, Realtime)
- Hilt (Inyección de dependencias)
- Retrofit + OkHttp (Networking)
- CameraX + ML Kit (Barcode scanning)
- Firebase Cloud Messaging (Push notifications)
- Coil (Carga de imágenes)
- Coroutines + Flow (Asincronía)

## 📦 Estructura del Proyecto

```
app/src/main/java/com/arkus/shoppyjuan/
├── data/
│   ├── local/
│   │   ├── dao/              # Room DAOs
│   │   ├── entity/           # Room entities
│   │   └── ShoppyDatabase.kt # Database setup
│   ├── remote/
│   │   ├── api/              # MealDB API
│   │   ├── dto/              # Data Transfer Objects
│   │   └── mapper/           # DTO to Domain mappers
│   ├── auth/                 # Supabase Auth
│   ├── realtime/             # Supabase Realtime
│   ├── push/                 # Firebase FCM
│   ├── speech/               # Voice input manager
│   ├── barcode/              # Barcode scanner
│   └── repository/           # Repository implementations
├── domain/
│   ├── model/                # Domain models (ShoppingList, Recipe, etc.)
│   ├── repository/           # Repository interfaces
│   └── util/                 # ProductCategorizer, utilities
├── di/                       # Hilt modules
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   └── RepositoryModule.kt
└── presentation/             # UI Layer (Compose)
    ├── auth/                 # Login/Register screens
    ├── home/                 # Home dashboard
    ├── lists/                # Shopping lists
    ├── listdetail/           # List detail + items
    ├── recipes/              # Recipes browser
    ├── components/           # Reusable UI components
    ├── navigation/           # Navigation graph
    └── theme/                # Material 3 theme
```

## 🚀 Configuración e Instalación

### Requisitos
- **Android Studio**: Hedgehog | 2023.1.1 o superior
- **JDK**: 17
- **SDK mínimo**: Android 7.0 (API 24)
- **SDK objetivo**: Android 14 (API 34)

### 1. Clonar el repositorio

```bash
git clone https://github.com/Arkus0/ShoppyJuan.git
cd ShoppyJuan/android
```

### 2. Configurar Supabase

1. Crea un proyecto en [https://supabase.com](https://supabase.com)
2. Ve a **Settings > API** y copia:
   - `URL` (ej: `https://xxx.supabase.co`)
   - `anon public` key
3. Actualiza `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "SUPABASE_URL", "\"https://TU_PROYECTO.supabase.co\"")
buildConfigField("String", "SUPABASE_ANON_KEY", "\"TU_ANON_KEY_AQUI\"")
```

4. Ejecuta el siguiente SQL en **SQL Editor** para crear las tablas:

```sql
-- Ver schema completo en: /android/docs/supabase-schema.sql
-- (Incluye tablas: shopping_lists, list_items, recipes, profiles, etc.)
```

### 3. Configurar Firebase (Opcional - para Push Notifications)

1. Ve a [Firebase Console](https://console.firebase.google.com)
2. Crea un nuevo proyecto o usa uno existente
3. Añade tu app Android:
   - Package name: `com.arkus.shoppyjuan`
   - Descarga `google-services.json`
4. Coloca `google-services.json` en: `android/app/google-services.json`

### 4. Sincronizar Gradle

```bash
./gradlew sync
```

### 5. Ejecutar la app

```bash
# Desde Android Studio: Click en "Run" ▶️
# O desde terminal:
./gradlew installDebug
```

## 📱 Compilar para Producción

### 1. Generar Keystore

```bash
keytool -genkey -v -keystore shoppyjuan.keystore \
  -alias shoppyjuan \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### 2. Configurar firma en `app/build.gradle.kts`

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../shoppyjuan.keystore")
            storePassword = "TU_PASSWORD"
            keyAlias = "shoppyjuan"
            keyPassword = "TU_PASSWORD"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ...
        }
    }
}
```

### 3. Compilar APK Release

```bash
./gradlew assembleRelease
```

📍 APK generado en: `app/build/outputs/apk/release/app-release.apk`

### 4. Compilar Android App Bundle (AAB) para Play Store

```bash
./gradlew bundleRelease
```

📍 AAB generado en: `app/build/outputs/bundle/release/app-release.aab`

## 🎮 Características Avanzadas

### 🎤 Entrada por Voz

Permite añadir items hablando. Requiere permiso `RECORD_AUDIO`.

```kotlin
// Uso en ListDetailScreen
VoiceInputButton(
    onVoiceResult = { text -> /* añadir item */ },
    onStartListening = { /* iniciar */ }
)
```

### 📷 Escaneo de Códigos de Barras

Escanea códigos EAN, UPC, QR, etc. Requiere permiso `CAMERA`.

```kotlin
// Uso en AddItemDialog
BarcodeScannerScreen(
    scannerManager = barcodeScannerManager,
    onBarcodeScanned = { barcode -> /* buscar producto */ }
)
```

### 🔔 Notificaciones Push

Recibe notificaciones cuando:
- Se añade un item a una lista compartida
- Alguien marca un item como completado
- Te comparten una nueva lista
- Se añade una nota

### 🔄 Colaboración en Tiempo Real

Sincroniza cambios en tiempo real con Supabase Realtime:
- Actualización instantánea de items
- Presence tracking (usuarios online)
- Broadcast de eventos

### 🏷️ Categorización Automática

Sistema inteligente con 700+ productos y 10 categorías:

```
🥕 Frutas y Verduras
🥩 Carnes y Pescados
🥛 Lácteos y Huevos
🍪 Despensa
❄️ Congelados
☕ Bebidas
🧹 Hogar y Limpieza
✨ Higiene Personal
🐕 Mascotas
📦 Otros
```

### 🍳 Búsqueda de Recetas (TheMealDB)

- 1000+ recetas internacionales
- Filtros por categoría y área
- Traducción automática de ingredientes
- Exportar ingredientes a lista de compras

## 🧪 Testing

```bash
# Unit tests
./gradlew test

# Instrumented tests (requiere emulador o dispositivo)
./gradlew connectedAndroidTest

# Coverage report
./gradlew jacocoTestReport
```

## 📚 Documentación Adicional

- `/android/docs/supabase-schema.sql` - Schema completo de Supabase
- `/android/docs/api-integration.md` - Guía de integración de APIs
- `/android/docs/architecture.md` - Decisiones arquitectónicas

## 🐛 Solución de Problemas

### Error: "Supabase URL not configured"
→ Verifica que hayas configurado las variables en `build.gradle.kts`

### Error: "google-services.json not found"
→ Descarga el archivo desde Firebase Console y colócalo en `android/app/`

### Error de compilación con Room
→ Ejecuta: `./gradlew clean` y vuelve a compilar

### CameraX no funciona en emulador
→ Usa un dispositivo físico para probar el escaneo de códigos

## 📄 Licencia

Este proyecto está bajo licencia MIT. Ver `LICENSE` para más detalles.

## 👨‍💻 Autor

**Arkus0**
- GitHub: [@Arkus0](https://github.com/Arkus0)
- PWA Original: [Lista-compra-online](https://github.com/Arkus0/Lista-compra-online)

---

## 🎯 Roadmap

### v1.1 (Próximo)
- [ ] Widget de Android
- [ ] Soporte para Wear OS
- [ ] Backup automático
- [ ] Modo offline completo

### v1.2
- [ ] Integración con Google Assistant
- [ ] Comparación de precios (supermercados)
- [ ] Lista de favoritos inteligente
- [ ] Sugerencias basadas en historial

---

¿Preguntas? Abre un [issue](https://github.com/Arkus0/ShoppyJuan/issues) 🚀
