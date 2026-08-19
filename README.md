# Momentum

Aplikacja na Androida do todosów i nawyków. Prywatna, dla jednej osoby, bez Google Play.

Pełna specyfikacja: [`docs/spec.html`](docs/spec.html) — jak jest teraz.
Dziennik decyzji: [`docs/decyzje.md`](docs/decyzje.md) — dlaczego tak, a nie inaczej.

## Środowisko

Wszystko siedzi w katalogu domowym, bez instalatorów i bez uprawnień administratora.
Odinstalowanie = skasowanie folderu `C:\Users\slusa\dev`.

| Narzędzie | Ścieżka | Wersja |
|---|---|---|
| JDK | `C:\Users\slusa\dev\jdk21` | Temurin 21.0.12 LTS |
| Android SDK | `C:\Users\slusa\dev\android-sdk` | platformy 36 i 37.0, build-tools 37.0.0 |
| Gradle | wrapper w repo | 9.7.0 |

Zmienne `JAVA_HOME`, `ANDROID_HOME` i `PATH` są ustawione na poziomie użytkownika
Windows, więc działają w każdym nowym terminalu.

## Budowanie

```bash
./gradlew assembleRelease      # podpisany APK w app/build/outputs/apk/release/
./gradlew installDebug         # wgranie na podłączony telefon
```

## Klucz podpisujący

Klucz leży w `C:\Users\slusa\dev\keys\momentum-release.jks`, czyli **poza repozytorium** —
tak, żeby nie dało się go wypchnąć przez pomyłkę. Hasło jest w `keystore.properties`
w katalogu projektu, też poza gitem.

Odcisk SHA-1 (potrzebny przy konfiguracji OAuth dla mostu do Google Tasks):

```
33:AF:5E:E5:68:71:F3:71:45:2E:2D:50:26:48:72:09:84:3C:5F:E3
```

> **Tego pliku nie da się odtworzyć.** Jeśli zginie, Android potraktuje kolejne
> wydania jako inną aplikację — trzeba będzie odinstalować Momentum z telefonu i
> zainstalować od zera, tracąc dane. Zrób kopię `momentum-release.jks` razem
> z hasłem w miejscu, które przetrwa awarię dysku.

## Wydania

Każde wypchnięcie na `main` uruchamia GitHub Actions, który buduje podpisany APK
i publikuje go jako wydanie `v0.1.<numer>`. Obtainium na telefonie widzi nowe
wydanie i proponuje aktualizację.

Sekrety wymagane w repozytorium (Settings → Secrets and variables → Actions):

| Sekret | Zawartość |
|---|---|
| `KEYSTORE_BASE64` | plik `.jks` zakodowany base64 |
| `KEYSTORE_PASSWORD` | hasło do keystore |
| `KEY_ALIAS` | `momentum` |
| `KEY_PASSWORD` | hasło do klucza (to samo co do keystore) |

Kodowanie klucza do wklejenia:

```bash
base64 -w0 "C:/Users/slusa/dev/keys/momentum-release.jks"
```

## Struktura

```
app/src/main/java/dev/slusa/momentum/
  MainActivity.kt          ekran startowy
  ui/theme/                paleta, typografia, rampa starzenia zadań
docs/spec.html             specyfikacja
docs/decyzje.md            dziennik decyzji
.github/workflows/         build i publikacja wydań
```
