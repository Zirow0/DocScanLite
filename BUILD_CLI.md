# Збірка проекту з командного рядка

## Передумови

- Java JDK 17+ (рекомендовано JDK 21)
- Android SDK
- USB-підключений Android пристрій з увімкненою налагодженням

## Перевірка розташування Java

```bash
where java
```

## Команди збірки

### Компіляція Kotlin коду

```bash
./gradlew :app:compileDebugKotlin -Dorg.gradle.java.home="C:/Program Files/Microsoft/jdk-21.0.7.6-hotspot"
```

### Збірка Debug APK

```bash
./gradlew assembleDebug -Dorg.gradle.java.home="C:/Program Files/Microsoft/jdk-21.0.7.6-hotspot"
```

### Збірка Release APK

```bash
./gradlew assembleRelease -Dorg.gradle.java.home="C:/Program Files/Microsoft/jdk-21.0.7.6-hotspot"
```

### Повна збірка з тестами

```bash
./gradlew build -Dorg.gradle.java.home="C:/Program Files/Microsoft/jdk-21.0.7.6-hotspot"
```

## Встановлення на пристрій

### Збірка та встановлення Debug APK

```bash
./gradlew installDebug -Dorg.gradle.java.home="C:/Program Files/Microsoft/jdk-21.0.7.6-hotspot"
```

### Встановлення додатку на пристрій через adb
```bash
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Запуск додатку на пристрої

```bash
adb shell am start -n com.docscanlite/.MainActivity
```

### Перевірка підключених пристроїв

```bash
adb devices
```

## Примітки

- Параметр `-Dorg.gradle.java.home` перевизначає значення `org.gradle.java.home` з `gradle.properties`
- Шлях до Java має відповідати вашій системі
- APK файли будуть у `app/build/outputs/apk/`
- Для встановлення на пристрій потрібен увімкнений режим розробника та USB-налагодження
