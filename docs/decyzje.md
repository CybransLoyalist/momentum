# Dziennik decyzji

Rzeczy, które kiedyś zostały rozstrzygnięte i nie ma sensu rozstrzygać ich drugi raz.
Nie changelog — tu nie ma listy zmian, tylko **dlaczego** coś wygląda tak, a nie inaczej,
razem z tym, co po drodze odrzuciliśmy.

Specyfikacja ([`spec.html`](spec.html)) mówi, jak jest **teraz**. Ten plik mówi, jak do tego
doszliśmy — i dzięki temu spec może być bezlitośnie nadpisywany, bez gubienia powodów.

Najnowsze na górze. Nowe wpisy dopisujemy zaraz pod tym nagłówkiem.

---

## 2026-08-20 · Kotwica dnia miesiąca przesuwa pierwszy termin

„Co miesiąc, 15." zapisane z domyślną datą wypadało **jutro**, a dopiero kolejny cykl trafiał na
piętnastego — bo kotwica rządziła następnymi terminami, a nie tym pierwszym. Wyglądało to na błąd
i nim było: nikt, kto wpisuje „15.", nie ma na myśli „jutro, a potem piętnastego".

Data i kotwica chodzą teraz razem w obie strony. Wybrana data podstawia się pod kotwicę, dopóki
nie ruszysz jej ręcznie; ręcznie wpisana kotwica przesuwa pierwszy termin na najbliższe swoje
wystąpienie, licząc od dzisiaj. Przy powtarzaniu rocznym miesiąc bierze się z już wybranej daty,
żeby „29 lutego co rok" zostało lutym.

**Ślad:** `TaskEditorScreen.firstOccurrence`.

## 2026-08-20 · Arkusz akcji pokazuje tylko to, co ma sens dla tego zadania

Menu było takie samo dla wszystkiego, więc przy zadaniu cyklicznym z Planu oferowało rzeczy,
które po cichu je rozbrajały, a przy zaplanowanym — przeniesienie do zakupów, czyli listy bez dat.

Zadanie cykliczne traci „Zrób to dzisiaj" (kasuje datę, czyli punkt odniesienia cyklu) i „Odłóż
na kiedyś" (kasuje regułę). Obie zmiany są odwracalne tylko przez ponowne skonfigurowanie cyklu,
więc od zmiany terminu powtarzającego się zadania jest edytor, gdzie widać, co się zmienia.
„Usuń" mówi wprost, że kończy powtarzanie. Zadanie z terminem w przyszłości traci „Przenieś do
zakupów", bo zakupy nie mają dat i znaczyłoby to tylko ciche skasowanie terminu.

Wypadło też „Zaplanuj na inny dzień" z listy głównej. Broniłem go jako skrótu — dwa dotknięcia
zamiast pięciu — ale z samych etykiet nie dało się odgadnąć, czym różni się od „Edytuj…", a menu,
w którym trzeba zgadywać, kosztuje więcej niż trzy zaoszczędzone dotknięcia. Została jedna droga
do terminu. Wyjątek to lista Kiedyś, gdzie „Zaplanuj na konkretny dzień" zostaje, bo edytor
pokazuje tam samo pole nazwy i inaczej nie dałoby się nadać daty.

**Ślad:** `ui/components/TodoActionsSheet.kt`.

## 2026-08-19 · Pełny ekran edytora zamiast konfiguracji w okienku

Wciśnięcie powtarzania pod kalendarz w okienku nie wyszło: rozwijana lista miesięcy nachodziła
na przełącznik „Powtarzaj”. To nie był błąd do poprawienia paroma dpkami — konfiguracja cyklu
po prostu potrzebuje miejsca, którego okno dialogowe nie ma. Powstał osobny, pełny ekran
tworzenia i edycji zadania, a kalendarz wrócił do bycia zwykłym kalendarzem.

Pasek dodawania **zostaje** na ToDo, Kiedyś, Zakupach i Nawykach. Kuszące było ujednolicenie
wszystkiego pod jeden okrągły przycisk, ale wpisywanie zakupów idzie seriami — „mleko”, „chleb”,
„masło” — i przepuszczanie każdej pozycji przez pełny ekran z zapisem byłoby wyraźnym cofnięciem.
Spec mówi o tej liście wprost: zero ceregieli.

Ustępuje tylko Plan, bo tam samym tytułem i tak nic nie dodasz. Jego plusik zamienił się
w strzałkę — skoro na czterech ekranach przycisk dodaje od razu, a tu otwiera ekran, to musi
wyglądać inaczej, inaczej ręka nauczy się jednego i zdziwi na drugim. Jest też aktywny przy
pustym polu: skoro i tak otwiera ekran z dużym polem i klawiaturą, wymaganie tytułu wcześniej
było sztuczne.

