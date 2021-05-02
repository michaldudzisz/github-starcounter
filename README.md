# Aplikacja serwerowa

## Uruchomienie
Aplikacja została napisana w języku Java (wersja 11) jako aplikacja Spring Boot. Do zarządzania projektem użyty został Maven. Należy mieć zainstalowane JDK 11 oraz Maven. Po pobraniu i rozpakowaniu katalogu projektu należy przejść do niego w konsoli i wpisać:
1. `$ mvn clean install` - co pobierze brakujące zależności oraz zbuduje projekt.
2. `$ java -jar target/starcounter-0.0.1-SNAPSHOT.jar \ mdudzisz.starcounter.StarcounterApplication` - co uruchomi aplikację.

Domyślnie aplikacja uruchamia się w pod adresem `http://localhost:8080/`.  Port można zmienić w pliku `application.properties` w lokalizacji `/src/main/resources/`.

## API
Aplikacja wspiera częściowo paginację dostępu do danych po stronie serwisu Github. Udostępnia swoje API opisane poniżej.

### Listowanie repozytoriów wraz z ich liczbą gwiazdek
#### `<adres bazowy aplikacji>/list/{nazwa użytkownika}?{query}` 

|    Parametr    |      Dozwolone wartości                              |  Domyślna wartość
|----------------|------------------------------------------------------|---------------------
|   per_page     | Liczby całkowite z zakresu <1, 100>                  |     30
|     page       | Ten parametr nie powinien być podawany samodzielnie  |      -

Po wysłaniu zapytania GET pod powyższy adres zostanie zwrócona wiadomość JSON z listą obiektów typu:

```  
{
"name": "<nazwa repozytorium>",
"stargazers_count": <liczba gwiazdek>
}
```

W polu "links" nagłówka HTTP znajdą się natomiast linki odsyłające do kolejnych stron danych, na przykład:

```
<http://localhost:8080/list/<nazwa użytkownika>?per_page=10&page=2>;rel="next"
```

Mogą się tam znaleźć adresy z następujących relacji: 
* `"next"` - odsyłający do następnej strony
* `"prev"` - odsyłający do poprzedniej strony
* `"last"` - odsyłający do ostatniej strony
* `"first"` - odsyłający do pierwszej strony

Wszystkie z nich są opcjonalne - jeśli zwracana strona jest jedyną, to nie pojawi się żaden adres. W celu wysyłania zapytań o kolejne strony należy korzystać z tych adresów, dlatego samodzielne podawanie numeru strony w adresie URL jest zbędne.

### Zliczanie sumy gwiazdek wszystkich repozytoriów użytkownika
#### `<adres bazowy aplikacji>/count/{nazwa użytkownika}`

Zwracana jest wiadomość JSON typu:

```  
{
  "username": "<nazwa użytkownika>",
  "star_count": <suma gwiazdek użytkownika>
}
```

### Uwagi do obecnej wersji aplikacji i propozycje jej poprawy

* API serwisu Github umożliwia na wysłanie jedynie 60 zapytań w ciągu godziny nieautoryzowanej aplikacji/niezalogowanemu użytkownikowi. Można zwiększyć tę liczbę logując się do serwisu lub rejestrując aplikację.
* Niektóre komunikaty błędów mogłyby być wypisywane użytkownikowi lepiej - np. obecnie przy odmowie dostępu do danych aplikacji przez serwis Github wyświetlany jest kod błędu i cała wiadomość zwrócona przez API Github - co może być przydatne, np. gdy użytkownik wyśle zbyt wiele zapytań w krótkim odstępie czasu, lecz  dla czytelności można by było wyłuskać z odpowiedzi samo pole "message" (kod błędu jest kopiowany osobno i tak).
* Brak logowania sytuacji wyjątkowych do pliku.
