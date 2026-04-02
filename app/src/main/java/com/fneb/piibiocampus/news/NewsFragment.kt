package com.fneb.piibiocampus.news

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.dao.NewsDao
import com.fneb.piibiocampus.data.model.ItemNews
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.squareup.picasso.Picasso

class NewsFragment : Fragment()  {

    private lateinit var recyclerView: RecyclerView

    private val photos = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: ItemNewsAdapter
    private val viewModel: NewsFragmentViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTopBarTitle(R.string.actualite);

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
        recyclerView.clipToPadding = false

        NewsDao.getAllNews(
            onSuccess = { items ->
                adapter = ItemNewsAdapter(items)
                recyclerView.adapter = adapter
            },
            onError = { exception ->
                Toast.makeText(requireContext(), "Erreur : ${exception.message}", Toast.LENGTH_SHORT).show()
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
                .inflate(R.layout.item_news, parent, false)
            return ItemNewsViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemNewsViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = item.titre
            Picasso.get()
                .load(item.imageUrl)
                .into(holder.image)

            holder.itemView.setOnClickListener {

                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(item.source)

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