Edytor **nie dostaje wyboru listy**, choć się prosił. Przestawienie listy na zakupy po cichu
zabrałoby zadaniu datę i cykliczność, a to za duża cena za jedno pole. Przenoszenie między
listami zostaje w arkuszu akcji, gdzie widać, że to osobna decyzja. Edytor odpowiada za trzy
rzeczy: nazwę, termin, powtarzanie.

„Zaplanuj na inny dzień” zostało w arkuszu obok „Edytuj…”, mimo że edytor też umie zmienić datę.
Przełożenie czegoś na jutro to dwa dotknięcia, a przez edytor byłoby pięć — to nie duplikat,
tylko skrót do najczęstszej czynności.

Przy okazji doszła zmiana nazwy zadania, której nie było w ogóle: raz wpisany tytuł zostawał
na zawsze.

**Ślad:** `ui/editor/TaskEditorScreen.kt`, `TodoRepository.addTask` i `.edit`.

## 2026-08-19 · Termin i powtarzanie w jednym oknie

Powtarzanie było osobnym arkuszem, doklejanym do istniejącego zadania. Dodanie czynszu
zajmowało przez to sześć kroków: dopisz zwykłe zadanie, wybierz datę, otwórz arkusz akcji,
dopiero tam ustaw regułę. Teraz okno wyboru daty ma przełącznik „Powtarzaj”, a pod nim
regułę — trzy kroki i jedno okno.

Sedno jest takie, że powtarzanie **jest własnością terminu**, a nie osobnym bytem. Skoro
mieszka w tym samym rekordzie co data, to i ustawia się je w tym samym miejscu. Arkusz akcji
zachował dwa wejścia („Zaplanuj na inny dzień” i „Powtarzanie: …”), ale oba otwierają to samo
okno — drugie tylko z sekcją powtarzania już rozwiniętą.

Odrzucone: rozpoznawanie reguły ze słów w tytule („czynsz co miesiąc 10”). Najszybsze, kiedy
trafi, ale przy chybieniu ustawia po cichu złą regułę albo zostawia śmieci w tytule, więc i tak
trzeba by dobudować widoczne potwierdzenie — czyli to samo okno, tylko okrężną drogą.

Zapis terminu i reguły idzie jedną metodą repozytorium, nie dwoma składanymi w UI: obie rzeczy
siedzą w tym samym rekordzie i dwa osobne odczyty z zapisem mogłyby się wzajemnie nadpisać.

**Ślad:** `ui/components/ScheduleDialog.kt`, `TodoRepository.plan`.

## 2026-08-19 · Kalendarz nie pozwala wybrać dnia z przeszłości

Dało się zaplanować zadanie na wczoraj, po czym znikało z Planu prosto na listę zaległości —
z przyciemnionym paskiem, jakby wisiało tam od dawna. Dni wcześniejsze niż dziś są teraz
w kalendarzu nieklikalne, w obu miejscach, gdzie się go używa: przy terminie zadania i przy
dacie powrotu z urlopu.

Zaległości dalej powstają — ale wyłącznie przez rollover, czyli przez to, że czegoś naprawdę
nie zrobiłaś. To jedyna uczciwa droga do zaległości i nie ma powodu dawać drugiej, ręcznej.

**Ślad:** `rememberFutureDatePickerState`.

## 2026-08-19 · Odhaczony nawyk schodzi do „Zrobionych”

Odhaczony todos znikał z sekcji i lądował w „Zrobionych”, a odhaczony nawyk zostawał w miejscu
z ptaszkiem. Ta sama czynność dawała dwa różne efekty. Teraz sekcja „Nawyki” pokazuje wyłącznie
to, co jeszcze przed tobą.

W „Zrobionych” nawyk jest nadal rysowany jako nawyk, z paskiem momentum — zwykły wiersz zabrałby
jedyną informację o tym, co odhaczenie właśnie dało.

**Ślad:** `ui/today/TodayScreen.kt`.

## 2026-08-19 · Zadanie cykliczne widać w Planie także wtedy, gdy wypada dziś

Plan pokazywał wyłącznie terminy w przyszłości, więc zadanie cykliczne wypadające dziś
znikało z Planu i wracało tam dopiero po odhaczeniu. Lista, która gubi zaplanowaną rzecz
dokładnie w dniu, w którym ma być zrobiona, kłamie o tym, co jest zaplanowane.

Teraz Plan pokazuje wszystkie zadania cykliczne niezależnie od daty — także dzisiejsze
i zaległe — plus, jak dotąd, wszystko z terminem w przyszłości. Zadanie cykliczne na dziś
jest więc widoczne równocześnie w ToDo i w Planie. To świadome zdublowanie: ToDo odpowiada
na „co robię dziś”, Plan na „co mnie czeka”, a cykliczne zadanie należy do obu odpowiedzi.

Doszły przy tym sekcje „Zaległe” i „Dziś”, bo od kiedy trafiają tu terminy z przeszłości,
„Jutro” musi znaczyć jutro, a nie wszystko do jutra włącznie.

**Ślad:** `MomentumViewModel.scheduledState`, `ScheduledScreen.groupByHorizon`.

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
