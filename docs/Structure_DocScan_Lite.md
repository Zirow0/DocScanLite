# 📱 DocScan Lite - Актуальна структура проекту

**Версія**: 3.0
**Дата оновлення**: 03 грудня 2025
**Статус**: 🟢 Активна розробка

---

## 📊 Загальний огляд

DocScan Lite реалізується за принципами **Clean Architecture** з модульною структурою:

```
DocScanLite/
├── app/              # Презентаційний шар (UI, Navigation, ViewModels)
├── core/             # Спільні компоненти (Utils, DI, Extensions)
├── data/             # Шар даних (Repository, Database, DataSource)
├── domain/           # Бізнес-логіка (UseCases, Models, Interfaces)
└── image-processing/ # Модуль обробки зображень
```

---

## 🎯 Модуль `app` - Презентаційний шар

### Основна структура

```
app/src/main/java/com/docscanlite/
├── DocScanApp.kt           # Application клас
├── MainActivity.kt         # Single Activity
├── navigation/             # Навігація
│   ├── NavGraph.kt        # Композиція навігаційних графів
│   └── Destinations.kt    # Sealed class з маршрутами
└── ui/                    # UI компоненти
    ├── theme/            # Material Design 3 тема
    ├── components/       # Reusable компоненти
    └── screens/          # Екрани додатку
```

---

### 🎨 Тема (`ui/theme/`)

```kotlin
Theme.kt    // Головна тема з Light/Dark режимами
Color.kt    // Кольорова палітра Material 3
Type.kt     // Типографіка (Roboto)
```

**Реалізовано**:
- ✅ Material Design 3
- ✅ Light/Dark themes
- ✅ Dynamic colors (Android 12+)

---

### 🖼️ Спільні компоненти (`ui/components/`)

```kotlin
CameraPreview.kt  // Camera preview з CameraX
```

**Примітка**: Більшість компонентів інтегровані безпосередньо в екрани

---

### 📱 Екрани додатку (`ui/screens/`)

#### 1. **Splash Screen** (`splash/`)
```
SplashScreen.kt  // Екран завантаження з логотипом
```
**Статус**: ✅ Реалізовано

---

#### 2. **Gallery Screen** (`gallery/`)
```
GalleryScreen.kt      // Галерея документів (сітка)
GalleryViewModel.kt   // Управління станом галереї
```

**Функціонал**:
- Grid layout (3 колонки)
- Lazy loading
- Сортування та пошук
- Навігація до деталей документу

**Статус**: ✅ Базовий функціонал реалізовано

---

#### 3. **Camera Screen** (`camera/`)
```
CameraScreen.kt      // Екран камери з preview
CameraViewModel.kt   // Управління камерою
```

**Функціонал**:
- CameraX integration
- Real-time preview
- Capture button
- Flash control (Auto/On/Off)
- Camera switching (Front/Back)

**Статус**: ✅ Реалізовано

---

#### 4. **Edit Screen** (`edit/`) ⭐ Модульна архітектура

```
edit/
├── EditScreen.kt              # Головний екран редагування
├── EditViewModel.kt           # Координатор для всіх вкладок
├── EditUiState.kt            # UI стани
│
├── bounds/                   # Вкладка визначення границь
│   ├── BoundsTab.kt         # UI для редагування границь
│   └── BoundsViewModel.kt   # Логіка границь
│
├── crop/                     # Вкладка обрізання
│   ├── CropTab.kt           # UI для crop інструменту
│   └── CropViewModel.kt     # Логіка crop
│
├── filter/                   # Вкладка фільтрів
│   ├── FilterTab.kt         # UI з фільтрами
│   └── FilterViewModel.kt   # Застосування фільтрів
│
├── adjust/                   # Вкладка налаштувань
│   ├── AdjustTab.kt         # Sliders (brightness/contrast)
│   ├── AdjustViewModel.kt   # Логіка коригувань
│   └── AdjustmentOverlay.kt # Overlay з контролами
│
├── rotate/                   # Вкладка повороту
│   ├── RotateTab.kt         # UI з кнопками повороту
│   └── RotateViewModel.kt   # Логіка обертання
│
└── components/               # Компоненти редагування
    ├── PreviewImage.kt      # Відображення зображення
    ├── ImageFrame.kt        # Рамка навколо зображення
    ├── TabContentWrapper.kt # Wrapper для вкладок
    │
    └── frames/              # Інтерактивні рамки
        ├── ImageTransform.kt            # Zoom/Pan state
        ├── ZoomablePreviewImage.kt      # Зображення з zoom
        ├── QuadrilateralFrameOverlay.kt # 4-точкова рамка (bounds)
        └── RectangularFrameOverlay.kt   # 8-точкова рамка (crop)
```

**Ключові особливості**:
- **Модульна архітектура**: Кожна вкладка - окремий ViewModel
- **EditViewModel як координатор**: Керує станом між вкладками
- **Bitmap caching**: `originalPreviewBitmap` + `croppedPreviewBitmap`
- **Shared ImageTransform**: Синхронізація zoom/pan між шарами
- **Frame widgets**: Reusable компоненти для інтерактивних рамок

