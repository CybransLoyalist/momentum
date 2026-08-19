# Dziennik decyzji

Rzeczy, które kiedyś zostały rozstrzygnięte i nie ma sensu rozstrzygać ich drugi raz.
Nie changelog — tu nie ma listy zmian, tylko **dlaczego** coś wygląda tak, a nie inaczej,
razem z tym, co po drodze odrzuciliśmy.

Specyfikacja ([`spec.html`](spec.html)) mówi, jak jest **teraz**. Ten plik mówi, jak do tego
doszliśmy — i dzięki temu spec może być bezlitośnie nadpisywany, bez gubienia powodów.

Najnowsze na górze. Nowe wpisy dopisujemy zaraz pod tym nagłówkiem.

---

## 2026-08-19 · Zadania cykliczne jednak się starzeją

Specyfikacja przeczyła sama sobie. Sekcja 02 mówiła „nawyki i zadania cykliczne nie starzeją
się — one z definicji wracają jutro”, a sekcja 05 przy regule jednej otwartej instancji —
„zaległy czynsz przesuwa się na kolejny dzień jak zwykły todo i normalnie czernieje”.

Rozstrzygnięte na korzyść sekcji 05: **zadania cykliczne starzeją się normalnie, nawyki nie.**
Uzasadnienie z sekcji 02 pasuje wyłącznie do nawyków, bo one faktycznie resetują się co dobę —
niezrobiony wtorek nie czeka, po prostu przepada. Zaległa instancja zadania cyklicznego nie
wraca jutro: to wciąż ta sama, jedyna instancja i gnije. Gdyby nie czerniała, nic nie
sygnalizowałoby spóźnienia, a czynsz zapłacony trzy tygodnie po terminie wyglądałby na ekranie
tak samo jak zapłacony w terminie.

Praktycznie oznacza to brak wyjątku w kodzie — wiek liczy się z `plannedDate` tak samo dla
wszystkich todosów. Sekcja 02 spec poprawiona.

**Ślad:** etap 5, `docs/spec.html` sekcja 02.

## 2026-08-19 · Cykliczność w osobnej tabeli, bez własnego tytułu i bez stanu

Reguła powtarzania siedzi w tabeli `recurrences` i ma dokładnie cztery pola ze spec:
`mode`, `everyN`, `unit`, `anchorDay`. Tytuł zostaje na instancji w `todos` — nowa instancja
kopiuje go z odhaczonej. Trzymanie tytułu także w regule dałoby dwa źródła prawdy przy zmianie
nazwy zadania.

Reguła **nie zapamiętuje daty ostatniego wykonania**. Nie musi: `OD_WYKONANIA` liczy od dnia
odhaczenia, który znamy w momencie generowania, a `KALENDARZOWA` od terminu odhaczonej
instancji. Każde przechowywane pole stanu to kolejna rzecz, która może rozjechać się z historią —
ta sama zasada, która wcześniej zdecydowała o liczeniu momentum w locie.

**Ślad:** etap 5, `data/Recurrence.kt`, `domain/Recurring.kt`.

## 2026-08-19 · Zaległy cykl dogania teraźniejszość, nie odtwarza przeszłości

Odhaczenie zadania zaległego o trzy miesiące mogłoby wygenerować kolejny termin też w
przeszłości, a po następnym odhaczeniu znowu — i zamiast jednej instancji zostałaby kolejka
zaległości do przeklikania. Generator przesuwa więc termin tyle razy, ile trzeba, żeby wypadł
po dzisiaj.

Kosztem jest to, że pominięte cykle znikają bez śladu. Świadomie: to apka do robienia rzeczy,
a nie do rozliczania się z tego, ile razy nie zapłaciło się czynszu na czas.

**Ślad:** etap 5, `Recurring.next`.

## 2026-08-19 · Cofnięcie odhaczenia kasuje wygenerowaną instancję

Odhaczenie zadania cyklicznego tworzy następną instancję od razu. Cofnięcie w oknie doby musi
więc tę instancję skasować, inaczej po jednym przypadkowym kliknięciu i jego cofnięciu na
liście zostają dwie kopie tego samego czynszu — a reguła mówi wyraźnie: jedna otwarta instancja.

**Ślad:** etap 5, `TodoRepository.setDone`.

## 2026-08-19 · Tryb urlopowy jest globalny, nie per-nawyk

Pauza była wcześniej polem nawyku (`pausedFrom`, `pausedTo`). Teraz jest jednym stanem
aplikacji w `SettingsStore` i wstrzymuje wszystkie nawyki naraz.

Urlop to stan całej osoby, a nie pojedynczego nawyku — nie jedzie się na wakacje od siłowni,
ale nie od Duolingo. Dwa mechanizmy robiące to samo (pauza per-nawyk i globalny urlop) prędzej
czy później by się rozjechały, więc stary został usunięty migracją, a nie zostawiony „na wszelki
wypadek”.

Data powrotu jest opcjonalna i domyślnie urlop trwa do odwołania. Wybieranie dnia powrotu z góry
to zgadywanka, a spóźniony powrót oznaczałby karę za dni wolnego — czyli dokładnie to, przed czym
urlop ma chronić. Przed zapomnieniem o włączonym urlopie chroni pasek na liście ToDo.

**Ślad:** commit `ea019d7`, migracja `DropPerHabitPause`.

## 2026-08-19 · Momentum liczy się z historii przy każdym odczycie

Wartość momentum nie jest przechowywana w bazie — przelicza się z tabeli odhaczeń przy każdym
odczycie ekranu.

