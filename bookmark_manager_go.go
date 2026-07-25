// bookmark_manager_go.go — менеджер закладок с тегами на Go

package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"strconv"
	"strings"
	"time"
)

type Bookmark struct {
	ID      int      `json:"id"`
	Title   string   `json:"title"`
	URL     string   `json:"url"`
	Tags    []string `json:"tags"`
	Notes   string   `json:"notes"`
	Created string   `json:"created"`
}

type Manager struct {
	bookmarks []Bookmark
	nextID    int
	dataFile  string
}

func NewManager() *Manager {
	m := &Manager{dataFile: "bookmarks.json"}
	m.load()
	return m
}

func (m *Manager) load() {
	data, err := ioutil.ReadFile(m.dataFile)
	if err != nil {
		return
	}
	var bms []Bookmark
	if err := json.Unmarshal(data, &bms); err == nil {
		m.bookmarks = bms
		if len(bms) > 0 {
			m.nextID = bms[len(bms)-1].ID + 1
		}
	}
}

func (m *Manager) save() {
	data, _ := json.MarshalIndent(m.bookmarks, "", "  ")
	ioutil.WriteFile(m.dataFile, data, 0644)
}

func (m *Manager) add(title, url string, tags []string, notes string) int {
	b := Bookmark{
		ID:      m.nextID,
		Title:   title,
		URL:     url,
		Tags:    tags,
		Notes:   notes,
		Created: time.Now().Format("2006-01-02 15:04:05"),
	}
	m.bookmarks = append(m.bookmarks, b)
	m.nextID++
	m.save()
	return b.ID
}

func (m *Manager) delete(id int) {
	for i, b := range m.bookmarks {
		if b.ID == id {
			m.bookmarks = append(m.bookmarks[:i], m.bookmarks[i+1:]...)
			m.save()
			return
		}
	}
}

func (m *Manager) edit(id int, title, url string, tags []string, notes string) bool {
	for i, b := range m.bookmarks {
		if b.ID == id {
			if title != "" {
				m.bookmarks[i].Title = title
			}
			if url != "" {
				m.bookmarks[i].URL = url
			}
			if tags != nil {
				m.bookmarks[i].Tags = tags
			}
			if notes != "" {
				m.bookmarks[i].Notes = notes
			}
			m.save()
			return true
		}
	}
	return false
}

func (m *Manager) search(query string) []Bookmark {
	query = strings.ToLower(query)
	var results []Bookmark
	for _, b := range m.bookmarks {
		if strings.Contains(strings.ToLower(b.Title), query) ||
			strings.Contains(strings.ToLower(b.URL), query) {
			results = append(results, b)
			continue
		}
		for _, tag := range b.Tags {
			if strings.Contains(strings.ToLower(tag), query) {
				results = append(results, b)
				break
			}
		}
	}
	return results
}

func (m *Manager) getAllTags() []string {
	tagSet := make(map[string]bool)
	for _, b := range m.bookmarks {
		for _, tag := range b.Tags {
			tagSet[tag] = true
		}
	}
	var tags []string
	for t := range tagSet {
		tags = append(tags, t)
	}
	return tags
}

func (m *Manager) export(filename string) {
	data, _ := json.MarshalIndent(m.bookmarks, "", "  ")
	ioutil.WriteFile(filename, data, 0644)
	fmt.Println("Экспортировано в", filename)
}

func (m *Manager) import_(filename string) {
	data, err := ioutil.ReadFile(filename)
	if err != nil {
		fmt.Println("Файл не найден.")
		return
	}
	var bms []Bookmark
	if err := json.Unmarshal(data, &bms); err != nil {
		fmt.Println("Ошибка формата JSON.")
		return
	}
	for _, b := range bms {
		if b.ID >= m.nextID {
			m.nextID = b.ID + 1
		}
		m.bookmarks = append(m.bookmarks, b)
	}
	m.save()
	fmt.Println("Импортировано из", filename)
}

func (m *Manager) listAll() {
	if len(m.bookmarks) == 0 {
		fmt.Println("Закладок нет.")
		return
	}
	for _, b := range m.bookmarks {
		fmt.Printf("ID %d: %s (%s) Теги: ", b.ID, b.Title, b.URL)
		if len(b.Tags) == 0 {
			fmt.Print("(без тегов)")
		} else {
			fmt.Print(strings.Join(b.Tags, ", "))
		}
		fmt.Println()
	}
}

func printColor(text string, color string) {
	colors := map[string]string{
		"green":  "\033[32m",
		"red":    "\033[31m",
		"yellow": "\033[33m",
		"blue":   "\033[34m",
		"cyan":   "\033[36m",
	}
	fmt.Print(colors[color])
	fmt.Println(text)
	fmt.Print("\033[0m")
}

