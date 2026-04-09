package com.fneb.piibiocampus.ui.admin.news

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.model.ItemNews
import com.fneb.piibiocampus.data.ui.UiState
import com.fneb.piibiocampus.data.ui.showError
import com.fneb.piibiocampus.ui.BaseActivity
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.fneb.piibiocampus.utils.showTopBarLeftButton
import com.squareup.picasso.Picasso

class NewsListAdminActivity : BaseActivity() {

    private val viewModel: NewsListAdminViewModel by viewModels { NewsListAdminViewModelFactory() }

    private lateinit var recyclerView: RecyclerView
    private lateinit var features: List<ImageView>
    private lateinit var adapter: ItemNewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_list_admin)

        setTopBarTitle(R.string.actualite) // Remplacé par la ressource String directement
        showTopBarLeftButton { finish() }

        val btnAddNews = findViewById<Button>(R.id.btnAddNews)
        ViewCompat.setOnApplyWindowInsetsListener(btnAddNews) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraPadding = (20 * resources.displayMetrics.density).toInt()
            (view.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                systemBars.bottom + extraPadding
            insets
        }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 1)

        val featuredMain = findViewById<ImageView>(R.id.featuredMain)
        val featuredLeft = findViewById<ImageView>(R.id.featuredLeft)
        val featuredRight = findViewById<ImageView>(R.id.featuredRight)
        features = listOf(featuredMain, featuredLeft, featuredRight)

        setupListeners(btnAddNews, featuredMain, featuredLeft, featuredRight)
        observeViewModel()
    }

    private fun setupListeners(
        btnAddNews: Button,
        featuredMain: ImageView,
        featuredLeft: ImageView,
        featuredRight: ImageView
    ) {
        btnAddNews.setOnClickListener {
            // On récupère la taille actuelle via le ViewModel pour l'ordre
            val currentSize = viewModel.dynamicNews.value?.size ?: 0
            val intent = Intent(this, UpdateNewsAdminActivity::class.java).apply {
                putExtra("order", currentSize + 1)
                putExtra("behavior", "dynamic")
                putExtra("status", "create")
            }
            startActivity(intent)
        }

        featuredMain.setOnClickListener { createNewsStatic(1) }
        featuredLeft.setOnClickListener { createNewsStatic(2) }
        featuredRight.setOnClickListener { createNewsStatic(3) }
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.loadState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    // Optionnel : afficher un loader si vous en avez un (ex: progressBar.visibility = View.VISIBLE)
                }
                is UiState.Success -> {
                    // Optionnel : cacher le loader
                }
                is UiState.Error -> {
                    // C'est ici que l'on gère l'erreur Firebase comme dans ExportDataActivity
                    showError(state.exception)
                }
                else -> Unit
            }
        }

        // Observation des actualités dynamiques
        viewModel.dynamicNews.observe(this) { items ->
            adapter = ItemNewsAdapter(items)
            recyclerView.adapter = adapter
        }

        // Observation des actualités statiques
        viewModel.staticNews.observe(this) { items ->
            // On remet les placeholders avant de charger
            for (f in features) {
                f.setImageResource(R.drawable.ic_placeholder_image)
            }
            // On assigne les images selon l'ordre
            for (item in items) {
                val order = item.order?.toInt() ?: continue
                val targetImageView = features.getOrNull(order - 1)

                if (targetImageView != null && !item.imageUrl.isNullOrEmpty()) {
                    Picasso.get()
                        .load(item.imageUrl)
                        .into(targetImageView)
                }
            }
        }
    }

    private fun createNewsStatic(id: Int) {
        // On cherche dans les données du ViewModel si la news statique existe déjà
        val itemsStatic = viewModel.staticNews.value ?: emptyList()
        val item = itemsStatic.firstOrNull { it.order?.toInt() == id }

        val intent = Intent(this, UpdateNewsAdminActivity::class.java)

        if (item == null) {
            intent.apply {
                putExtra("behavior", "static")
                putExtra("order", id)
                putExtra("status", "create")
            }
        } else {
            Log.d("DEBUG_ID", "ID item = ${item.id}")
            intent.apply {
                putExtra("id", item.id)
                putExtra("title", item.titre)
                putExtra("imageUrl", item.imageUrl)
                putExtra("source", item.source)
                putExtra("behavior", item.behavior)
                putExtra("order", item.order)
                putExtra("status", "update")
            }
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        setTopBarTitle(R.string.actualite)
        // On demande au ViewModel de tout recharger
        viewModel.loadNews()
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    inner class ItemNewsAdapter(
        private val items: List<ItemNews>
    ) : RecyclerView.Adapter<ItemNewsAdapter.ItemNewsViewHolder>() {

        inner class ItemNewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.title)
            val image: ImageView = view.findViewById(R.id.image)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemNewsViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_news_admin, parent, false)
            return ItemNewsViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemNewsViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = item.titre

            if (!item.imageUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(item.imageUrl)
                    .into(holder.image)
            }

            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, UpdateNewsAdminActivity::class.java).apply {
                    Log.d("DEBUG_ID", "ID item = ${item.id}")
                    putExtra("id", item.id)
                    putExtra("title", item.titre)
                    putExtra("imageUrl", item.imageUrl)
                    putExtra("source", item.source)
                    putExtra("behavior", item.behavior)
                    putExtra("order", item.order)
                    putExtra("status", "update")
                }
                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount() = items.size
    }
}