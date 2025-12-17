# Налаштування мовних даних Tesseract OCR

## Про Tesseract OCR

Цей проєкт використовує Tesseract OCR для розпізнавання тексту з підтримкою української мови.
Tesseract потребує мовні файли (traineddata) для роботи.

## Як завантажити мовні дані

### Автоматичне завантаження (Рекомендовано)

Мовні файли автоматично завантажуються при першому запуску додатку.
OCR модуль завантажить необхідні файли з репозиторію Tesseract.

### Ручне завантаження

Якщо автоматичне завантаження не працює, виконайте наступні кроки:

1. **Завантажте мовні файли**

   Перейдіть на офіційний репозиторій Tesseract:
   https://github.com/tesseract-ocr/tessdata_best

   Завантажте наступні файли:
   - `ukr.traineddata` - українська мова (кирилиця)
   - `eng.traineddata` - англійська мова (опціонально, для змішаних текстів)

   **Альтернативно (швидші, але менш точні моделі):**
   https://github.com/tesseract-ocr/tessdata_fast
   - `ukr.traineddata`
   - `eng.traineddata`

2. **Створіть папку tessdata в assets**

   ```
   DocScanLite/
   └── ocr/
       └── src/
           └── main/
               └── assets/
                   └── tessdata/
                       ├── ukr.traineddata
                       └── eng.traineddata
   ```

3. **Скопіюйте файли**

   Помістіть завантажені `.traineddata` файли в папку `ocr/src/main/assets/tessdata/`

## Підтримувані мови

Tesseract підтримує більше 100 мов. Ось деякі з них:

| Код | Мова | Файл |
|-----|------|------|
| ukr | Українська | ukr.traineddata |
| eng | Англійська | eng.traineddata |
| rus | Російська | rus.traineddata |
| pol | Польська | pol.traineddata |
| deu | Німецька | deu.traineddata |
| fra | Французька | fra.traineddata |
| spa | Іспанська | spa.traineddata |

Повний список: https://github.com/tesseract-ocr/tessdata_best

## Використання в коді

### Українська мова (за замовчуванням)

```kotlin
val ocrProcessor = OcrProcessor(context)
ocrProcessor.initialize("ukr") // або "ukr+eng" для української та англійської
```

### Кілька мов одночасно

```kotlin
ocrProcessor.initialize("ukr+eng+rus") // Українська, англійська та російська
```

### Перевірка статусу

```kotlin
if (ocrProcessor.isReady()) {
    val result = ocrProcessor.processImage(bitmap)
    // Обробка результату
}
```

## Розміри файлів

- **tessdata_best** (найкраща точність): ~10-15 MB на мову
- **tessdata_fast** (швидкість): ~900 KB - 2 MB на мову
- **tessdata** (баланс): ~3-5 MB на мову

Рекомендуємо **tessdata_best** для найкращої якості розпізнавання української.

## Troubleshooting

### Помилка: "Failed to initialize Tesseract"

**Рішення:**
1. Перевірте що файли `.traineddata` знаходяться в правильній папці
2. Переконайтесь що назва файлу відповідає коду мови (наприклад, `ukr.traineddata`)
3. Перевірте що файл не пошкоджений (розмір > 0 bytes)

### Помилка: "Failed to copy language file"

**Рішення:**
1. Файл не знайдено в assets. Завантажте мовні файли та помістіть їх в `assets/tessdata/`
2. Перевірте що build процес правильно копіює assets файли

### Низька точність розпізнавання

**Рішення:**
1. Використовуйте `tessdata_best` замість `tessdata_fast`
2. Покращте якість вхідного зображення (контрастність, роздільна здатність)
3. Використовуйте фільтри обробки зображення перед OCR
4. Переконайтесь що текст чіткий та добре освітлений

## Додаткові ресурси

- Офіційна документація Tesseract: https://tesseract-ocr.github.io/
- Tesseract4Android бібліотека: https://github.com/adaptyteam/tesseract4android
- Тренування власних моделей: https://github.com/tesseract-ocr/tesstrain

## Ліцензія

Tesseract OCR та мовні дані розповсюджуються під Apache License 2.0.
