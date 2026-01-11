# ShoppyJuan - Android App

Aplicación nativa de Android desarrollada en Kotlin para gestión de listas de compras y recetas.

## 🏗️ Arquitectura

- **Patrón**: MVVM + Clean Architecture
- **UI**: Jetpack Compose
- **Base de datos**: Room (SQLite)
- **DI**: Hilt
- **Navegación**: Navigation Compose
- **Imágenes**: Coil
- **Backend**: Supabase (PostgreSQL, Auth, Realtime)

## 📦 Estructura del Proyecto

```
app/
├── data/
│   ├── local/
│   │   ├── dao/           # Data Access Objects
│   │   ├── entity/        # Room entities
│   │   └── ShoppyDatabase.kt
│   └── repository/        # Repository implementations
├── domain/
│   ├── model/            # Domain models
│   └── repository/       # Repository interfaces
├── di/                   # Dependency Injection modules
└── presentation/         # UI layer
    ├── components/       # Reusable UI components
    ├── home/            # Home screen
    ├── lists/           # Shopping lists screen
    ├── listdetail/      # List detail screen
    ├── recipes/         # Recipes screen
    ├── navigation/      # Navigation setup
    └── theme/           # Material 3 theme
```

## 🚀 Características

### Core
- ✅ Crear, editar y eliminar listas de compras
- ✅ Añadir artículos con cantidad, unidad y categoría
- ✅ Marcar artículos como completados
- ✅ Guardar recetas
- ✅ Navegación con Bottom Navigation Bar
- ✅ Tema Material 3

### En desarrollo
- 🔄 Integración con Supabase (auth, realtime)
- 🔄 Compartir listas (códigos de 6 dígitos)
- 🔄 Búsqueda de recetas online (TheMealDB API)
- 🔄 Notificaciones push
- 🔄 Categorización automática de productos
- 🔄 Entrada por voz
- 🔄 Escaneo de códigos de barras

## 🛠️ Configuración

### Requisitos
- Android Studio Hedgehog | 2023.1.1+
- JDK 17
- Gradle 8.2+
- SDK mínimo: 24 (Android 7.0)
- SDK objetivo: 34 (Android 14)

### Instalación

1. Clona el repositorio:
```bash
git clone https://github.com/Arkus0/ShoppyJuan.git
cd ShoppyJuan/android
```

2. Configura Supabase (opcional):
   - Crea un proyecto en [Supabase](https://supabase.com)
   - Actualiza `app/build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "SUPABASE_URL", "\"TU_URL\"")
   buildConfigField("String", "SUPABASE_ANON_KEY", "\"TU_KEY\"")
   ```

3. Abre el proyecto en Android Studio

4. Sincroniza Gradle:
```bash
./gradlew sync
```

5. Ejecuta la app:
```bash
./gradlew installDebug
```

## 📱 Compilar APK

### Debug
```bash
./gradlew assembleDebug
```

### Release
```bash
./gradlew assembleRelease
```

El APK se generará en: `app/build/outputs/apk/`

## 🧪 Testing

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

## 📄 Licencia

Este proyecto está bajo licencia MIT.

## 👨‍💻 Autor

Desarrollado por Arkus0
