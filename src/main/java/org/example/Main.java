package org.example;

import java.io.BufferedReader;//класс для чтения файлов/потоков
import java.io.FileInputStream; //класс для чтения файлов/потоков
import java.io.InputStream;//класс для чтения файлов/потоков
import java.io.InputStreamReader; //класс для чтения файлов/потоков
import java.net.URL;//чтобы открыть URL и скачать содержимое
import java.util.HashMap;//ключ → значение
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher; //регулярные выражения
import java.util.regex.Pattern; //регулярные выражения

public class Main {

    public static void main(String[] args) {
        System.out.println("=== Этап 1: Загрузка конфигурационного файла ===");
        Scanner scanner = new Scanner(System.in); //Создаём Scanner, чтобы читать строки

        System.out.print("Введите путь к CSV-файлу конфигурации (src/main/resources/config.csv): ");
        String filePath = scanner.nextLine().trim(); //читаем ввод (строка) и trim() убирает пробелы по краям

        Map<String, String> config;
        try {
            config = readConfig(filePath);
            System.out.println("Конфигурация успешно загружена.\n");
            System.out.println("Параметры:");
            for (String key : config.keySet()) {
                System.out.println(" " + key + ": " + config.get(key));
            }
        } catch (Exception e) {
            System.out.println("Ошибка при чтении конфигурации: " + e.getMessage());
            return;
        }

        System.out.println("\n=== Этап 2: Сбор данных о зависимостях ===");
        System.out.print("Введите команду для получения зависимостей (dependencies): ");
        String command = scanner.nextLine().trim().toLowerCase();
        //toLowerCase() — переводит всю строку в нижний регистр, чтобы не зависеть от регистра
        // (Dependencies, DEPENDENCIES, dependencies — всё засчитается одинаково)

        if (!command.equals("dependencies")) {
            //если введённая команда не равна слову "dependencies"
            System.out.println("Неизвестная команда. Программа завершена.");
            return;
        }
        try {
            String packageName = config.get("package_name");
            String repoUrl = config.get("repository_url");

            //проверка конфигурации, достаем из конфигурации имя пакета и URL репозитория
            if (packageName == null || repoUrl == null) {
                System.out.println("Ошибка: отсутствует package_name или repository_url в конфигурации.");
                return;
            }
            //разбор имени пакета
            String[] parts = packageName.split(":");
            //if packageName = "org.springframework:spring-core"= parts[0] = "org.springframework" -> groupId = "org/springframework"
            //parts[1] = "spring-core" -> artifactId = "spring-core"
            if (parts.length != 2) {
                //Проверка if (parts.length != 2) нужна, чтобы убедиться, что строка действительно содержит ровно один :
                System.out.println("Неверный формат package_name. Ожидалось groupId:artifactId");
                return;
            }
            String groupId = parts[0].replace('.', '/');
            //В Maven пути в репозитории строятся по иерархии папок, где точки в groupId заменяются на слэши
            String artifactId = parts[1];


            //получение инфы о последней версии
            String metadataUrl = repoUrl + groupId + "/" + artifactId + "/maven-metadata.xml";
            //Формирует ссылку, по которой хранится XML-файл с информацией о версиях пакета.
            System.out.println("Получение информации о последней версии из:\n" + metadataUrl);

            String metadata = readUrl(metadataUrl); //скачивает содержимое файла с Maven-репозитория т.е. XML как текст
            Pattern versionPattern = Pattern.compile("<latest>(.*?)</latest>"); //создаёт регулярное выражение, чтобы найти в XML строку между тегами <latest> и </latest>
            Matcher matcher = versionPattern.matcher(metadata);
            String latestVersion = matcher.find() ? matcher.group(1) : null;
            //Формируем URL: репозиторий/org/springframework/spring-core/maven-metadata.xml
            //Скачиваем XML-файл
            //Ищем тег <latest>5.3.20</latest> и извлекаем версию "5.3.20"
            //matcher.find() ищет совпадение
            //matcher.group(1) возвращает найденную подстроку (первая группа в скобках (.*?))

            if (latestVersion == null) {
                System.out.println("Не удалось определить последнюю версию пакета.");
                return;
            }

            System.out.println("Последняя версия: " + latestVersion);

            //загрузка пом-файлика и извлечение зависимостей
            String pomUrl = repoUrl + groupId + "/" + artifactId + "/" + latestVersion + "/" + artifactId + "-" + latestVersion + ".pom";
            System.out.println("Загрузка POM-файла:\n" + pomUrl);
            String pom = readUrl(pomUrl);

            //поиск зависимостей в пом-файлике
            Pattern depPattern = Pattern.compile(
                    // ищем в XML блоки зависимостей и выводим оттуда groupId, artifactId и version
                    "<dependency>\\s*<groupId>(.*?)</groupId>\\s*<artifactId>(.*?)</artifactId>\\s*(?:<version>(.*?)</version>)?.*?</dependency>",
                    Pattern.DOTALL
                    //Точка совпадает с абсолютно любым символом, включая переносы строк
                    //Регулярка работает с текстом как с единым целым
                    //Без DOTALL наша регулярка не сможет найти зависимости, потому что между тегами есть переносы строк.
            );
            Matcher depMatcher = depPattern.matcher(pom); //объект Matcher, который будет искать совпадения с нашей регуляркой в тексте POM-файла

            System.out.println("\nПрямые зависимости:");
            boolean found = false;
            while (depMatcher.find()) { //ищем след совпадения
                found = true; //если нашли dependency
                System.out.println(" - " + depMatcher.group(1) + ":" + depMatcher.group(2) +
                        (depMatcher.group(3) != null ? ":" + depMatcher.group(3) : ""));//если версия есть,тодобавляем :версия;ели версии нет,то добавляем пустую строку

            }
            if (!found) System.out.println(" (Нет прямых зависимостей)");

        } catch (Exception e) {
            System.out.println("Ошибка при получении зависимостей: " + e.getMessage());
        }
    }


    //readConfig() - читает CSV файл и превращает его в Map (ключ-значение)
    //readUrl() - скачивает содержимое по URL и возвращает как строку
    public static Map<String, String> readConfig(String filePath) throws Exception {
        Map<String, String> config = new HashMap<>();
        //создаем пустой словарь для хранения конфигурации в формате ключ-значение.

        InputStream inputStream = new FileInputStream(filePath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String headers = reader.readLine();
        String data = reader.readLine();
        reader.close();
        //headers - первая строка с названиями колонок
        //data - вторая строка со значениями

        if (headers == null || data == null)
            throw new IllegalArgumentException("CSV-файл пустой или неполный.");

        String[] headerParts = headers.split(",");
        String[] dataParts = data.split(",");
        //разбиваем строки по запятым на массивы

//        if (headerParts.length != dataParts.length)
//            throw new IllegalArgumentException("Несоответствие количества колонок.");
        for (int i = 0; i < headerParts.length; i++) {
            config.put(headerParts[i].trim(), dataParts[i].trim());//заполняем хэш
        }
        return config;
    }

    private static String readUrl(String urlStr) throws Exception {
        StringBuilder content = new StringBuilder(); //StringBuilder для накопления скачанного содержимого
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(urlStr).openStream()))) {
            //открываем соединение с URL и создаем читалку
            // try-with-resources автоматически закроет соединение
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                content.append(inputLine).append("\n");
            //читаем файл построчно и добавляем каждую строку в content+ про переносы строк
        }
        return content.toString(); //возвращаем содержимое как одну строку
    }
}
