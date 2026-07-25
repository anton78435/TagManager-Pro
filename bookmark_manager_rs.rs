// bookmark_manager_rs.rs — менеджер закладок с тегами на Rust

use serde::{Deserialize, Serialize};
use std::collections::HashSet;
use std::fs;
use std::io::{self, Write, BufRead};
use std::time::{SystemTime, UNIX_EPOCH};
use chrono::{NaiveDateTime, Local};

#[derive(Serialize, Deserialize, Clone)]
struct Bookmark {
    id: u32,
    title: String,
    url: String,
    tags: Vec<String>,
    notes: String,
    created: String,
}

struct Manager {
    bookmarks: Vec<Bookmark>,
    next_id: u32,
    data_file: String,
}

impl Manager {
    fn new() -> Self {
        let mut mgr = Manager {
            bookmarks: Vec::new(),
            next_id: 1,
            data_file: "bookmarks.json".to_string(),
        };
        mgr.load();
        mgr
    }

    fn load(&mut self) {
        if let Ok(data) = fs::read_to_string(&self.data_file) {
            if let Ok(bms) = serde_json::from_str::<Vec<Bookmark>>(&data) {
                self.bookmarks = bms;
                if let Some(last) = self.bookmarks.last() {
                    self.next_id = last.id + 1;
                }
            }
        }
    }

    fn save(&self) {
        if let Ok(json) = serde_json::to_string_pretty(&self.bookmarks) {
            let _ = fs::write(&self.data_file, json);
        }
    }

    fn add(&mut self, title: &str, url: &str, tags: Vec<String>, notes: &str) -> u32 {
        let id = self.next_id;
        self.next_id += 1;
        let now = Local::now().format("%Y-%m-%d %H:%M:%S").to_string();
        let b = Bookmark {
            id,
            title: title.to_string(),
            url: url.to_string(),
            tags,
            notes: notes.to_string(),
            created: now,
        };
        self.bookmarks.push(b);
        self.save();
        id
    }

    fn delete(&mut self, id: u32) {
        self.bookmarks.retain(|b| b.id != id);
        self.save();
    }

    fn edit(&mut self, id: u32, title: Option<String>, url: Option<String>, tags: Option<Vec<String>>, notes: Option<String>) -> bool {
        for b in &mut self.bookmarks {
            if b.id == id {
                if let Some(t) = title { b.title = t; }
                if let Some(u) = url { b.url = u; }
                if let Some(t) = tags { b.tags = t; }
                if let Some(n) = notes { b.notes = n; }
                self.save();
                return true;
            }
        }
        false
    }

    fn search(&self, query: &str) -> Vec<&Bookmark> {
        let q = query.to_lowercase();
        let mut results = Vec::new();
        for b in &self.bookmarks {
            if b.title.to_lowercase().contains(&q) || b.url.to_lowercase().contains(&q) {
                results.push(b);
                continue;
            }
            for tag in &b.tags {
                if tag.to_lowercase().contains(&q) {
                    results.push(b);
                    break;
                }
            }
        }
        results
    }

    fn get_all_tags(&self) -> Vec<String> {
        let mut set = HashSet::new();
        for b in &self.bookmarks {
            for tag in &b.tags {
                set.insert(tag.clone());
            }
        }
        let mut tags: Vec<String> = set.into_iter().collect();
        tags.sort();
        tags
    }

    fn export(&self, filename: &str) {
        if let Ok(json) = serde_json::to_string_pretty(&self.bookmarks) {
            let _ = fs::write(filename, json);
            println!("Экспортировано в {}", filename);
        }
    }

    fn import_(&mut self, filename: &str) {
        if let Ok(data) = fs::read_to_string(filename) {
            if let Ok(bms) = serde_json::from_str::<Vec<Bookmark>>(&data) {
                for mut b in bms {
                    if b.id >= self.next_id {
                        self.next_id = b.id + 1;
                    }
                    self.bookmarks.push(b);
                }
                self.save();
                println!("Импортировано из {}", filename);
            } else {
                println!("Ошибка формата JSON.");
            }
        } else {
            println!("Файл не найден.");
        }
    }

    fn list_all(&self) {
        if self.bookmarks.is_empty() {
            println!("Закладок нет.");
            return;
        }
        for b in &self.bookmarks {
            print!("ID {}: {} ({}) Теги: ", b.id, b.title, b.url);
            if b.tags.is_empty() {
                println!("(без тегов)");
            } else {
                println!("{}", b.tags.join(", "));
            }
        }
    }
}

fn print_color(text: &str, color: &str) {
    let code = match color {
        "green" => "\x1b[32m",
        "red" => "\x1b[31m",
        "yellow" => "\x1b[33m",
        "blue" => "\x1b[34m",
        "cyan" => "\x1b[36m",
        _ => "",
    };
    println!("{}{}\x1b[0m", code, text);
}