Przechowywana liczba mogłaby rozjechać się z historią przy odhaczaniu wstecz, przy zmianie
harmonogramu albo po urlopie, i wtedy nie wiadomo, która wersja ma rację. Kilkaset iteracji po
dniach kosztuje tyle, że nie da się tego zauważyć na telefonie.

Konsekwencja, którą warto pamiętać: odhaczenie wstecz przelicza historię wstecz i to jest
zamierzone.

**Ślad:** commit `33269f9`, `domain/Momentum.kt`.

## 2026-08-19 · Nadprogramowa robota daje bonus, ale brak roboty w dzień wolny nie karze

Nawyk „wtorek i czwartek” odhaczony w środę dostaje +1. Nieodhaczony w środę nie traci nic.
Asymetria jest świadoma: inaczej harmonogram dwa razy w tygodniu nigdy nie dobiłby do dziesiątki,
bo tracił by punkty w poniedziałki.

Druga reguła z tej samej rodziny: dzisiejszy brak odhaczenia nie jest jeszcze karą, bo dzień
trwa. Bez tego każdy poranek zaczynałby się od minusa.

Obie mają testy jednostkowe, bo tej logiki nie da się sprawdzić klikaniem — efekty widać po
tygodniach.

**Ślad:** commit `33269f9`, `MomentumTest.kt`.

## 2026-08-19 · Arkusz akcji zamiast gestów przesunięcia

Wszystkie akcje na zadaniu żyją w jednym arkuszu wywoływanym kliknięciem w tytuł. Gesty są
szybsze, ale niewidoczne, a większość tych akcji wykonuje się rzadko — ważniejsze, żeby dało się
je znaleźć, niż żeby dało się je zrobić w jeden ruch.

Arkusz i wybór daty mieszkają w powłoce z zakładkami, nie w każdym ekranie osobno. Działają tak
samo z każdej listy i jest jedna kopia tej logiki zamiast czterech.

**Ślad:** commit `2a18178`, `ui/components/TodoActionsSheet.kt`.

## 2026-08-19 · `firstTodayDate` skasowane jako duplikat `plannedDate`

Wiek zadania liczy się z `plannedDate`, a nie z osobnego pola. Okazało się zbędne: rollover
zostawia datę nietkniętą, a każde świadome działanie — zdjęcie z dzisiaj, przełożenie terminu —
ma wiek zresetować, co dzieje się samo przez nadpisanie tego pola.

Dwa pola trzymane w zgodzie ręcznie to gotowa przyszła awaria.

**Ślad:** commit `2a18178`, migracja `DropFirstTodayDate`.

## 2026-08-19 · Rollover bez zadania w tle

Niedokończone zadanie „na dzisiaj” zostaje z wczorajszą datą, a warunek `plannedDate <= dziś`
nadal je łapie. Nie ma żadnego budzika o północy, nie ma nic do przespania przy uśpionej
aplikacji, nie ma nic, co Samsung może zablokować.

Odświeżenie daty dnia dzieje się przy powrocie do aplikacji, bo przepływy nie budzą się same.

**Ślad:** commit `ff8ac2e`, `data/Todo.kt`.

## 2026-08-19 · Jedna tabela na pięć list

„Na dzisiaj”, „ogólne”, „zaplanowane”, „kiedyś” i zakupy to nie pięć list, tylko jedna tabela
z dwoma polami: `bucket` i `plannedDate`. Przenoszenie między listami jest wtedy zmianą jednego
pola, a nie przepisywaniem rekordu między tabelami.

**Ślad:** commit `ff8ac2e`, `data/Todo.kt`.

## 2026-08-19 · Odhaczone todosy giną po dobie, odhaczone nawyki zostają na zawsze

Na ekranie wyglądają tak samo, ale to dwa różne mechanizmy. Todos po odhaczeniu zostaje przez
24 godziny w zwijanej sekcji na dole — wyłącznie po to, żeby dało się cofnąć przypadkowe
kliknięcie — i potem jest kasowany przy starcie aplikacji. Nawyki bez pełnej historii nie mają
z czego liczyć momentum, więc ich odhaczenia zostają bezterminowo.

**Ślad:** commit `ff8ac2e`, `TodoDao.purgeCompletedBefore`.

## 2026-08-19 · Najpierw dystrybucja, potem funkcje

Etap pierwszy to pusta aplikacja Compose pokazująca numer wersji. Cel: potwierdzić, że działa
cały łańcuch build → podpis → GitHub Release → Obtainium → telefon, **zanim** będzie co
aktualizować. Zepsuta dystrybucja odkryta po trzech tygodniach pisania funkcji kosztowałaby
znacznie więcej niż jeden dzień na początku.

Przy okazji: klucz podpisujący leży poza repozytorium, a workflow odtwarza go z sekretów.
Klucza nie da się odtworzyć — jego utrata oznacza reinstalację aplikacji na telefonie i utratę
danych.

**Ślad:** commity `43875d1`, `da6bd8a`, `.github/workflows/release.yml`.

## 2026-08-19 · Most głosowy idzie przez Google Tasks, nie przez Samsung Notes

Test na telefonie pokazał, jak Samsung rozdziela komendy: „dodaj zadanie XYZ” trafia do Google
Tasks, które ma publiczne i darmowe API, a „dodaj do listy zakupów” do Samsung Notes, które nie
ma żadnego. Dlatego wszystko wchodzi jedną rurą — przez Tasks — a rozdziela się po naszej
stronie po słowach kluczowych.

Dzięki temu routing nie zależy od tego, co Samsung wymyśli w kolejnej aktualizacji, i nie trzeba
zmieniać nawyku mówienia.

Etap siódmy, jeszcze niezrobiony. Widżet z mikrofonem zostaje w zanadrzu jako plan awaryjny.

**Ślad:** `docs/spec.html` sekcja 08.
