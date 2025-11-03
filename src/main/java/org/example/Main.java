package org.example;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== Этап 1: Минимальное CLI-приложение и настройка ===");
        Map<String, String> config = new HashMap<>();

        try {
            config = readConfig();
            System.out.println("Конфигурация успешно загружена.");
        } catch (Exception e) {
            System.out.println("Ошибка чтения config.csv: " + e.getMessage());
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n> Введите команду: ");
            String command = scanner.nextLine().trim().toLowerCase();

            switch (command) {
                case "show":
                    System.out.println("Настроенные параметры:");
                    for (String key : config.keySet()) {
                        System.out.println(" " + key + ": " + config.get(key));
                    }
                    break;

                case "dependencies":
                    System.out.println("\n=== Этап 2: Сбор данных о зависимостях ===");
                    try {
                        String packageName = config.get("package_name");
                        String repoUrl = config.get("repository_url");

                        if (packageName == null || repoUrl == null) {
                            System.out.println("Ошибка: отсутствует package_name или repository_url в config.csv");
                            break;
                        }

                        String[] parts = packageName.split(":");
                        if (parts.length != 2) {
                            System.out.println("Неверный формат package_name. Ожидалось groupId:artifactId");
                            break;
                        }

                        String groupId = parts[0].replace('.', '/');
                        String artifactId = parts[1];

                        String metadataUrl = repoUrl + groupId + "/" + artifactId + "/maven-metadata.xml";
                        System.out.println("Получение информации о последней версии из:\n" + metadataUrl);

                        String metadata = readUrl(metadataUrl);
                        Pattern versionPattern = Pattern.compile("<latest>(.*?)</latest>");
                        Matcher matcher = versionPattern.matcher(metadata);
                        String latestVersion = matcher.find() ? matcher.group(1) : null;

                        if (latestVersion == null) {
                            System.out.println("Не удалось определить последнюю версию пакета.");
                            break;
                        }

                        System.out.println("Последняя версия: " + latestVersion);

                        String pomUrl = repoUrl + groupId + "/" + artifactId + "/" + latestVersion + "/" + artifactId + "-" + latestVersion + ".pom";
                        System.out.println("Загрузка POM-файла:\n" + pomUrl);

                        String pom = readUrl(pomUrl);

                        Pattern depPattern = Pattern.compile(
                                "<dependency>\\s*<groupId>(.*?)</groupId>\\s*<artifactId>(.*?)</artifactId>\\s*(?:<version>(.*?)</version>)?.*?</dependency>",
                                Pattern.DOTALL
                        );
                        Matcher depMatcher = depPattern.matcher(pom);

                        System.out.println("\nПрямые зависимости:");
                        boolean found = false;
                        while (depMatcher.find()) {
                            found = true;
                            System.out.println(" - " + depMatcher.group(1) + ":" + depMatcher.group(2) +
                                    (depMatcher.group(3) != null ? ":" + depMatcher.group(3) : ""));
                        }
                        if (!found) System.out.println(" (Нет прямых зависимостей)");

                    } catch (Exception e) {
                        System.out.println("Ошибка при получении зависимостей: " + e.getMessage());
                    }
                    break;

                case "help":
                    System.out.println("Доступные команды:");
                    System.out.println(" show          - показать параметры конфигурации");
                    System.out.println(" dependencies  - получить и вывести зависимости пакета");
                    System.out.println(" help          - показать список команд");
                    System.out.println(" exit          - выйти из программы");
                    break;

                case "exit":
                    System.out.println("Завершение работы...");
                    return;

                default:
                    System.out.println("Неизвестная команда. Введите help для списка команд.");
            }
        }
    }

    public static Map<String, String> readConfig() {
        Map<String, String> config = new HashMap<>();

        try {
            System.out.println("Попытка чтения config.csv...");
            InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("config.csv");
            if (inputStream == null)
                throw new RuntimeException("Файл config.csv не найден в resources.");

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String headers = reader.readLine();
            String data = reader.readLine();
            reader.close();

            if (headers == null || data == null)
                throw new IllegalArgumentException("CSV-файл пустой или неполный.");

            String[] headerParts = headers.split(",");
            String[] dataParts = data.split(",");

            if (headerParts.length != dataParts.length)
                throw new IllegalArgumentException("Несоответствие количества колонок.");

            for (int i = 0; i < headerParts.length; i++) {
                config.put(headerParts[i].trim(), dataParts[i].trim());
            }
        } catch (Exception e) {
            throw new RuntimeException("Не получилось прочитать config.csv: " + e.getMessage());
        }

        return config;
    }

    private static String readUrl(String urlStr) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(urlStr).openStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                content.append(inputLine).append("\n");
        }
        return content.toString();
    }
}
