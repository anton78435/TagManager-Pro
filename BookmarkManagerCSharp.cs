// BookmarkManagerCSharp.cs — менеджер закладок с тегами на C#

using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;

class Bookmark
{
    public int Id { get; set; }
    public string Title { get; set; }
    public string Url { get; set; }
    public List<string> Tags { get; set; } = new List<string>();
    public string Notes { get; set; } = "";
    public string Created { get; set; }
}

class BookmarkManager
{
    private List<Bookmark> bookmarks = new List<Bookmark>();
    private int nextId = 1;
    private string dataFile = "bookmarks.json";

    public BookmarkManager()
    {
        Load();
    }

    private void Load()
    {
        if (File.Exists(dataFile))
        {
            try
            {
                string json = File.ReadAllText(dataFile);
                bookmarks = JsonSerializer.Deserialize<List<Bookmark>>(json) ?? new List<Bookmark>();
                if (bookmarks.Any()) nextId = bookmarks.Max(b => b.Id) + 1;
            }
            catch { }
        }
    }

    private void Save()
    {
        string json = JsonSerializer.Serialize(bookmarks, new JsonSerializerOptions { WriteIndented = true });
        File.WriteAllText(dataFile, json);
    }

    public int Add(string title, string url, List<string> tags, string notes = "")
    {
        var b = new Bookmark
        {
            Id = nextId++,
            Title = title,
            Url = url,
            Tags = tags ?? new List<string>(),
            Notes = notes ?? "",
            Created = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss")
        };
        bookmarks.Add(b);
        Save();
        return b.Id;
    }

    public void Delete(int id)
    {
        bookmarks.RemoveAll(b => b.Id == id);
        Save();
    }

    public bool Edit(int id, string title, string url, List<string> tags, string notes)
    {
        var b = bookmarks.FirstOrDefault(b => b.Id == id);
        if (b == null) return false;
        if (!string.IsNullOrEmpty(title)) b.Title = title;
        if (!string.IsNullOrEmpty(url)) b.Url = url;
        if (tags != null) b.Tags = tags;
        if (!string.IsNullOrEmpty(notes)) b.Notes = notes;
        Save();
        return true;
    }

    public List<Bookmark> Search(string query)
    {
        query = query.ToLower();
        return bookmarks.Where(b =>
            b.Title.ToLower().Contains(query) ||
            b.Url.ToLower().Contains(query) ||
            b.Tags.Any(t => t.ToLower().Contains(query))
        ).ToList();
    }

    public List<string> GetAllTags()
    {
        return bookmarks.SelectMany(b => b.Tags).Distinct().OrderBy(t => t).ToList();
    }

    public List<Bookmark> GetByTag(string tag)
    {
        return bookmarks.Where(b => b.Tags.Contains(tag)).ToList();
    }

    public void Export(string filename = "export.json")
    {
        string json = JsonSerializer.Serialize(bookmarks, new JsonSerializerOptions { WriteIndented = true });
        File.WriteAllText(filename, json);
        Console.WriteLine($"Экспортировано в {filename}");
    }

    public void Import(string filename)
    {
        if (!File.Exists(filename)) { Console.WriteLine("Файл не найден."); return; }
        string json = File.ReadAllText(filename);
        var imported = JsonSerializer.Deserialize<List<Bookmark>>(json);
        if (imported != null)
        {
            foreach (var b in imported)
            {
                if (b.Id >= nextId) nextId = b.Id + 1;
                bookmarks.Add(b);
            }
            Save();
            Console.WriteLine($"Импортировано из {filename}");
        }
    }

    public void ListAll()
    {
        if (!bookmarks.Any())
        {
            Console.WriteLine("Закладок нет.");
            return;
        }
        foreach (var b in bookmarks)
        {
            Console.Write($"ID {b.Id}: {b.Title} ({b.Url}) Теги: ");
            if (!b.Tags.Any()) Console.Write("(без тегов)");
            else Console.Write(string.Join(", ", b.Tags));
            Console.WriteLine();
        }
    }

    private void PrintColor(string text, ConsoleColor color = ConsoleColor.White)
    {
        Console.ForegroundColor = color;
        Console.WriteLine(text);
        Console.ResetColor();
    }

