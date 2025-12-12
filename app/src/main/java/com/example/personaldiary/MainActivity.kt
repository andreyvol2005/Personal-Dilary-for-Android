package com.example.personaldiary

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var btnDates: Button
    private lateinit var btnNotes: Button
    private lateinit var btnNew: Button
    private lateinit var recyclerViewEntries: RecyclerView
    private lateinit var tvCollectionInfo: TextView

    private lateinit var database: FirebaseDatabase
    private lateinit var databaseRef: DatabaseReference

    private val FIREBASE_URL = "https://diary-ae3ea-default-rtdb.firebaseio.com/"
    private val entries = mutableMapOf<String, Entry>() // key -> Entry object
    private var currentCollection = "dates" // текущая коллекция

    private lateinit var entriesAdapter: EntriesAdapter

    // Класс для хранения данных о записи
    data class Entry(
        val key: String,
        val displayName: String,
        val text: String
    )

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Находим элементы
        btnDates = findViewById(R.id.btnDates)
        btnNotes = findViewById(R.id.btnNotes)
        btnNew = findViewById(R.id.btnNew)
        recyclerViewEntries = findViewById(R.id.recyclerViewEntries)

        // Настройка RecyclerView
        recyclerViewEntries.layoutManager = LinearLayoutManager(this)
        entriesAdapter = EntriesAdapter { position -> openEditActivity(position) }
        recyclerViewEntries.adapter = entriesAdapter

        // Назначаем обработчики
        btnDates.setOnClickListener { switchToDates() }
        btnNotes.setOnClickListener { switchToNotes() }
        btnNew.setOnClickListener { createNewEntry() }

        // Автоматическое подключение к Firebase
        database = FirebaseDatabase.getInstance(FIREBASE_URL)
        databaseRef = database.reference

        // Загружаем заметки при запуске (по умолчанию dates)
        loadEntries()
    }

    private fun switchToDates() {
        if (currentCollection != "dates") {
            currentCollection = "dates"
            loadEntries()
            updateCollectionInfo()
        }
    }

    private fun switchToNotes() {
        if (currentCollection != "notes") {
            currentCollection = "notes"
            loadEntries()
            updateCollectionInfo()
        }
    }

    private fun updateCollectionInfo() {
        val collectionName = when (currentCollection) {
            "dates" -> "Дневник"
            "notes" -> "Заметки"
            else -> currentCollection
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadEntries() {
        databaseRef.child(currentCollection).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                entries.clear()
                val entryList = mutableListOf<Entry>()

                for (entry in snapshot.children) {
                    val entryKey = entry.key ?: continue
                    val entryText = entry.getValue(String::class.java) ?: ""

                    // Очищаем JSON строку
                    val cleanedText = cleanJsonString(entryText)

                    // Создаем отображаемое имя
                    val displayName = entryKey.replace("_", " ")

                    val entryObject = Entry(entryKey, displayName, cleanedText)
                    entries[entryKey] = entryObject
                    entryList.add(entryObject)
                }

                // Сортируем по алфавиту
                entryList.sortBy { it.displayName }

                // Обновляем адаптер
                entriesAdapter.submitList(entryList)

                // Отображаем подсказку если нет заметок
                if (entryList.isEmpty()) {
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
        val entryList = entriesAdapter.currentList
        if (position in entryList.indices) {
            val entry = entryList[position]

            val intent = Intent(this, EditActivity::class.java)
            intent.putExtra("collection", currentCollection)
            intent.putExtra("key", entry.key)
            intent.putExtra("displayName", entry.displayName)
            intent.putExtra("text", entry.text)
            intent.putExtra("isNew", false)
            startActivityForResult(intent, 1)
        }
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