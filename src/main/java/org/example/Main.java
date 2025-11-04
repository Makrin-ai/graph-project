package org.example;

import java.io.BufferedReader;
import java.io.FileInputStream;
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
        System.out.println("=== Этап 1: Загрузка конфигурационного файла ===");
        Scanner scanner = new Scanner(System.in);

        System.out.print("Введите путь к CSV-файлу конфигурации (src/main/resources/config.csv): ");
        String filePath = scanner.nextLine().trim();

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

        if (!command.equals("dependencies")) {
            System.out.println("Неизвестная команда. Программа завершена.");
            return;
        }

        try {
            String packageName = config.get("package_name");
            String repoUrl = config.get("repository_url");

            if (packageName == null || repoUrl == null) {
                System.out.println("Ошибка: отсутствует package_name или repository_url в конфигурации.");
                return;
            }

            String[] parts = packageName.split(":");
            if (parts.length != 2) {
                System.out.println("Неверный формат package_name. Ожидалось groupId:artifactId");
                return;
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
                return;
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
    }

    public static Map<String, String> readConfig(String filePath) throws Exception {
        Map<String, String> config = new HashMap<>();

        InputStream inputStream = new FileInputStream(filePath);
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