    public void Run()
    {
        PrintColor("🏷️ TagManager Pro — C# Edition", ConsoleColor.Cyan);
        Console.WriteLine("Команды: add, list, search, delete, edit, tags, export, import, exit");
        while (true)
        {
            Console.Write("> ");
            string line = Console.ReadLine()?.Trim() ?? "";
            if (string.IsNullOrEmpty(line)) continue;
            string cmd = line.Split(' ')[0].ToLower();
            switch (cmd)
            {
                case "exit": return;
                case "add":
                    Console.Write("Название: ");
                    string title = Console.ReadLine()?.Trim();
                    Console.Write("URL: ");
                    string url = Console.ReadLine()?.Trim();
                    Console.Write("Теги (через запятую): ");
                    string tagsLine = Console.ReadLine()?.Trim();
                    Console.Write("Заметки (опционально): ");
                    string notes = Console.ReadLine()?.Trim();
                    if (string.IsNullOrEmpty(title) || string.IsNullOrEmpty(url))
                    {
                        PrintColor("Название и URL обязательны.", ConsoleColor.Red);
                        break;
                    }
                    var tags = new List<string>();
                    if (!string.IsNullOrEmpty(tagsLine))
                    {
                        foreach (var t in tagsLine.Split(','))
                        {
                            var tt = t.Trim();
                            if (!string.IsNullOrEmpty(tt)) tags.Add(tt);
                        }
                    }
                    int id = Add(title, url, tags, notes);
                    PrintColor($"✅ Закладка добавлена (ID: {id})", ConsoleColor.Green);
                    break;
                case "list":
                    ListAll();
                    break;
                case "search":
                    Console.Write("Поиск: ");
                    string query = Console.ReadLine()?.Trim();
                    var results = Search(query);
                    if (!results.Any())
                        PrintColor("Ничего не найдено.", ConsoleColor.Yellow);
                    else
                    {
                        foreach (var b in results)
                        {
                            Console.Write($"ID {b.Id}: {b.Title} ({b.Url}) Теги: ");
                            if (!b.Tags.Any()) Console.Write("(без тегов)");
                            else Console.Write(string.Join(", ", b.Tags));
                            Console.WriteLine();
                        }
                    }
                    break;
                case "delete":
                    Console.Write("ID закладки: ");
                    if (int.TryParse(Console.ReadLine()?.Trim(), out int delId))
                    {
                        Delete(delId);
                        PrintColor($"Закладка #{delId} удалена.", ConsoleColor.Green);
                    }
                    else PrintColor("Введите число.", ConsoleColor.Red);
                    break;
                case "edit":
                    Console.Write("ID закладки: ");
                    if (!int.TryParse(Console.ReadLine()?.Trim(), out int editId))
                    {
                        PrintColor("Введите число.", ConsoleColor.Red);
                        break;
                    }
                    Console.Write("Новое название (Enter для пропуска): ");
                    string newTitle = Console.ReadLine()?.Trim();
                    if (string.IsNullOrEmpty(newTitle)) newTitle = null;
                    Console.Write("Новый URL (Enter для пропуска): ");
                    string newUrl = Console.ReadLine()?.Trim();
                    if (string.IsNullOrEmpty(newUrl)) newUrl = null;
                    Console.Write("Новые теги (через запятую, Enter для пропуска): ");
                    string newTagsLine = Console.ReadLine()?.Trim();
                    List<string> newTags = null;
                    if (!string.IsNullOrEmpty(newTagsLine))
                    {
                        newTags = new List<string>();
                        foreach (var t in newTagsLine.Split(','))
                        {
                            var tt = t.Trim();
                            if (!string.IsNullOrEmpty(tt)) newTags.Add(tt);
                        }
                    }
                    Console.Write("Новые заметки (Enter для пропуска): ");
                    string newNotes = Console.ReadLine()?.Trim();
                    if (string.IsNullOrEmpty(newNotes)) newNotes = null;
                    if (Edit(editId, newTitle, newUrl, newTags, newNotes))
                        PrintColor("Закладка обновлена.", ConsoleColor.Green);
                    else PrintColor("Закладка не найдена.", ConsoleColor.Red);
                    break;
                case "tags":
                    var allTags = GetAllTags();
                    if (!allTags.Any())
                        PrintColor("Тегов нет.", ConsoleColor.Yellow);
                    else
                    {
                        PrintColor("Все теги: ", ConsoleColor.Blue);
                        Console.WriteLine(string.Join(", ", allTags));
                    }
                    break;
                case "export":
                    Console.Write("Имя файла (по умолчанию export.json): ");
                    string expFile = Console.ReadLine()?.Trim();
                    if (string.IsNullOrEmpty(expFile)) expFile = "export.json";
                    Export(expFile);
                    break;
                case "import":
                    Console.Write("Имя файла: ");
                    string impFile = Console.ReadLine()?.Trim();
                    if (!string.IsNullOrEmpty(impFile)) Import(impFile);
                    else PrintColor("Укажите имя файла.", ConsoleColor.Red);
                    break;
                default:
                    PrintColor("Неизвестная команда.", ConsoleColor.Red);
                    break;
            }
        }
    }

    public static void Main()
    {
        new BookmarkManager().Run();
    }
}
