package com.example.note.presentation.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.note.R
import com.example.note.databinding.RecyclerviewBinding
import com.example.note.presentation.create.model.NoteModel

class NotesPagingAdapter : PagingDataAdapter<NoteModel, NotesPagingAdapter.Holder>(DiffCallback) {

    interface EditClick { fun editClick(noteModel: NoteModel) }
    interface DeleteClick { fun deleteClick(noteModel: NoteModel) }

    var editClick: EditClick? = null
    var deleteClick: DeleteClick? = null

    init { setHasStableIds(true) }

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<NoteModel>() {
            override fun areItemsTheSame(oldItem: NoteModel, newItem: NoteModel) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: NoteModel, newItem: NoteModel) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = RecyclerviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        if (item == null) {
            holder.bindPlaceholder()
        } else {
            holder.bind(item, editClick, deleteClick)
        }
    }

    class Holder(private val binding: RecyclerviewBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindPlaceholder() {
            // 플레이스홀더(스켈레톤) 필요하면 여기서 처리
            binding.title.text = " "
            binding.content.text = " "
            binding.summarize.text = " "
            binding.ivEdit.isVisible = false
            binding.ivDelete.isVisible = false
        }

        fun bind(item: NoteModel, editClick: EditClick?, deleteClick: DeleteClick?) {
            binding.title.text = item.title
            binding.content.text = item.content
            binding.summarize.text = item.summarize

            // 토글: 요약/본문 + 액션 아이콘
            binding.root.setOnClickListener {
                val showContent = binding.content.visibility != View.VISIBLE
                binding.content.isVisible = showContent
                binding.ivEdit.isVisible = showContent
                binding.ivDelete.isVisible = showContent
                binding.summarize.isVisible = !showContent
            }

            binding.ivEdit.setOnClickListener { editClick?.editClick(item) }
            binding.ivDelete.setOnClickListener { deleteClick?.deleteClick(item) }

            // 감정 점수 → 배경색
            val score = item.sentiment ?: 0.0
            val colorRes = when {
                score == 0.0 -> R.color.sentiment_neutral
                score in 0.8..1.0 -> R.color.sentiment_very_positive
                score in 0.6..0.8 -> R.color.sentiment_positive
                score in 0.4..0.6 -> R.color.sentiment_neutral
                score in 0.2..0.4 -> R.color.sentiment_negative
                score in 0.0..0.2 -> R.color.sentiment_very_negative
                else -> R.color.sentiment_neutral
            }
            binding.root.setBackgroundResource(colorRes)
        }
    }
}
