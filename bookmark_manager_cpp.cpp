// bookmark_manager_cpp.cpp — менеджер закладок с тегами на C++

#include <iostream>
#include <vector>
#include <string>
#include <fstream>
#include <sstream>
#include <algorithm>
#include <nlohmann/json.hpp>

using namespace std;
using json = nlohmann::json;

struct Bookmark {
    int id;
    string title;
    string url;
    vector<string> tags;
    string notes;
    string created;

    json to_json() const {
        return {
            {"id", id},
            {"title", title},
            {"url", url},
            {"tags", tags},
            {"notes", notes},
            {"created", created}
        };
    }

    static Bookmark from_json(const json& j) {
        Bookmark b;
        b.id = j["id"];
        b.title = j["title"];
        b.url = j["url"];
        b.tags = j["tags"].get<vector<string>>();
        b.notes = j.value("notes", "");
        b.created = j.value("created", "");
        return b;
    }
};

class BookmarkManager {
private:
    vector<Bookmark> bookmarks;
    int next_id = 1;
    string data_file = "bookmarks.json";

public:
    BookmarkManager() { load(); }

    void load() {
        ifstream f(data_file);
        if (f.is_open()) {
            json data;
            f >> data;
            if (data.is_array()) {
                for (auto& item : data) {
                    Bookmark b = Bookmark::from_json(item);
                    bookmarks.push_back(b);
                    if (b.id >= next_id) next_id = b.id + 1;
                }
            }
            f.close();
        }
    }

    void save() {
        json arr = json::array();
        for (const auto& b : bookmarks) {
            arr.push_back(b.to_json());
        }
        ofstream f(data_file);
        f << arr.dump(4);
        f.close();
    }

    int add(const string& title, const string& url, const vector<string>& tags, const string& notes = "") {
        Bookmark b;
        b.id = next_id++;
        b.title = title;
        b.url = url;
        b.tags = tags;
        b.notes = notes;
        // created заполним сейчас
        time_t now = time(nullptr);
        char buf[20];
        strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S", localtime(&now));
        b.created = string(buf);
        bookmarks.push_back(b);
        save();
        return b.id;
    }

    void remove(int id) {
        bookmarks.erase(remove_if(bookmarks.begin(), bookmarks.end(),
            [id](const Bookmark& b){ return b.id == id; }), bookmarks.end());
        save();
    }

    bool edit(int id, const string& title, const string& url, const vector<string>& tags, const string& notes) {
        for (auto& b : bookmarks) {
            if (b.id == id) {
                if (!title.empty()) b.title = title;
                if (!url.empty()) b.url = url;
                if (!tags.empty()) b.tags = tags;
                if (!notes.empty()) b.notes = notes;
                save();
                return true;
            }
        }
        return false;
    }

    vector<Bookmark> search(const string& query) const {
        vector<Bookmark> results;
        string q = query;
        transform(q.begin(), q.end(), q.begin(), ::tolower);
        for (const auto& b : bookmarks) {
            string title = b.title;
            string url = b.url;
            transform(title.begin(), title.end(), title.begin(), ::tolower);
            transform(url.begin(), url.end(), url.begin(), ::tolower);
            if (title.find(q) != string::npos || url.find(q) != string::npos) {
                results.push_back(b);
                continue;
            }
            for (const auto& tag : b.tags) {
                string t = tag;
                transform(t.begin(), t.end(), t.begin(), ::tolower);
                if (t.find(q) != string::npos) {
                    results.push_back(b);
                    break;
                }
            }
        }
        return results;
    }

    vector<string> get_all_tags() const {
        vector<string> tags;
        for (const auto& b : bookmarks) {
            for (const auto& tag : b.tags) {
                if (find(tags.begin(), tags.end(), tag) == tags.end())
                    tags.push_back(tag);
            }
        }
        sort(tags.begin(), tags.end());
        return tags;
    }

    vector<Bookmark> get_by_tag(const string& tag) const {
        vector<Bookmark> results;
        for (const auto& b : bookmarks) {
            if (find(b.tags.begin(), b.tags.end(), tag) != b.tags.end())
                results.push_back(b);
        }
        return results;
    }

    void export_(const string& filename = "export.json") {
        json arr = json::array();
        for (const auto& b : bookmarks) arr.push_back(b.to_json());
        ofstream f(filename);
        f << arr.dump(4);
        f.close();
        cout << "Экспортировано в " << filename << endl;
    }

    void import_(const string& filename) {
        ifstream f(filename);
        if (!f.is_open()) {
            cout << "Файл не найден." << endl;
            return;
        }
        json data;
        f >> data;
        if (data.is_array()) {
            for (auto& item : data) {
                Bookmark b = Bookmark::from_json(item);
                if (b.id >= next_id) next_id = b.id + 1;
                bookmarks.push_back(b);
            }
            save();
            cout << "Импортировано из " << filename << endl;
        }
        f.close();
    }