func main() {
	mgr := NewManager()
	printColor("🏷️ TagManager Pro — Go Edition", "cyan")
	fmt.Println("Команды: add, list, search, delete, edit, tags, export, import, exit")
	scanner := bufio.NewScanner(os.Stdin)
	for {
		fmt.Print("> ")
		if !scanner.Scan() {
			break
		}
		line := strings.TrimSpace(scanner.Text())
		if line == "" {
			continue
		}
		parts := strings.Fields(line)
		cmd := parts[0]
		switch cmd {
		case "exit":
			return
		case "add":
			fmt.Print("Название: ")
			scanner.Scan()
			title := strings.TrimSpace(scanner.Text())
			fmt.Print("URL: ")
			scanner.Scan()
			url := strings.TrimSpace(scanner.Text())
			fmt.Print("Теги (через запятую): ")
			scanner.Scan()
			tagsLine := strings.TrimSpace(scanner.Text())
			var tags []string
			if tagsLine != "" {
				for _, t := range strings.Split(tagsLine, ",") {
					t = strings.TrimSpace(t)
					if t != "" {
						tags = append(tags, t)
					}
				}
			}
			fmt.Print("Заметки (опционально): ")
			scanner.Scan()
			notes := strings.TrimSpace(scanner.Text())
			if title == "" || url == "" {
				printColor("Название и URL обязательны.", "red")
				continue
			}
			id := mgr.add(title, url, tags, notes)
			printColor(fmt.Sprintf("✅ Закладка добавлена (ID: %d)", id), "green")
		case "list":
			mgr.listAll()
		case "search":
			fmt.Print("Поиск: ")
			scanner.Scan()
			query := strings.TrimSpace(scanner.Text())
			results := mgr.search(query)
			if len(results) == 0 {
				printColor("Ничего не найдено.", "yellow")
			} else {
				for _, b := range results {
					fmt.Printf("ID %d: %s (%s) Теги: ", b.ID, b.Title, b.URL)
					if len(b.Tags) == 0 {
						fmt.Print("(без тегов)")
					} else {
						fmt.Print(strings.Join(b.Tags, ", "))
					}
					fmt.Println()
				}
			}
		case "delete":
			fmt.Print("ID закладки: ")
			scanner.Scan()
			idStr := strings.TrimSpace(scanner.Text())
			if id, err := strconv.Atoi(idStr); err == nil {
				mgr.delete(id)
				printColor(fmt.Sprintf("Закладка #%d удалена.", id), "green")
			} else {
				printColor("Введите число.", "red")
			}
		case "edit":
			fmt.Print("ID закладки: ")
			scanner.Scan()
			idStr := strings.TrimSpace(scanner.Text())
			id, err := strconv.Atoi(idStr)
			if err != nil {
				printColor("Введите число.", "red")
				continue
			}
			fmt.Print("Новое название (Enter для пропуска): ")
			scanner.Scan()
			title := strings.TrimSpace(scanner.Text())
			fmt.Print("Новый URL (Enter для пропуска): ")
			scanner.Scan()
			url := strings.TrimSpace(scanner.Text())
			fmt.Print("Новые теги (через запятую, Enter для пропуска): ")
			scanner.Scan()
			tagsLine := strings.TrimSpace(scanner.Text())
			var tags []string
			if tagsLine != "" {
				for _, t := range strings.Split(tagsLine, ",") {
					t = strings.TrimSpace(t)
					if t != "" {
						tags = append(tags, t)
					}
				}
			}
			fmt.Print("Новые заметки (Enter для пропуска): ")
			scanner.Scan()
			notes := strings.TrimSpace(scanner.Text())
			if mgr.edit(id, title, url, tags, notes) {
				printColor("Закладка обновлена.", "green")
			} else {
				printColor("Закладка не найдена.", "red")
			}
		case "tags":
			tags := mgr.getAllTags()
			if len(tags) == 0 {
				printColor("Тегов нет.", "yellow")
			} else {
				printColor("Все теги: ", "blue")
				fmt.Println(strings.Join(tags, ", "))
			}
		case "export":
			fmt.Print("Имя файла (по умолчанию export.json): ")
			scanner.Scan()
			fname := strings.TrimSpace(scanner.Text())
			if fname == "" {
				fname = "export.json"
			}
			mgr.export(fname)
		case "import":
			fmt.Print("Имя файла: ")
			scanner.Scan()
			fname := strings.TrimSpace(scanner.Text())
			if fname == "" {
				printColor("Укажите имя файла.", "red")
			} else {
				mgr.import_(fname)
			}
		default:
			printColor("Неизвестная команда.", "red")
		}
	}
}
