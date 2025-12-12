package com.example.personaldiary

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson

class EditActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var etNoteName: EditText
    private lateinit var etMessage: EditText
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button

    private lateinit var database: FirebaseDatabase

    private val FIREBASE_URL = "https://diary-ae3ea-default-rtdb.firebaseio.com/"
    private lateinit var collection: String
    private lateinit var firebaseKey: String
    private var isNew = true
    private var originalDisplayName = ""

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        // Находим элементы
        etNoteName = findViewById(R.id.etNoteName)
        etMessage = findViewById(R.id.etMessage)
        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)

        // Получаем данные из Intent
        collection = intent.getStringExtra("collection") ?: "dates"
        isNew = intent.getBooleanExtra("isNew", true)

        database = FirebaseDatabase.getInstance(FIREBASE_URL)

        if (isNew) {
            // Новая заметка
            etNoteName.setText("")
            etMessage.setText("")
            btnDelete.isEnabled = false
            btnDelete.alpha = 0.5f
        } else {
            // Редактирование существующей заметки
            firebaseKey = intent.getStringExtra("key") ?: ""
            originalDisplayName = intent.getStringExtra("displayName") ?: ""
            val text = intent.getStringExtra("text") ?: ""

            etNoteName.setText(originalDisplayName)
            etMessage.setText(text)
            btnDelete.isEnabled = true
        }

        // Назначаем обработчики
        btnSave.setOnClickListener { saveEntry() }
        btnDelete.setOnClickListener { deleteEntry() }
    }

    private fun saveEntry() {
        val displayName = etNoteName.text.toString().trim()
        val message = etMessage.text.toString().trim()

        if (displayName.isEmpty()) {
            Toast.makeText(this, "Введите название заметки", Toast.LENGTH_SHORT).show()
            return
        }

        if (message.isEmpty()) {
            Toast.makeText(this, "Введите текст заметки", Toast.LENGTH_SHORT).show()
            return
        }

        // Преобразуем красивое имя в ключ для Firebase
        val newFirebaseKey = convertToFirebaseKey(displayName)

        // Сериализуем текст в JSON
        val jsonMessage = Gson().toJson(message)

        if (isNew) {
            // Создание новой заметки
            database.reference.child(collection).child(newFirebaseKey)
                .setValue(jsonMessage)
                .addOnSuccessListener {
                    Toast.makeText(this, "Заметка создана!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Обновление существующей заметки
            if (newFirebaseKey != firebaseKey) {
                // Если название изменилось
                AlertDialog.Builder(this)
                    .setTitle("Переименование заметки")
                    .setMessage("Вы изменили название заметки. Создать новую заметку и удалить старую?")
                    .setPositiveButton("Да") { dialog, _ ->
                        // Создаем новую заметку
                        database.reference.child(collection).child(newFirebaseKey)
                            .setValue(jsonMessage)
                            .addOnSuccessListener {
                                // Удаляем старую
                                database.reference.child(collection).child(firebaseKey)
                                    .removeValue()
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Заметка обновлена!", Toast.LENGTH_SHORT).show()
                                        finish()
                                    }
                            }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Нет") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            } else {
                // Просто обновляем содержимое
                database.reference.child(collection).child(firebaseKey)
                    .setValue(jsonMessage)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Заметка обновлена!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun convertToFirebaseKey(displayName: String): String {
        var key = displayName
            .replace(".", "_")
            .replace("$", "_")
            .replace("#", "_")
            .replace("[", "_")
            .replace("]", "_")
            .replace("/", "_")

        key = key.replace(" ", "_")

        while (key.contains("__")) {
            key = key.replace("__", "_")
        }

        key = key.trim('_')

        if (key.isEmpty()) {
            key = "new_note"
        }

        return key
    }

    private fun deleteEntry() {
        if (!isNew) {
            AlertDialog.Builder(this)
                .setTitle("Удаление заметки")
                .setMessage("Удалить заметку '${originalDisplayName}'?")
                .setPositiveButton("Удалить") { dialog, _ ->
                    database.reference.child(collection).child(firebaseKey)
                        .removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Заметка удалена", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
}