**Статус**: ✅ Повністю реалізовано та відрефакторено

---

#### 5. **Document Details Screen** (`document/`)
```
DocumentDetailsScreen.kt      # Деталі документа
DocumentDetailsViewModel.kt   # Управління документом
```

**Функціонал**:
- Full-screen перегляд
- Metadata відображення
- Дії: Edit, Delete, Share

**Статус**: ✅ Реалізовано

---

#### 6. **Bounds Edit Screen** (`boundsedit/`)
⚠️ **LEGACY** - Старий екран для редагування границь

```
BoundsEditScreen.kt       # [DEPRECATED]
BoundsEditViewModel.kt    # [DEPRECATED]
```

**Примітка**: Функціонал перенесено в `edit/bounds/`

**Статус**: ⚠️ Потребує видалення або рефакторингу

---

#### 7. **Settings Screen** (`settings/`)
```
SettingsScreen.kt  // Налаштування додатку
```

**Статус**: 🚧 Базова реалізація

---

## 🧩 Модуль `core` - Спільні компоненти

```
core/src/main/java/com/docscanlite/core/
├── di/              # Dependency Injection (Hilt)
│   ├── DatabaseModule.kt
│   ├── RepositoryModule.kt
│   └── ...
├── utils/           # Утиліти
│   ├── ImageUtils.kt        # Робота з зображеннями
│   ├── FileUtils.kt         # Файлова система
│   ├── PermissionManager.kt # Runtime permissions
│   └── Extensions.kt        # Kotlin extensions
└── constants/       # Константи
```

### Ключові утиліти

**ImageUtils.kt**:
```kotlin
fun loadBitmapFromFile(file: File, maxSize: Int): Bitmap?
fun getOrientedImageDimensions(file: File): Pair<Int, Int>
fun getImageOrientation(file: File): Int
fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap
fun saveBitmapToFile(bitmap: Bitmap, file: File, quality: Int)
```

**FileUtils.kt**:
```kotlin
fun getTempImageFile(context: Context): File
fun getDocumentImageFile(context: Context, documentId: String): File
fun deleteFile(file: File): Boolean
```

**Статус**: ✅ Базові утиліти реалізовані

---

## 💾 Модуль `data` - Шар даних

```
data/src/main/java/com/docscanlite/data/
├── local/
│   ├── database/
│   │   ├── AppDatabase.kt      # Room database
│   │   ├── DatabaseMigrations.kt
│   │   └── Converters.kt
│   ├── entity/
│   │   └── DocumentEntity.kt   # Room entity
│   ├── dao/
│   │   └── DocumentDao.kt      # Data Access Object
│   └── preferences/
│       └── UserPreferences.kt  # DataStore
├── repository/
│   └── DocumentRepositoryImpl.kt
└── mapper/
    └── DocumentMapper.kt       # Entity <-> Domain
```

### Database Schema

**DocumentEntity**:
```kotlin
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val originalPath: String,
    val processedPath: String?,
    val thumbnailPath: String?,
    val bounds: List<Float>?,      // [x1,y1, x2,y2, x3,y3, x4,y4]
    val brightness: Float?,
    val contrast: Float?,
    val saturation: Float?,
    val filterName: String?,
    val rotationAngle: Float?,
    val createdAt: Long,
    val modifiedAt: Long
)
```

**Статус**: ✅ Базова структура реалізована

---

## 🎯 Модуль `domain` - Бізнес-логіка

```
domain/src/main/java/com/docscanlite/domain/
├── model/
│   ├── Document.kt            # Domain model
│   └── ProcessingSettings.kt
├── repository/
│   ├── DocumentRepository.kt  # Інтерфейс
│   └── SettingsRepository.kt
├── usecase/
│   ├── document/
│   │   ├── GetDocumentsUseCase.kt
│   │   ├── SaveDocumentUseCase.kt
│   │   ├── UpdateDocumentUseCase.kt
│   │   └── DeleteDocumentUseCase.kt
│   └── processing/
│       └── [Planned]
└── common/
    └── Result.kt              # Sealed class для результатів
```

**Статус**: 🚧 Частково реалізовано

---

## 🖼️ Модуль `image-processing` - Обробка зображень

```
image-processing/src/main/java/com/docscanlite/imageprocessing/
├── ImageProcessor.kt          # Головний процесор
├── detector/
│   └── DocumentBoundsDetector.kt  # Canny Edge Detection
├── transform/
│   └── PerspectiveTransform.kt    # 4-point transform
└── filters/
    ├── FilterOption.kt        # Enum фільтрів
    └── [Individual filters]
```

### Ключові компоненти

**DocumentBoundsDetector.kt** ⭐:
- Повна реалізація оператора Кенні
- Gaussian blur → Sobel → Non-max suppression → Hysteresis
- Морфологічна обробка
- Багатокритеріальна оцінка кандидатів
- **Точність**: ~92%

