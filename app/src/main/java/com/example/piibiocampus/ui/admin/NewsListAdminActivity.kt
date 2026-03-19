package com.example.piibiocampus.ui.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piibiocampus.R
import com.example.piibiocampus.data.dao.NewsDao
import com.example.piibiocampus.data.model.ItemNews
import com.example.piibiocampus.news.NewsFragment
import com.example.piibiocampus.utils.setTopBarTitle
import com.squareup.picasso.Picasso

class NewsListAdminActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    private val photos = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: ItemNewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_list_admin)
        setTopBarTitle("Actualité")
        val btnAddNews = findViewById<Button>(R.id.btnAddNews)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 1)

        NewsDao.getAllNews(
        onSuccess = { items ->
            adapter = ItemNewsAdapter(items)
            recyclerView.adapter = adapter
        },
        onError = { exception ->
            Toast.makeText(this, "Erreur : ${exception.message}", Toast.LENGTH_SHORT).show()
        }
        )

        btnAddNews.setOnClickListener {
            val intent = Intent(this, UpdateNewsAdminActivity::class.java)
            intent.putExtra("status", "create")
            this.startActivity(intent)
        }
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
            // signifie que c'est une modif et pas une nouvelle actu (car meme page)
            intent.putExtra("status", "update")

            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = items.size
}

override fun onResume() {
    super.onResume()
    setTopBarTitle(R.string.actualite)
}
}