package com.example.personaldiary

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var btnDates: Button
    private lateinit var btnNotes: Button
    private lateinit var btnNew: Button
    private lateinit var listViewEntries: ListView
    private lateinit var tvCollectionInfo: TextView

    private lateinit var database: FirebaseDatabase
    private lateinit var databaseRef: DatabaseReference

    private val FIREBASE_URL = "https://diary-ae3ea-default-rtdb.firebaseio.com/"
    private val entries = mutableMapOf<String, String>() // key -> text
    private var currentCollection = "dates" // текущая коллекция

    private lateinit var entriesAdapter: ArrayAdapter<String>

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Находим элементы
        btnDates = findViewById(R.id.btnDates)
        btnNotes = findViewById(R.id.btnNotes)
        btnNew = findViewById(R.id.btnNew)
        listViewEntries = findViewById(R.id.recyclerViewEntries)

        // Инициализация адаптера
        entriesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        listViewEntries.adapter = entriesAdapter

        // Назначаем обработчики
        btnDates.setOnClickListener { switchToDates() }
        btnNotes.setOnClickListener { switchToNotes() }
        btnNew.setOnClickListener { createNewEntry() }

        listViewEntries.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            openEditActivity(position)
        }

        // Автоматическое подключение к Firebase
        database = FirebaseDatabase.getInstance(FIREBASE_URL)
        databaseRef = database.reference

        // Загружаем заметки при запуске (по умолчанию dates)
        loadEntries()
        highlightDatesButton()
    }

    private fun switchToDates() {
        if (currentCollection != "dates") {
            currentCollection = "dates"
            loadEntries()
            highlightDatesButton()
        }
    }

    private fun switchToNotes() {
        if (currentCollection != "notes") {
            currentCollection = "notes"
            loadEntries()
            highlightNotesButton()
        }
    }

    private fun highlightDatesButton() {
        btnDates.setBackgroundColor(getColor(android.R.color.white))
        btnDates.setTextColor(getColor(R.color.colorPrimary))
        btnDates.text = "📅 Дневник (активно)"

        btnNotes.setBackgroundColor(getColor(R.color.colorPrimaryLight))
        btnNotes.setTextColor(getColor(android.R.color.white))
        btnNotes.text = "📝 Заметки"
    }

    private fun highlightNotesButton() {
        btnNotes.setBackgroundColor(getColor(android.R.color.white))
        btnNotes.setTextColor(getColor(R.color.colorPrimary))
        btnNotes.text = "📝 Заметки (активно)"

        btnDates.setBackgroundColor(getColor(R.color.colorPrimaryLight))
        btnDates.setTextColor(getColor(android.R.color.white))
        btnDates.text = "📅 Дневник"
    }

    private fun loadEntries() {
        databaseRef.child(currentCollection).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                entries.clear()
                entriesAdapter.clear()

                for (entry in snapshot.children) {
                    val entryKey = entry.key ?: continue
                    val entryText = entry.getValue(String::class.java) ?: ""

                    // Очищаем JSON строку
                    val cleanedText = cleanJsonString(entryText)

                    entries[entryKey] = cleanedText

                    // Отображаем красивое имя (без _)
                    val displayName = entryKey.replace("_", " ")
                    entriesAdapter.add(displayName)
                }

                // Сортируем по алфавиту
                entriesAdapter.sort { o1, o2 -> o1.compareTo(o2, true) }

                // Отображаем подсказку если нет заметок
                if (entriesAdapter.isEmpty) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity,
                            "Нет заметок. Нажмите 'Новая заметка' для создания.",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity,
                    "Ошибка загрузки: ${error.message}",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun cleanJsonString(input: String): String {
        if (input.isEmpty()) return input

        var result = input
        if (result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length - 1)
        }

        result = result.replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")

        return result
    }

    private fun openEditActivity(position: Int) {
        if (position >= 0 && position < entriesAdapter.count) {
            val displayName = entriesAdapter.getItem(position) ?: ""

            // Находим реальный ключ в Firebase
            val firebaseKey = findFirebaseKeyByDisplayName(displayName)

            if (firebaseKey != null) {
                val intent = Intent(this, EditActivity::class.java)
                intent.putExtra("collection", currentCollection)
                intent.putExtra("key", firebaseKey)
                intent.putExtra("displayName", displayName)
                intent.putExtra("text", entries[firebaseKey] ?: "")
                intent.putExtra("isNew", false)
                startActivityForResult(intent, 1)
            }
        }
    }

    private fun findFirebaseKeyByDisplayName(displayName: String): String? {
        val firebaseKey = displayName.replace(" ", "_")

        // Ищем точное совпадение
        if (entries.containsKey(firebaseKey)) {
            return firebaseKey
        }

        // Ищем похожие ключи
        for (key in entries.keys) {
            if (key.replace("_", " ") == displayName) {
                return key
            }
        }

        return null
    }

    private fun createNewEntry() {
        val intent = Intent(this, EditActivity::class.java)
        intent.putExtra("collection", currentCollection)
        intent.putExtra("isNew", true)
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            // Обновляем список после возврата из EditActivity
            loadEntries()
        }
    }
}