**PerspectiveTransform.kt**:
- 4-точкова трансформація перспективи
- Корекція спотворень
- Вирівнювання документа

**ImageProcessor.kt**:
- Застосування фільтрів
- Корекція яскравості/контрасту
- Поворот зображення

**Статус**: ✅ Ключові алгоритми реалізовані

---

## 📦 Ресурси (`app/src/main/res/`)

```
res/
├── values/
│   ├── strings.xml         # Рядки (англійська)
│   ├── colors.xml          # Кольори Material 3
│   └── themes.xml          # XML теми
├── values-uk/
│   └── strings.xml         # Українська локалізація
├── values-night/
│   └── colors.xml          # Темна тема
├── drawable/               # Векторні іконки
├── mipmap/                 # Іконка додатку
└── xml/
    └── file_paths.xml      # FileProvider paths
```

**Статус**: ✅ Базові ресурси створені

---

## 🔧 Build Configuration

```
DocScanLite/
├── build.gradle.kts         # Project-level Gradle
├── settings.gradle.kts      # Modules configuration
├── gradle.properties        # Gradle properties
│
├── app/
│   └── build.gradle.kts    # App module dependencies
├── core/
│   └── build.gradle.kts
├── data/
│   └── build.gradle.kts
├── domain/
│   └── build.gradle.kts
└── image-processing/
    └── build.gradle.kts
```

### Ключові залежності

**app/build.gradle.kts**:
- Jetpack Compose (UI)
- Navigation Compose
- Hilt (DI)
- CameraX
- Coil (Image loading)
- Room Database

**image-processing/build.gradle.kts**:
- OpenCV (planned)
- ML Kit (planned)

---

## 📊 Поточний статус реалізації

| Модуль | Прогрес | Статус |
|--------|---------|--------|
| **Navigation** | 90% | ✅ Працює |
| **Theme System** | 100% | ✅ Завершено |
| **Camera** | 85% | ✅ Базовий функціонал |
| **Gallery** | 70% | 🚧 В розробці |
| **Edit Screen** | 95% | ✅ Відрефакторено |
| **Document Details** | 80% | ✅ Базовий функціонал |
| **Settings** | 30% | 🚧 Базова UI |
| **Image Processing** | 75% | ✅ Ключові алгоритми |
| **Database** | 80% | ✅ Працює |
| **OCR** | 0% | ❌ Не розпочато |
| **Export** | 0% | ❌ Не розпочато |

---

## 🎯 Наступні кроки

### Короткостроковий план (Фаза 2):
1. ✅ ~~Рефакторинг Edit Screen~~ (Завершено)
2. 🔄 Завершити Gallery функціонал
3. 🔄 Реалізувати Export (PDF, Image)
4. 🔄 Додати більше фільтрів обробки

### Довгостроковий план (Фаза 3):
1. ❌ OCR Integration (ML Kit + Tesseract)
2. ❌ Advanced filters
3. ❌ Cloud sync (v2.0)
4. ❌ Папки та організація (v2.0)

---

## 📝 Технічні рішення

### Архітектурні патерни:
- ✅ Clean Architecture (Domain/Data/Presentation)
- ✅ MVVM pattern
- ✅ Repository pattern
- ✅ UseCase pattern (в розробці)
- ✅ Single Activity Architecture

### Підходи до розробки:
- ✅ Jetpack Compose (декларативний UI)
- ✅ Kotlin Coroutines (асинхронність)
- ✅ StateFlow/Flow (реактивність)
- ✅ Hilt (Dependency Injection)
- ✅ Modularization (багатомодульна структура)

### Оптимізації:
- ✅ Bitmap caching (зменшення завантажень з диску)
- ✅ Lazy loading (Gallery)
- ✅ Image downsampling (зменшення використання пам'яті)
- ✅ Coroutine scope management
- ✅ Compose recomposition optimization

---

## 🐛 Відомі проблеми та TODO

### Критичні:
- ⚠️ BoundsEditScreen (legacy) потребує видалення
- ⚠️ Немає error handling для camera permission denied
- ⚠️ Відсутня валідація при збереженні документів

### Середні:
- 🔸 Gallery не має pull-to-refresh
- 🔸 Немає пагінації в Gallery
- 🔸 Document Details - мінімальний функціонал
- 🔸 Settings UI не завершена

### Низькі:
- 🔹 Немає анімацій переходів між екранами
- 🔹 Відсутні unit tests
- 🔹 Немає UI tests

---

## 📚 Документація

Основні документи проекту:

1. **Structure_DocScan_Lite.md** (цей файл) - Архітектура проекту
2. **Description_DocScan_Lite.md** - Опис функціоналу та UI/UX
3. **Canny_Edge_Detection_Algorithm.md** - Алгоритм визначення границь
4. **00-02_ТЗ_*.md** - Технічне завдання (3 файли)

---

**Останнє оновлення**: 03 грудня 2025
**Автор**: DocScan Lite Development Team
**Версія документу**: 3.0
