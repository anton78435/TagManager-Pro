# bookmark_manager_python.py — менеджер закладок с тегами на Python

import json
import os
import sys
from datetime import datetime
from collections import defaultdict

try:
    from colorama import init, Fore, Style
    init(autoreset=True)
    HAS_COLOR = True
except ImportError:
    HAS_COLOR = False

DATA_FILE = "bookmarks.json"

class Bookmark:
    def __init__(self, id, title, url, tags=None, notes=""):
        self.id = id
        self.title = title
        self.url = url
        self.tags = tags if tags else []
        self.notes = notes
        self.created = datetime.now().isoformat()

    def to_dict(self):
        return {
            "id": self.id,
            "title": self.title,
            "url": self.url,
            "tags": self.tags,
            "notes": self.notes,
            "created": self.created
        }

    @classmethod
    def from_dict(cls, data):
        b = cls(data["id"], data["title"], data["url"], data["tags"], data["notes"])
        b.created = data.get("created", datetime.now().isoformat())
        return b

class BookmarkManager:
    def __init__(self):
        self.bookmarks = []
        self.next_id = 1
        self.load()

    def load(self):
        if os.path.exists(DATA_FILE):
            with open(DATA_FILE, 'r') as f:
                data = json.load(f)
                self.bookmarks = [Bookmark.from_dict(d) for d in data]
                if self.bookmarks:
                    self.next_id = max(b.id for b in self.bookmarks) + 1

    def save(self):
        with open(DATA_FILE, 'w') as f:
            json.dump([b.to_dict() for b in self.bookmarks], f, indent=2)

    def add(self, title, url, tags, notes=""):
        b = Bookmark(self.next_id, title, url, tags, notes)
        self.bookmarks.append(b)
        self.next_id += 1
        self.save()
        return b.id

    def delete(self, id):
        self.bookmarks = [b for b in self.bookmarks if b.id != id]
        self.save()

    def edit(self, id, title=None, url=None, tags=None, notes=None):
        for b in self.bookmarks:
            if b.id == id:
                if title is not None: b.title = title
                if url is not None: b.url = url
                if tags is not None: b.tags = tags
                if notes is not None: b.notes = notes
                self.save()
                return True
        return False

    def search(self, query):
        query = query.lower()
        results = []
        for b in self.bookmarks:
            if query in b.title.lower() or query in b.url.lower():
                results.append(b)
            elif any(query in tag.lower() for tag in b.tags):
                results.append(b)
        return results

    def get_all_tags(self):
        tags = set()
        for b in self.bookmarks:
            tags.update(b.tags)
        return sorted(tags)

    def get_by_tag(self, tag):
        return [b for b in self.bookmarks if tag in b.tags]

    def export(self, filename="export.json"):
        with open(filename, 'w') as f:
            json.dump([b.to_dict() for b in self.bookmarks], f, indent=2)
        print(f"Экспортировано в {filename}")

    def import_(self, filename):
        with open(filename, 'r') as f:
            data = json.load(f)
            for d in data:
                b = Bookmark.from_dict(d)
                if b.id >= self.next_id:
                    self.next_id = b.id + 1
                self.bookmarks.append(b)
            self.save()
        print(f"Импортировано из {filename}")

def color_print(text, color=None):
    if HAS_COLOR:
        colors = {
            'green': Fore.GREEN,
            'red': Fore.RED,
            'yellow': Fore.YELLOW,
            'blue': Fore.BLUE,
            'magenta': Fore.MAGENTA,
            'cyan': Fore.CYAN,
        }
        print(colors.get(color, '') + text + Style.RESET_ALL)
    else:
        print(text)

def main():
    mgr = BookmarkManager()
    color_print("🏷️ TagManager Pro — Python Edition", 'cyan')
    print("Команды: add, list, search, delete, edit, tags, export, import, exit")
    while True:
        cmd = input("> ").strip().lower()
        if not cmd:
            continue
        if cmd == "exit":
            break
        elif cmd == "add":
            title = input("Название: ").strip()
            url = input("URL: ").strip()
            tags = [t.strip() for t in input("Теги (через запятую): ").split(",") if t.strip()]
            notes = input("Заметки (опционально): ").strip()
            if not title or not url:
                color_print("Название и URL обязательны.", 'red')
                continue
            id = mgr.add(title, url, tags, notes)
            color_print(f"✅ Закладка добавлена (ID: {id})", 'green')
        elif cmd == "list":
            if not mgr.bookmarks:
                color_print("Закладок нет.", 'yellow')
            else:
                for b in mgr.bookmarks:
                    tags_str = ", ".join(b.tags) if b.tags else "(без тегов)"
                    print(f"ID {b.id}: {b.title} ({b.url}) Теги: {tags_str}")
        elif cmd == "search":
            query = input("Поиск: ").strip()
            results = mgr.search(query)
            if not results:
                color_print("Ничего не найдено.", 'yellow')
            else:
                for b in results:
                    print(f"ID {b.id}: {b.title} ({b.url}) Теги: {', '.join(b.tags)}")
        elif cmd == "delete":
            try:
                id = int(input("ID закладки: ").strip())
                mgr.delete(id)
                color_print(f"Закладка #{id} удалена.", 'green')
            except ValueError:
                color_print("Введите число.", 'red')
        elif cmd == "edit":
            try:
                id = int(input("ID закладки: ").strip())
                b = next((b for b in mgr.bookmarks if b.id == id), None)
                if not b:
                    color_print("Закладка не найдена.", 'red')
                    continue
                title = input(f"Новое название (было {b.title}): ").strip() or None
                url = input(f"Новый URL (был {b.url}): ").strip() or None
                tags = input(f"Новые теги (были {', '.join(b.tags)}): ").strip()
                tags = [t.strip() for t in tags.split(",") if t.strip()] if tags else None
                notes = input(f"Новые заметки (были {b.notes}): ").strip() or None
                if mgr.edit(id, title, url, tags, notes):
                    color_print("Закладка обновлена.", 'green')
                else:
                    color_print("Ошибка обновления.", 'red')
            except ValueError:
                color_print("Введите число.", 'red')
        elif cmd == "tags":
            tags = mgr.get_all_tags()
            if tags:
                color_print("Все теги: " + ", ".join(tags), 'blue')
            else:
                color_print("Тегов нет.", 'yellow')
        elif cmd == "export":
            filename = input("Имя файла (по умолчанию export.json): ").strip()
            if not filename:
                filename = "export.json"
            mgr.export(filename)
        elif cmd == "import":
            filename = input("Имя файла: ").strip()
            if filename:
                try:
                    mgr.import_(filename)
                except FileNotFoundError:
                    color_print("Файл не найден.", 'red')
            else:
                color_print("Укажите имя файла.", 'red')
        else:
            color_print("Неизвестная команда.", 'red')

if __name__ == "__main__":
    main()