    void list_all() const {
        if (bookmarks.empty()) {
            cout << "Закладок нет." << endl;
            return;
        }
        for (const auto& b : bookmarks) {
            cout << "ID " << b.id << ": " << b.title << " (" << b.url << ") Теги: ";
            for (size_t i = 0; i < b.tags.size(); ++i) {
                if (i) cout << ", ";
                cout << b.tags[i];
            }
            if (b.tags.empty()) cout << "(без тегов)";
            cout << endl;
        }
    }
};

void print_color(const string& text, const string& color = "") {
#ifdef _WIN32
    cout << text;
#else
    if (color == "green") cout << "\033[32m";
    else if (color == "red") cout << "\033[31m";
    else if (color == "yellow") cout << "\033[33m";
    else if (color == "blue") cout << "\033[34m";
    else if (color == "cyan") cout << "\033[36m";
    cout << text << "\033[0m";
#endif
}

int main() {
    BookmarkManager mgr;
    print_color("🏷️ TagManager Pro — C++ Edition\n", "cyan");
    cout << "Команды: add, list, search, delete, edit, tags, export, import, exit" << endl;
    string cmd;
    while (true) {
        cout << "> ";
        getline(cin, cmd);
        if (cmd == "exit") break;
        else if (cmd == "add") {
            string title, url, notes, tags_line;
            cout << "Название: "; getline(cin, title);
            cout << "URL: "; getline(cin, url);
            cout << "Теги (через запятую): "; getline(cin, tags_line);
            cout << "Заметки (опционально): "; getline(cin, notes);
            if (title.empty() || url.empty()) {
                print_color("Название и URL обязательны.\n", "red");
                continue;
            }
            vector<string> tags;
            stringstream ss(tags_line);
            string tag;
            while (getline(ss, tag, ',')) {
                tag.erase(0, tag.find_first_not_of(" \t"));
                tag.erase(tag.find_last_not_of(" \t")+1);
                if (!tag.empty()) tags.push_back(tag);
            }
            int id = mgr.add(title, url, tags, notes);
            print_color("✅ Закладка добавлена (ID: " + to_string(id) + ")\n", "green");
        } else if (cmd == "list") {
            mgr.list_all();
        } else if (cmd == "search") {
            string query;
            cout << "Поиск: "; getline(cin, query);
            auto results = mgr.search(query);
            if (results.empty()) {
                print_color("Ничего не найдено.\n", "yellow");
            } else {
                for (const auto& b : results) {
                    cout << "ID " << b.id << ": " << b.title << " (" << b.url << ") Теги: ";
                    for (size_t i=0; i<b.tags.size(); ++i) {
                        if (i) cout << ", ";
                        cout << b.tags[i];
                    }
                    cout << endl;
                }
            }
        } else if (cmd == "delete") {
            cout << "ID закладки: ";
            string id_str; getline(cin, id_str);
            try {
                int id = stoi(id_str);
                mgr.remove(id);
                print_color("Закладка #" + to_string(id) + " удалена.\n", "green");
            } catch (...) {
                print_color("Введите число.\n", "red");
            }
        } else if (cmd == "edit") {
            cout << "ID закладки: ";
            string id_str; getline(cin, id_str);
            try {
                int id = stoi(id_str);
                // найти закладку
                // упростим: запросим поля
                string title, url, tags_line, notes;
                cout << "Новое название (Enter для пропуска): "; getline(cin, title);
                cout << "Новый URL (Enter для пропуска): "; getline(cin, url);
                cout << "Новые теги (через запятую, Enter для пропуска): "; getline(cin, tags_line);
                cout << "Новые заметки (Enter для пропуска): "; getline(cin, notes);
                vector<string> tags;
                if (!tags_line.empty()) {
                    stringstream ss(tags_line);
                    string tag;
                    while (getline(ss, tag, ',')) {
                        tag.erase(0, tag.find_first_not_of(" \t"));
                        tag.erase(tag.find_last_not_of(" \t")+1);
                        if (!tag.empty()) tags.push_back(tag);
                    }
                }
                if (mgr.edit(id, title, url, tags, notes)) {
                    print_color("Закладка обновлена.\n", "green");
                } else {
                    print_color("Закладка не найдена.\n", "red");
                }
            } catch (...) {
                print_color("Введите число.\n", "red");
            }
        } else if (cmd == "tags") {
            auto tags = mgr.get_all_tags();
            if (tags.empty()) {
                print_color("Тегов нет.\n", "yellow");
            } else {
                print_color("Все теги: ", "blue");
                for (size_t i=0; i<tags.size(); ++i) {
                    if (i) cout << ", ";
                    cout << tags[i];
                }
                cout << endl;
            }
        } else if (cmd == "export") {
            cout << "Имя файла (по умолчанию export.json): ";
            string fname; getline(cin, fname);
            if (fname.empty()) fname = "export.json";
            mgr.export_(fname);
        } else if (cmd == "import") {
            cout << "Имя файла: ";
            string fname; getline(cin, fname);
            if (!fname.empty()) mgr.import_(fname);
            else print_color("Укажите имя файла.\n", "red");
        } else {
            print_color("Неизвестная команда.\n", "red");
        }
    }
    return 0;
}
