package com.fneb.piibiocampus.ui.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.dao.NewsDao
import com.fneb.piibiocampus.data.model.ItemNews
import com.fneb.piibiocampus.ui.BaseActivity
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.fneb.piibiocampus.utils.showTopBarLeftButton
import com.squareup.picasso.Picasso

class NewsListAdminActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView

    private lateinit var itemsStatic: List<ItemNews>
    private lateinit var itemsDynamic: List<ItemNews>
    private lateinit var features : List<ImageView>
    private lateinit var adapter: ItemNewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_list_admin)
        setTopBarTitle("Actualité")
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

        loadDynamicNews()

        btnAddNews.setOnClickListener {
            val intent = Intent(this, UpdateNewsAdminActivity::class.java)
            intent.putExtra("order", itemsDynamic.size+1)
            intent.putExtra("behavior", "dynamic")
            intent.putExtra("status", "create")
            this.startActivity(intent)
        }
        val featuredMain = findViewById<ImageView>(R.id.featuredMain)
        val featuredLeft = findViewById<ImageView>(R.id.featuredLeft)
        val featuredRight = findViewById<ImageView>(R.id.featuredRight)

        features = listOf(featuredMain,featuredLeft,featuredRight)
        loadStaticNews(features)

        featuredMain.setOnClickListener {
            createNewsStatic(1)
        }
        featuredLeft.setOnClickListener {
            createNewsStatic(2)
        }
        featuredRight.setOnClickListener {
            createNewsStatic(3)
        }
    }

    private fun createNewsStatic(id : Int){
        val item = itemsStatic.firstOrNull { it.order?.toInt() == id }
        if (item == null){
            val intent = Intent(this, UpdateNewsAdminActivity::class.java)
            intent.putExtra("behavior", "static")
            intent.putExtra("order", id)
            intent.putExtra("status", "create")
            this.startActivity(intent)
        }else{
            val intent = Intent(this, UpdateNewsAdminActivity::class.java)
            Log.d("DEBUG_ID", "ID item = ${item.id}")
            intent.putExtra("id", item.id)
            intent.putExtra("title", item.titre)
            intent.putExtra("imageUrl", item.imageUrl)
            intent.putExtra("source", item.source)
            intent.putExtra("behavior", item.behavior)
            intent.putExtra("order", item.order)
            intent.putExtra("status", "update")

            startActivity(intent)
        }
    }

    private fun loadDynamicNews() {
        NewsDao.getDynamicNews(
            onSuccess = { items ->
                itemsDynamic = items
                adapter = ItemNewsAdapter(items)
                recyclerView.adapter = adapter
            },
            onError = { exception ->
                Toast.makeText(this, "Erreur : ${exception.message}", Toast.LENGTH_SHORT).show()
                //Log.d("Test","Erreur : ${exception.message}")
            }
        )
    }

    private fun loadStaticNews(features : List<ImageView> ){
        NewsDao.getStaticNews(
            onSuccess = { items ->
                itemsStatic = items
                for (i in 0..items.size - 1){
                    val item = items[i]
                    val order = item.order?.toInt()
                    Picasso.get()
                        .load(item.imageUrl)
                        .into(features[order?.minus(1) ?: 0])
                }
            },
            onError = { exception ->
                Toast.makeText(this, "Erreur : ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.d("Test","Erreur : ${exception.message}")
            }
        )
    }

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
            Picasso.get()
                .load(item.imageUrl)
                .into(holder.image)

            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, UpdateNewsAdminActivity::class.java)
                Log.d("DEBUG_ID", "ID item = ${item.id}")
                intent.putExtra("id", item.id)
                intent.putExtra("title", item.titre)
                intent.putExtra("imageUrl", item.imageUrl)
                intent.putExtra("source", item.source)
                intent.putExtra("behavior", item.behavior)
                intent.putExtra("order", item.order)
                intent.putExtra("status", "update")

                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount() = items.size
    }

    override fun onResume() {
        super.onResume()
        setTopBarTitle(R.string.actualite)
        loadDynamicNews()
        for (f in features){
            f.setImageResource(R.drawable.ic_placeholder_image)
        }
        loadStaticNews(features)
    }
}