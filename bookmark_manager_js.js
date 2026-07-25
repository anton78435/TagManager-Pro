// bookmark_manager_js.js — менеджер закладок с тегами на JavaScript (Node.js)

const fs = require('fs');
const readline = require('readline');

const DATA_FILE = 'bookmarks.json';
let bookmarks = [];
let nextId = 1;

function load() {
    try {
        if (fs.existsSync(DATA_FILE)) {
            const data = fs.readFileSync(DATA_FILE, 'utf8');
            bookmarks = JSON.parse(data);
            if (bookmarks.length > 0) {
                nextId = Math.max(...bookmarks.map(b => b.id)) + 1;
            }
        }
    } catch (e) {}
}

function save() {
    fs.writeFileSync(DATA_FILE, JSON.stringify(bookmarks, null, 2));
}

class Bookmark {
    constructor(id, title, url, tags, notes) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.tags = tags || [];
        this.notes = notes || '';
        this.created = new Date().toISOString();
    }
}

function add(title, url, tags, notes) {
    const b = new Bookmark(nextId++, title, url, tags, notes);
    bookmarks.push(b);
    save();
    return b.id;
}

function deleteById(id) {
    bookmarks = bookmarks.filter(b => b.id !== id);
    save();
}

function edit(id, title, url, tags, notes) {
    const b = bookmarks.find(b => b.id === id);
    if (!b) return false;
    if (title !== undefined) b.title = title;
    if (url !== undefined) b.url = url;
    if (tags !== undefined) b.tags = tags;
    if (notes !== undefined) b.notes = notes;
    save();
    return true;
}

function search(query) {
    query = query.toLowerCase();
    return bookmarks.filter(b =>
        b.title.toLowerCase().includes(query) ||
        b.url.toLowerCase().includes(query) ||
        b.tags.some(t => t.toLowerCase().includes(query))
    );
}

function getAllTags() {
    const tagSet = new Set();
    bookmarks.forEach(b => b.tags.forEach(t => tagSet.add(t)));
    return Array.from(tagSet).sort();
}

function exportData(filename) {
    fs.writeFileSync(filename, JSON.stringify(bookmarks, null, 2));
    console.log(`Экспортировано в ${filename}`);
}

function importData(filename) {
    try {
        const data = fs.readFileSync(filename, 'utf8');
        const imported = JSON.parse(data);
        imported.forEach(b => {
            if (b.id >= nextId) nextId = b.id + 1;
            bookmarks.push(b);
        });
        save();
        console.log(`Импортировано из ${filename}`);
    } catch (e) {
        console.log('Ошибка импорта:', e.message);
    }
}

function listAll() {
    if (bookmarks.length === 0) {
        console.log('Закладок нет.');
        return;
    }
    for (const b of bookmarks) {
        console.log(`ID ${b.id}: ${b.title} (${b.url}) Теги: ${b.tags.length ? b.tags.join(', ') : '(без тегов)'}`);
    }
}

const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    prompt: '> '
});

function printColor(text, color) {
    const colors = {
        green: '\x1b[32m',
        red: '\x1b[31m',
        yellow: '\x1b[33m',
        blue: '\x1b[34m',
        cyan: '\x1b[36m'
    };
    console.log((colors[color] || '') + text + '\x1b[0m');
}

function ask(question) {
    return new Promise(resolve => rl.question(question, resolve));
}

async function main() {
    load();
    printColor('🏷️ TagManager Pro — JavaScript Edition', 'cyan');
    console.log('Команды: add, list, search, delete, edit, tags, export, import, exit');
    rl.prompt();

    rl.on('line', async (line) => {
        const parts = line.trim().split(' ');
        const cmd = parts[0];
        switch (cmd) {
            case 'exit':
                save();
                console.log('До свидания!');
                rl.close();
                return;
            case 'add':
                const title = await ask('Название: ');
                const url = await ask('URL: ');
                const tagsLine = await ask('Теги (через запятую): ');
                const notes = await ask('Заметки (опционально): ');
                if (!title.trim() || !url.trim()) {
                    printColor('Название и URL обязательны.', 'red');
                    break;
                }
                const tags = tagsLine.trim() ? tagsLine.split(',').map(t => t.trim()).filter(t => t) : [];
                const id = add(title.trim(), url.trim(), tags, notes.trim());
                printColor(`✅ Закладка добавлена (ID: ${id})`, 'green');
                break;
            case 'list':
                listAll();
                break;
            case 'search':
                const query = await ask('Поиск: ');
                const results = search(query.trim());
                if (results.length === 0) {
                    printColor('Ничего не найдено.', 'yellow');
                } else {
                    for (const b of results) {
                        console.log(`ID ${b.id}: ${b.title} (${b.url}) Теги: ${b.tags.length ? b.tags.join(', ') : '(без тегов)'}`);
                    }
                }
                break;
            case 'delete':
                const delId = await ask('ID закладки: ');
                const dId = parseInt(delId.trim());
                if (isNaN(dId)) {
                    printColor('Введите число.', 'red');
                    break;
                }
                deleteById(dId);
                printColor(`Закладка #${dId} удалена.`, 'green');
                break;
            case 'edit':
                const editId = await ask('ID закладки: ');
                const eId = parseInt(editId.trim());
                if (isNaN(eId)) {
                    printColor('Введите число.', 'red');
                    break;
                }
                const newTitle = await ask('Новое название (Enter для пропуска): ');
                const newUrl = await ask('Новый URL (Enter для пропуска): ');
                const newTagsLine = await ask('Новые теги (через запятую, Enter для пропуска): ');
                const newNotes = await ask('Новые заметки (Enter для пропуска): ');
                let tagsEdit = undefined;
                if (newTagsLine.trim()) {
                    tagsEdit = newTagsLine.split(',').map(t => t.trim()).filter(t => t);
                }
                if (edit(eId, newTitle.trim() || undefined, newUrl.trim() || undefined, tagsEdit, newNotes.trim() || undefined)) {
                    printColor('Закладка обновлена.', 'green');
                } else {
                    printColor('Закладка не найдена.', 'red');
                }
                break;
            case 'tags':
                const allTags = getAllTags();
                if (allTags.length === 0) {
                    printColor('Тегов нет.', 'yellow');
                } else {
                    printColor('Все теги: ', 'blue');
                    console.log(allTags.join(', '));
                }
                break;
            case 'export':
                const expFile = await ask('Имя файла (по умолчанию export.json): ');
                exportData(expFile.trim() || 'export.json');
                break;
            case 'import':
                const impFile = await ask('Имя файла: ');
                if (!impFile.trim()) {
                    printColor('Укажите имя файла.', 'red');
                } else {
                    importData(impFile.trim());
                }
                break;
            default:
                printColor('Неизвестная команда.', 'red');
        }
        rl.prompt();
    }).on('close', () => {
        save();
        process.exit(0);
    });
}

main();
