🏷️ TagManager Pro — менеджер закладок с тегами
Умный менеджер закладок с поддержкой тегов, поиском, категоризацией и экспортом.
Реализован на 7 языках программирования для демонстрации работы с данными и пользовательским интерфейсом.

https://img.shields.io/github/repo-size/yourname/tagmanager
https://img.shields.io/github/stars/yourname/tagmanager?style=social
https://img.shields.io/badge/License-MIT-blue.svg

🧠 Концепция
TagManager Pro — это мощный инструмент для организации закладок с помощью тегов. Он позволяет:

✅ Добавлять закладки с названием, URL, тегами и заметками.

✅ Искать закладки по тегам, названию или URL.

✅ Просматривать все теги и фильтровать по ним.

✅ Редактировать и удалять закладки.

✅ Экспортировать/импортировать данные в JSON.

✅ Сохранять историю в локальном файле.

✅ Цветной вывод в консоли (в некоторых версиях).

✅ Кроссплатформенность — работает на Windows, Linux, macOS.

🚀 Как запустить
Для каждой версии требуются соответствующие библиотеки. Инструкции по установке и запуску:

Python
bash
pip install colorama  # опционально, для цветного вывода
python bookmark_manager_python.py
C++
bash
# Требуется nlohmann/json (header-only) — скачайте или установите
# Например: sudo apt install nlohmann-json3-dev
g++ -std=c++17 bookmark_manager_cpp.cpp -o bookmark_manager
./bookmark_manager
Java
bash
# Требуется Gson (скачайте gson.jar)
javac -cp .:gson.jar BookmarkManagerJava.java
java -cp .:gson.jar BookmarkManagerJava
C# (.NET Core)
bash
dotnet new console -n TagManager -f net6.0
dotnet add package System.Text.Json
# Замените Program.cs на код
dotnet run
Go
bash
go mod init tagmanager
go run bookmark_manager_go.go
Rust
bash
cargo new tagmanager
cd tagmanager
# Добавьте зависимости serde и serde_json в Cargo.toml
cargo run
JavaScript (Node.js)
bash
npm install readline
node bookmark_manager_js.js
🧩 Пример сессии
text
$ python bookmark_manager_python.py
🏷️ TagManager Pro — Python Edition
Команды: add, list, search, delete, edit, tags, export, import, exit

> add
Название: GitHub
URL: https://github.com
Теги (через запятую): dev, tools
Заметки: Главный репозиторий
✅ Закладка добавлена (ID: 1)

> list
ID 1: GitHub (https://github.com) Теги: dev, tools

> search dev
ID 1: GitHub (https://github.com) Теги: dev, tools

> tags
Все теги: dev, tools
📦 Содержимое репозитория
Файл	Язык	Особенности
bookmark_manager_python.py	Python	цветной вывод, JSON-хранение
bookmark_manager_cpp.cpp	C++	nlohmann/json, цветной вывод (ANSI)
BookmarkManagerJava.java	Java	Gson, консольное меню
BookmarkManagerCSharp.cs	C#	System.Text.Json, цветной вывод
bookmark_manager_go.go	Go	encoding/json, цветной вывод
bookmark_manager_rs.rs	Rust	serde_json, termion для цветов
bookmark_manager_js.js	JavaScript	fs, readline, цветной вывод (chalk)
🔮 Расширенные функции
Автодополнение тегов (в некоторых версиях).

Статистика — количество закладок по тегам.

Импорт/экспорт в CSV.

Резервное копирование данных.

📜 Лицензия
MIT — свободно используйте, модифицируйте и распространяйте.

🤝 Вклад
Приветствуются пул-реквесты с улучшениями, поддержкой новых платформ и расширением функциональности.

⭐ Если проект помогает вам организовывать закладки — поставьте звёздочку!