fn main() {
    let mut mgr = Manager::new();
    print_color("🏷️ TagManager Pro — Rust Edition", "cyan");
    println!("Команды: add, list, search, delete, edit, tags, export, import, exit");
    let stdin = io::stdin();
    let mut reader = stdin.lock();
    loop {
        print!("> ");
        io::stdout().flush().unwrap();
        let mut line = String::new();
        if reader.read_line(&mut line).is_err() { break; }
        let line = line.trim();
        if line.is_empty() { continue; }
        let parts: Vec<&str> = line.split_whitespace().collect();
        let cmd = parts[0];
        match cmd {
            "exit" => break,
            "add" => {
                print!("Название: ");
                io::stdout().flush().unwrap();
                let mut title = String::new();
                reader.read_line(&mut title).unwrap();
                let title = title.trim();
                print!("URL: ");
                io::stdout().flush().unwrap();
                let mut url = String::new();
                reader.read_line(&mut url).unwrap();
                let url = url.trim();
                print!("Теги (через запятую): ");
                io::stdout().flush().unwrap();
                let mut tags_line = String::new();
                reader.read_line(&mut tags_line).unwrap();
                let tags_line = tags_line.trim();
                let mut tags = Vec::new();
                if !tags_line.is_empty() {
                    for t in tags_line.split(',') {
                        let t = t.trim();
                        if !t.is_empty() { tags.push(t.to_string()); }
                    }
                }
                print!("Заметки (опционально): ");
                io::stdout().flush().unwrap();
                let mut notes = String::new();
                reader.read_line(&mut notes).unwrap();
                let notes = notes.trim();
                if title.is_empty() || url.is_empty() {
                    print_color("Название и URL обязательны.", "red");
                    continue;
                }
                let id = mgr.add(title, url, tags, notes);
                print_color(&format!("✅ Закладка добавлена (ID: {})", id), "green");
            }
            "list" => mgr.list_all(),
            "search" => {
                print!("Поиск: ");
                io::stdout().flush().unwrap();
                let mut query = String::new();
                reader.read_line(&mut query).unwrap();
                let query = query.trim();
                let results = mgr.search(query);
                if results.is_empty() {
                    print_color("Ничего не найдено.", "yellow");
                } else {
                    for b in results {
                        print!("ID {}: {} ({}) Теги: ", b.id, b.title, b.url);
                        if b.tags.is_empty() { println!("(без тегов)"); }
                        else { println!("{}", b.tags.join(", ")); }
                    }
                }
            }
            "delete" => {
                print!("ID закладки: ");
                io::stdout().flush().unwrap();
                let mut id_str = String::new();
                reader.read_line(&mut id_str).unwrap();
                let id_str = id_str.trim();
                if let Ok(id) = id_str.parse::<u32>() {
                    mgr.delete(id);
                    print_color(&format!("Закладка #{} удалена.", id), "green");
                } else {
                    print_color("Введите число.", "red");
                }
            }
            "edit" => {
                print!("ID закладки: ");
                io::stdout().flush().unwrap();
                let mut id_str = String::new();
                reader.read_line(&mut id_str).unwrap();
                let id_str = id_str.trim();
                let id: u32 = match id_str.parse() {
                    Ok(v) => v,
                    Err(_) => {
                        print_color("Введите число.", "red");
                        continue;
                    }
                };
                print!("Новое название (Enter для пропуска): ");
                io::stdout().flush().unwrap();
                let mut title = String::new();
                reader.read_line(&mut title).unwrap();
                let title = title.trim();
                let title = if title.is_empty() { None } else { Some(title.to_string()) };
                print!("Новый URL (Enter для пропуска): ");
                io::stdout().flush().unwrap();
                let mut url = String::new();
                reader.read_line(&mut url).unwrap();
                let url = url.trim();
                let url = if url.is_empty() { None } else { Some(url.to_string()) };
                print!("Новые теги (через запятую, Enter для пропуска): ");
                io::stdout().flush().unwrap();
                let mut tags_line = String::new();
                reader.read_line(&mut tags_line).unwrap();
                let tags_line = tags_line.trim();
                let tags = if tags_line.is_empty() {
                    None
                } else {
                    let mut v = Vec::new();
                    for t in tags_line.split(',') {
                        let t = t.trim();
                        if !t.is_empty() { v.push(t.to_string()); }
                    }
                    Some(v)
                };
                print!("Новые заметки (Enter для пропуска): ");
                io::stdout().flush().unwrap();
                let mut notes = String::new();
                reader.read_line(&mut notes).unwrap();
                let notes = notes.trim();
                let notes = if notes.is_empty() { None } else { Some(notes.to_string()) };
                if mgr.edit(id, title, url, tags, notes) {
                    print_color("Закладка обновлена.", "green");
                } else {
                    print_color("Закладка не найдена.", "red");
                }
            }
            "tags" => {
                let tags = mgr.get_all_tags();
                if tags.is_empty() {
                    print_color("Тегов нет.", "yellow");
                } else {
                    print_color("Все теги: ", "blue");
                    println!("{}", tags.join(", "));
                }
            }
            "export" => {
                print!("Имя файла (по умолчанию export.json): ");
                io::stdout().flush().unwrap();
                let mut fname = String::new();
                reader.read_line(&mut fname).unwrap();
                let fname = fname.trim();
                let fname = if fname.is_empty() { "export.json" } else { fname };
                mgr.export(fname);
            }
            "import" => {
                print!("Имя файла: ");
                io::stdout().flush().unwrap();
                let mut fname = String::new();
                reader.read_line(&mut fname).unwrap();
                let fname = fname.trim();
                if fname.is_empty() {
                    print_color("Укажите имя файла.", "red");
                } else {
                    mgr.import_(fname);
                }
            }
            _ => print_color("Неизвестная команда.", "red"),
        }
    }
}
