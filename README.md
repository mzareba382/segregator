# Segregator

### Instrukcja uruchomienia

By zbudować aplikację, należy wywołać w katalogu głównym:

`./mvnw clean package`

lub w środowisku Windows:

`mvnw.cmd clean package`

Po zbudowaniu, należy uruchomić poprzez podanie ścieżki do utworzonego pliku .jar:

`java -jar target/segregator-0.0.1-SNAPSHOT.jar`

Aplikacja umożliwia konfigurowanie ścieżek segregatora 
poprzez zmianę wartości w pliku src/main/resources/application.properties

Wymagania:
* Maven 3.6.3
* JDK 11


