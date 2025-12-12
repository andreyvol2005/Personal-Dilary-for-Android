package com.example.personaldiary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class EntriesAdapter(
    private val onItemClick: (Int) -> Unit
) : ListAdapter<MainActivity.Entry, EntriesAdapter.ViewHolder>(EntryDiffCallback()) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val cardView: MaterialCardView = view.findViewById(R.id.cardView)
        private val titleTextView: TextView = view.findViewById(R.id.item_title)
        private val dateTextView: TextView = view.findViewById(R.id.item_date)
        private val previewTextView: TextView = view.findViewById(R.id.item_preview)

        init {
            cardView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(adapterPosition)
                }
            }
        }

        fun bind(entry: MainActivity.Entry) {
            titleTextView.text = entry.displayName

            // Преобразуем ключ в дату, если это дневник
            val dateText = if (entry.key.matches(Regex("\\d{4}_\\d{2}_\\d{2}"))) {
                formatDateFromKey(entry.key)
            } else {
                // Для заметок показываем превью текста
                ""
            }

            dateTextView.text = dateText

            // Превью текста (первые 50 символов)
            val preview = if (entry.text.length > 50) {
                entry.text.substring(0, 50) + "..."
            } else {
                entry.text
            }
            previewTextView.text = preview

            // Скрываем dateTextView если нет даты
            dateTextView.visibility = if (dateText.isNotEmpty()) View.VISIBLE else View.GONE
        }

        private fun formatDateFromKey(key: String): String {
            return try {
                val parts = key.split("_")
                if (parts.size == 3) {
                    val year = parts[0].toInt()
                    val month = parts[1].toInt()
                    val day = parts[2].toInt()
                    val monthNames = arrayOf(
                        "января", "февраля", "марта", "апреля", "мая", "июня",
                        "июля", "августа", "сентября", "октября", "ноября", "декабря"
                    )
                    "$day ${monthNames.getOrNull(month - 1) ?: month} $year"
                } else {
                    key.replace("_", " ")
                }
            } catch (e: Exception) {
                key.replace("_", " ")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class EntryDiffCallback : DiffUtil.ItemCallback<MainActivity.Entry>() {
    override fun areItemsTheSame(oldItem: MainActivity.Entry, newItem: MainActivity.Entry): Boolean {
        return oldItem.key == newItem.key
    }

    override fun areContentsTheSame(oldItem: MainActivity.Entry, newItem: MainActivity.Entry): Boolean {
        return oldItem == newItem
    }
}