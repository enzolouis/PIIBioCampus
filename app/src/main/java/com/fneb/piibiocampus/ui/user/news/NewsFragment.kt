package com.fneb.piibiocampus.ui.user.news

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.model.ItemNews
import com.fneb.piibiocampus.data.ui.UiState
import com.fneb.piibiocampus.data.ui.showError
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.squareup.picasso.Picasso

class NewsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var features: List<ImageView>
    private lateinit var adapter: ItemNewsAdapter

    private val viewModel: NewsFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTopBarTitle(R.string.actualite)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
        recyclerView.isNestedScrollingEnabled = false // Important pour le NestedScrollView

        features = listOf(
            view.findViewById(R.id.featuredMain),
            view.findViewById(R.id.featuredLeft),
            view.findViewById(R.id.featuredRight)
        )

        observeViewModel()
    }

    private fun observeViewModel() {
        // État global
        viewModel.loadState.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Error) {
                showError(state.exception)
            }
        }

        // dynamic news
        viewModel.dynamicNews.observe(viewLifecycleOwner) { items ->
            adapter = ItemNewsAdapter(items)
            recyclerView.adapter = adapter
        }

        // static news
        viewModel.staticNews.observe(viewLifecycleOwner) { items ->
            // Reset placeholders
            features.forEach { it.setImageResource(R.drawable.ic_placeholder_image) }

            items.forEach { item ->
                val order = item.order?.toInt() ?: return@forEach
                val targetImageView = features.getOrNull(order - 1)

                if (targetImageView != null && !item.imageUrl.isNullOrEmpty()) {
                    Picasso.get().load(item.imageUrl).into(targetImageView)

                    targetImageView.setOnClickListener {
                        Log.d("Test",item.source)
                        openUrl(item.source)
                    }
                }
            }
        }
    }

    private fun openUrl(url: String?) {
        if (url.isNullOrEmpty()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            // pas de navigateur ?
        }
    }

    override fun onResume() {
        super.onResume()
        setTopBarTitle(R.string.actualite)
        viewModel.loadNews()
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    inner class ItemNewsAdapter(private val items: List<ItemNews>) :
        RecyclerView.Adapter<ItemNewsAdapter.ItemNewsViewHolder>() {

        inner class ItemNewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.title)
            val image: ImageView = view.findViewById(R.id.image)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemNewsViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_news, parent, false)
            return ItemNewsViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemNewsViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = item.titre

            Picasso.get()
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_placeholder_image)
                .into(holder.image)

            holder.itemView.setOnClickListener { openUrl(item.source) }
        }

        override fun getItemCount() = items.size
    }
}