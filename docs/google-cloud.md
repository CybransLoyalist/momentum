# Konfiguracja Google Cloud dla mostu głosowego

Jednorazowa robota w przeglądarce, około 40 minut. Koszt zero, karta niepotrzebna.
Wymaga zalogowania na **to samo konto Google, którego używasz na telefonie** — to jego
Lista zadań będzie mikrofonem.

Otwórz [console.cloud.google.com](https://console.cloud.google.com) i idź po kolei.

---

## 1. Projekt

Menu projektu na górnym pasku → **Nowy projekt**.

- Nazwa: `Momentum`
- Organizacja: bez organizacji

Utwórz i **przełącz się na niego** — łatwo o tym zapomnieć i konfigurować przez pół
godziny nie ten projekt, w którym się jest.

## 2. Włącz Google Tasks API

**Interfejsy API i usługi → Biblioteka** → wyszukaj `Google Tasks API` → **Włącz**.

To jedyne API, którego potrzebujemy.

## 3. Ekran zgody

**Interfejsy API i usługi → Ekran akceptacji OAuth** (w nowszym układzie konsoli:
**Google Auth Platform**).

- Typ użytkownika: **Zewnętrzny**
- Nazwa aplikacji: `Momentum`
- Adres e-mail pomocy: twój
- Dane kontaktowe dewelopera: twój adres

Reszty pól nie wypełniaj — logo, strona domowa i polityka prywatności są potrzebne
dopiero przy weryfikacji, przez którą nie przechodzimy.

## 4. Zakres uprawnień

W sekcji **Dostęp do danych** (albo **Zakresy**) → **Dodaj lub usuń zakresy** →
wklej w filtr:

```
https://www.googleapis.com/auth/tasks
```

Zaznacz go i zapisz.

To zakres **zapisu, nie tylko odczytu** — i tak ma być. Momentum musi kasować wpisy po
swojej stronie, żeby Lista zadań pozostała pusta i pełniła wyłącznie rolę mikrofonu.

Google oznaczy ten zakres jako „wrażliwy". To oznacza dwie rzeczy, obie dla nas
nieszkodliwe: przy pierwszym logowaniu zobaczysz ekran „ta aplikacja nie została
zweryfikowana" (klikasz raz w życiu przez **Zaawansowane → Przejdź do Momentum**),
oraz dożywotni limit stu użytkowników projektu. Jest jeden użytkownik: ty.

## 5. Opublikuj aplikację ⚠️

**To jest krok, którego nie wolno pominąć.**

W **Odbiorcy** (albo na ekranie akceptacji) znajdź stan publikacji i kliknij
**Opublikuj aplikację** → potwierdź przejście na **W produkcji**.

Dlaczego to takie ważne: dokumentacja Google wiąże siedmiodniowe wygasanie tokenu ze
stanem **Testowanie**. Zostawiona w testach aplikacja **przestanie działać po tygodniu**
i będziesz musiała logować się co siedem dni do końca świata. Przejście na „W produkcji"
usuwa ten warunek i **nie wymaga przechodzenia weryfikacji**.

Jedyną ceną jest ten jednorazowy ekran ostrzeżenia z punktu 4.

## 6. Identyfikator klienta

**Interfejsy API i usługi → Dane logowania → Utwórz dane logowania →
Identyfikator klienta OAuth**.

- Typ aplikacji: **Android**
- Nazwa: `Momentum`
- Nazwa pakietu: `dev.slusa.momentum`
- Odcisk cyfrowy certyfikatu podpisywania SHA-1:

```
33:AF:5E:E5:68:71:F3:71:45:2E:2D:50:26:48:72:09:84:3C:5F:E3
```

Utwórz. **Skopiuj identyfikator klienta** — wygląda mniej więcej tak:

```
123456789012-abcdefghijklmnopqrstuvwxyz012345.apps.googleusercontent.com
```

Prześlij mi go, wtedy dokończę stronę aplikacji.

> Klient typu Android **nie ma klucza tajnego** i nie musi go mieć — tożsamość aplikacji
> potwierdza para nazwa pakietu plus podpis. Dlatego identyfikator klienta może spokojnie
> siedzieć w repozytorium.

---

## O czym pamiętać

**To jest odcisk klucza wydaniowego.** Momentum instalowane przez Obtainium to build
release, podpisany kluczem z `C:\Users\slusa\dev\keys\momentum-release.jks`. Build debug
ma inny podpis i inną nazwę pakietu (`dev.slusa.momentum.debug`) — jeśli kiedyś most ma
działać także na buildzie debug, trzeba dodać **drugi** identyfikator klienta z odciskiem
klucza debug.

**Zgubienie klucza wydaniowego psuje też to.** Nowy klucz to nowy odcisk, czyli nowy
identyfikator klienta i konfiguracja od nowa — obok utraty możliwości aktualizacji
aplikacji. Kopia klucza jest równie ważna jak kopia danych.

**Nie używaj tej Listy zadań do niczego innego.** Momentum kasuje z niej wpisy po
zaciągnięciu. Kasuje wyłącznie te, które sama zaciągnęła, po zapamiętanych
identyfikatorach — ale i tak nie ma powodu trzymać tam czegokolwiek, na czym ci zależy.

## Jak sprawdzić, że działa

Po skonfigurowaniu strony aplikacji:

1. Powiedz do telefonu „dodaj zadanie kup mleko".
2. Otwórz Zadania Google — wpis ma tam być.
3. Poczekaj kilka minut i otwórz Momentum — wpis ma być na liście zakupów, bo `kup`
   jest słowem kluczowym.
4. Wróć do Zadań Google — ma być pusto.

Jeśli krok 2 zawodzi, problem jest po stronie asystenta i nie ma z Momentum nic
wspólnego. Jeśli zawodzi krok 3, to nasza wina.
