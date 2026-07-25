// BookmarkManagerJava.java — менеджер закладок с тегами на Java

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.google.gson.*;

public class BookmarkManagerJava {
    private static class Bookmark {
        int id;
        String title;
        String url;
        List<String> tags;
        String notes;
        String created;

        Bookmark(int id, String title, String url, List<String> tags, String notes) {
            this.id = id;
            this.title = title;
            this.url = url;
            this.tags = tags != null ? tags : new ArrayList<>();
            this.notes = notes != null ? notes : "";
            this.created = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    private List<Bookmark> bookmarks = new ArrayList<>();
    private int nextId = 1;
    private String dataFile = "bookmarks.json";
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public BookmarkManagerJava() {
        load();
    }

    private void load() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(dataFile)));
            JsonArray arr = JsonParser.parseString(content).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                int id = obj.get("id").getAsInt();
                String title = obj.get("title").getAsString();
                String url = obj.get("url").getAsString();
                List<String> tags = new ArrayList<>();
                JsonArray tagsArr = obj.get("tags").getAsJsonArray();
                for (JsonElement t : tagsArr) tags.add(t.getAsString());
                String notes = obj.get("notes").getAsString();
                String created = obj.get("created").getAsString();
                Bookmark b = new Bookmark(id, title, url, tags, notes);
                b.created = created;
                bookmarks.add(b);
                if (id >= nextId) nextId = id + 1;
            }
        } catch (IOException e) {}
    }

    private void save() {
        JsonArray arr = new JsonArray();
        for (Bookmark b : bookmarks) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", b.id);
            obj.addProperty("title", b.title);
            obj.addProperty("url", b.url);
            JsonArray tagsArr = new JsonArray();
            for (String t : b.tags) tagsArr.add(t);
            obj.add("tags", tagsArr);
            obj.addProperty("notes", b.notes);
            obj.addProperty("created", b.created);
            arr.add(obj);
        }
        try (PrintWriter pw = new PrintWriter(dataFile)) {
            pw.println(gson.toJson(arr));
        } catch (IOException e) {}
    }

    public int add(String title, String url, List<String> tags, String notes) {
        Bookmark b = new Bookmark(nextId++, title, url, tags, notes);
        bookmarks.add(b);
        save();
        return b.id;
    }

    public void delete(int id) {
        bookmarks.removeIf(b -> b.id == id);
        save();
    }

    public boolean edit(int id, String title, String url, List<String> tags, String notes) {
        for (Bookmark b : bookmarks) {
            if (b.id == id) {
                if (title != null) b.title = title;
                if (url != null) b.url = url;
                if (tags != null) b.tags = tags;
                if (notes != null) b.notes = notes;
                save();
                return true;
            }
        }
        return false;
    }

    public List<Bookmark> search(String query) {
        List<Bookmark> results = new ArrayList<>();
        String q = query.toLowerCase();
        for (Bookmark b : bookmarks) {
            if (b.title.toLowerCase().contains(q) || b.url.toLowerCase().contains(q)) {
                results.add(b);
            } else {
                for (String tag : b.tags) {
                    if (tag.toLowerCase().contains(q)) {
                        results.add(b);
                        break;
                    }
                }
            }
        }
        return results;
    }

    public Set<String> getAllTags() {
        Set<String> tags = new HashSet<>();
        for (Bookmark b : bookmarks) tags.addAll(b.tags);
        return tags;
    }

    public List<Bookmark> getByTag(String tag) {
        List<Bookmark> results = new ArrayList<>();
        for (Bookmark b : bookmarks) {
            if (b.tags.contains(tag)) results.add(b);
        }
        return results;
    }

    public void export(String filename) {
        JsonArray arr = new JsonArray();
        for (Bookmark b : bookmarks) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", b.id);
            obj.addProperty("title", b.title);
            obj.addProperty("url", b.url);
            JsonArray tagsArr = new JsonArray();
            for (String t : b.tags) tagsArr.add(t);
            obj.add("tags", tagsArr);
            obj.addProperty("notes", b.notes);
            obj.addProperty("created", b.created);
            arr.add(obj);
        }
        try (PrintWriter pw = new PrintWriter(filename)) {
            pw.println(gson.toJson(arr));
            System.out.println("Экспортировано в " + filename);
        } catch (IOException e) {}
    }

    public void import_(String filename) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            JsonArray arr = JsonParser.parseString(content).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                int id = obj.get("id").getAsInt();
                String title = obj.get("title").getAsString();
                String url = obj.get("url").getAsString();
                List<String> tags = new ArrayList<>();
                JsonArray tagsArr = obj.get("tags").getAsJsonArray();
                for (JsonElement t : tagsArr) tags.add(t.getAsString());
                String notes = obj.get("notes").getAsString();
                String created = obj.get("created").getAsString();
                Bookmark b = new Bookmark(id, title, url, tags, notes);
                b.created = created;
                bookmarks.add(b);
                if (id >= nextId) nextId = id + 1;
            }
            save();
            System.out.println("Импортировано из " + filename);
        } catch (Exception e) {
            System.out.println("Ошибка импорта: " + e.getMessage());
        }
    }

    public void listAll() {
        if (bookmarks.isEmpty()) {
            System.out.println("Закладок нет.");
            return;
        }
        for (Bookmark b : bookmarks) {
            System.out.print("ID " + b.id + ": " + b.title + " (" + b.url + ") Теги: ");
            if (b.tags.isEmpty()) System.out.print("(без тегов)");
            else {
                for (int i=0; i<b.tags.size(); ++i) {
                    if (i>0) System.out.print(", ");
                    System.out.print(b.tags.get(i));
                }
            }
            System.out.println();
        }
    }

    private void printColor(String text, String color) {
        String code = "";
        switch (color) {
            case "green": code = "\u001B[32m"; break;
            case "red": code = "\u001B[31m"; break;
            case "yellow": code = "\u001B[33m"; break;
            case "blue": code = "\u001B[34m"; break;
            case "cyan": code = "\u001B[36m"; break;
        }
        System.out.println(code + text + "\u001B[0m");
    }

    public void run() {
        Scanner sc = new Scanner(System.in);
        printColor("🏷️ TagManager Pro — Java Edition", "cyan");
        System.out.println("Команды: add, list, search, delete, edit, tags, export, import, exit");
        while (true) {
            System.out.print("> ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(" ");
            String cmd = parts[0].toLowerCase();
            if (cmd.equals("exit")) break;
            else if (cmd.equals("add")) {
                System.out.print("Название: ");
                String title = sc.nextLine().trim();
                System.out.print("URL: ");
                String url = sc.nextLine().trim();
                System.out.print("Теги (через запятую): ");
                String tagsLine = sc.nextLine().trim();
                System.out.print("Заметки (опционально): ");
                String notes = sc.nextLine().trim();
                if (title.isEmpty() || url.isEmpty()) {
                    printColor("Название и URL обязательны.", "red");
                    continue;
                }
                List<String> tags = new ArrayList<>();
                if (!tagsLine.isEmpty()) {
                    for (String t : tagsLine.split(",")) {
                        t = t.trim();
                        if (!t.isEmpty()) tags.add(t);
                    }
                }
                int id = add(title, url, tags, notes);
                printColor("✅ Закладка добавлена (ID: " + id + ")", "green");
            } else if (cmd.equals("list")) {
                listAll();
            } else if (cmd.equals("search")) {
                System.out.print("Поиск: ");
                String query = sc.nextLine().trim();
                List<Bookmark> results = search(query);
                if (results.isEmpty()) {
                    printColor("Ничего не найдено.", "yellow");
                } else {
                    for (Bookmark b : results) {
                        System.out.print("ID " + b.id + ": " + b.title + " (" + b.url + ") Теги: ");
                        if (b.tags.isEmpty()) System.out.print("(без тегов)");
                        else {
                            for (int i=0; i<b.tags.size(); ++i) {
                                if (i>0) System.out.print(", ");
                                System.out.print(b.tags.get(i));
                            }
                        }
                        System.out.println();
                    }
                }
            } else if (cmd.equals("delete")) {
                System.out.print("ID закладки: ");
                try {
                    int id = Integer.parseInt(sc.nextLine().trim());
                    delete(id);
                    printColor("Закладка #" + id + " удалена.", "green");
                } catch (NumberFormatException e) {
                    printColor("Введите число.", "red");
                }
            } else if (cmd.equals("edit")) {
                System.out.print("ID закладки: ");
                try {
                    int id = Integer.parseInt(sc.nextLine().trim());
                    System.out.print("Новое название (Enter для пропуска): ");
                    String title = sc.nextLine().trim();
                    if (title.isEmpty()) title = null;
                    System.out.print("Новый URL (Enter для пропуска): ");
                    String url = sc.nextLine().trim();
                    if (url.isEmpty()) url = null;
                    System.out.print("Новые теги (через запятую, Enter для пропуска): ");
                    String tagsLine = sc.nextLine().trim();
                    List<String> tags = null;
                    if (!tagsLine.isEmpty()) {
                        tags = new ArrayList<>();
                        for (String t : tagsLine.split(",")) {
                            t = t.trim();
                            if (!t.isEmpty()) tags.add(t);
                        }
                    }
                    System.out.print("Новые заметки (Enter для пропуска): ");
                    String notes = sc.nextLine().trim();
                    if (notes.isEmpty()) notes = null;
                    if (edit(id, title, url, tags, notes)) {
                        printColor("Закладка обновлена.", "green");
                    } else {
                        printColor("Закладка не найдена.", "red");
                    }
                } catch (NumberFormatException e) {
                    printColor("Введите число.", "red");
                }
            } else if (cmd.equals("tags")) {
                Set<String> tags = getAllTags();
                if (tags.isEmpty()) {
                    printColor("Тегов нет.", "yellow");
                } else {
                    printColor("Все теги: ", "blue");
                    System.out.println(String.join(", ", tags));
                }
            } else if (cmd.equals("export")) {
                System.out.print("Имя файла (по умолчанию export.json): ");
                String fname = sc.nextLine().trim();
                if (fname.isEmpty()) fname = "export.json";
                export(fname);
            } else if (cmd.equals("import")) {
                System.out.print("Имя файла: ");
                String fname = sc.nextLine().trim();
                if (!fname.isEmpty()) import_(fname);
                else printColor("Укажите имя файла.", "red");
            } else {
                printColor("Неизвестная команда.", "red");
            }
        }
        sc.close();
    }

    public static void main(String[] args) {
        new BookmarkManagerJava().run();
    }
}
