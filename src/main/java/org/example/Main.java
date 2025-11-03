package org.example;
/*
CSV (Comma-Separated Values) — это простой текстовый формат,
где данные разделены запятыми.
 */
//CLI-приложение — это программа,
// с которой пользователь взаимодействует с помощью
// текстовых команд в терминале или командной строке,
// а не с помощью графического интерфейса (GUI).

import java.io.BufferedReader;//чтение текста из потока ввода
import java.util.Scanner;
import java.util.HashMap;
import java.io.FileReader;
import java.io.InputStream; //абстрактный класс для чтения байтов
import java.io.OutputStream; //класс для записи байтов
import java.io.InputStreamReader; //читает байты из входного потока и декодирует их в символы
import java.util.Map; //это интерфейс, говорит, как работать со словарем, но не реализует
import java.util.HashMap;//то структура данных, использующая интерфейс Map и хэш-таблицу для хранения пар «ключ-значение».
//беспечивает эффективный доступ к данным и управление ими на основе уникальных ключей




public class Main {

    public static void main(String[] args) {
        System.out.println("ПЕРВЫЙ ШАГ: Минимальное CLI-приложение. Введите команду (help для справки).");
        Map<String, String> config = new HashMap<>();
        try {
            //чтение конфигурации метод readConfig
            config = readConfig();
        } catch (Exception e) {
            System.out.println("Ошибка чтения config.csv: " + e.getMessage());
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("> ");
            String command = scanner.nextLine().trim().toLowerCase();

            switch (command) {
                case "show":
                    System.out.println("Настроенные параметры:");
                    for (String key : config.keySet()) {
                        System.out.println(" " + key + ": " + config.get(key));
                    }
                    break;
                case "help":
                    System.out.println("Доступные команды:");
                    System.out.println(" show - показать параметры конфигурации");
                    System.out.println(" exit - выйти из программы");
                    break;

                case "exit":
                    System.out.println("Завершение работы");
                    return;

                default:
                    System.out.println("Неизвестная команда. Введите help для списка команд");
            }
        }
    }

    public static Map<String, String> readConfig() {
        Map<String, String> config = new HashMap<>();

        try {
            System.out.println("Попытка чтения config.csv");

            InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("config.csv");
            if (inputStream == null) {
                throw new RuntimeException("Файл config.csv не найдет в resources/");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String headers = reader.readLine();
            String data = reader.readLine();
            reader.close();

            //Проверка на пустой файл
            if (headers == null) {
                throw new IllegalArgumentException("CSV-файл пустой - нет заголовка");
            }

            if (data == null) {
                throw new IllegalArgumentException("CSV-файл не содержит данных");
            }

            //разделение на колонки
            String[] headerParts = headers.split(",");
            String[] dataParts = data.split(",");

            //проверка на соотв колонок
            if (headerParts.length != dataParts.length) {
                throw new IllegalArgumentException("Несоответствие количества колонок: "
                        + headerParts.length + " vs " + dataParts.length);
            }

            //заполнение конфигурации
            for (int i = 0; i < headerParts.length; i++) {
                String key = headerParts[i].trim();
                String value = dataParts[i].trim();

                if (key.isEmpty()) {
                    throw new IllegalArgumentException("Пустой заголовок в колонке " + (i + 1));
                }
                config.put(key, value);
            }
        } catch (Exception e) {
            throw new RuntimeException("Не получилось прочитать config.csv: " + e.getMessage());
        }
        return config;
    }
}