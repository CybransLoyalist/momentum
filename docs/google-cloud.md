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

**Interfejsy API i usługi → Ekran akceptacji OAuth**. W nowszym układzie konsoli nazywa
się to **Google Auth Platform** i zamiast formularza dostaniesz ekran „Google Auth Platform
not configured yet" z przyciskiem **Get started**. Kliknij go — to kreator na cztery kroki:

**App Information**
- App name: `Momentum`
- User support email: twój adres

**Audience** — tu jest wybór typu użytkownika
- **External**
- *Internal* będzie wyszarzone i tak ma być: ta opcja istnieje wyłącznie dla kont
  w organizacji Google Workspace, a nie dla zwykłego Gmaila

**Contact Information**
- Twój adres e-mail

**Finish**
- Zaznacz zgodę na *Google API Services: User Data Policy* → **Create**

Po zamknięciu kreatora pozycje w lewym menu (Branding, Audience, Clients, Data Access)
przestaną być puste. Dopiero teraz da się zrobić kolejne kroki.

### Dokończ stronę Branding

Kreator zwykle nie wypełnia wszystkiego i na stronie **Audience** wisi wtedy żółty pasek
„Your app's OAuth configuration is incomplete", a przycisk **Publish app** jest wyszarzony.
Wejdź w **Branding** i uzupełnij:

- **App name**: `Momentum`
- **User support email**: twój adres
- **Developer contact information** (na dole): ten sam adres

**Zostaw puste:** App logo, Application home page, Application privacy policy link,
Application terms of service link, Authorized domains.

> ⚠️ To nie jest kwestia wygody. Wpisanie **jakiegokolwiek adresu URL** w sekcji App domain
> sprawia, że Google zażąda dodania i **zweryfikowania domeny** — potwierdzenia, że jesteś
> jej właścicielką. Bez własnej strony internetowej nie ma z tego wyjścia. Aplikacja
> z pustymi polami publikuje się bez przeszkód.

Zapisz i wróć na **Audience** — dopiero teraz publikacja będzie możliwa.

## 4. Zakres uprawnień

**Data Access** (po polsku **Dostęp do danych**) → **Add or remove scopes** →
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

**Audience** (**Odbiorcy**) → zobaczysz „Publishing status: Testing" i przycisk
**Publish app**. Kliknij go i potwierdź przejście na **In production**.

Dlaczego to takie ważne: dokumentacja Google wiąże siedmiodniowe wygasanie tokenu ze
stanem **Testowanie**. Zostawiona w testach aplikacja **przestanie działać po tygodniu**
i będziesz musiała logować się co siedem dni do końca świata. Przejście na „W produkcji"
usuwa ten warunek i **nie wymaga przechodzenia weryfikacji**.

Jedyną ceną jest ten jednorazowy ekran ostrzeżenia z punktu 4.

## 6. Identyfikator klienta

**Clients** (**Klienci**) w menu Google Auth Platform → **Create client**.
W starszym układzie: **Interfejsy API i usługi → Dane logowania → Utwórz dane logowania →
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
