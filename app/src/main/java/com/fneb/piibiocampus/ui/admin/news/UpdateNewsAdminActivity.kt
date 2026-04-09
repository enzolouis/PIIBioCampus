package com.fneb.piibiocampus.ui.admin.news

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.ui.UiState
import com.fneb.piibiocampus.data.ui.showError
import com.fneb.piibiocampus.ui.BaseActivity
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.fneb.piibiocampus.utils.showTopBarLeftButton
import com.squareup.picasso.Picasso

class UpdateNewsAdminActivity : BaseActivity() {

    private lateinit var viewModel: UpdateNewsAdminViewModel

    private lateinit var imageView: ImageView
    private lateinit var titleView: EditText
    private lateinit var sourceView: EditText
    private lateinit var btnValidateUpdate: Button
    private lateinit var btnChangeImage: Button
    private lateinit var btnSuppr: Button

    // On garde l'Uri localement pour l'affichage immédiat avant l'envoi
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imageView.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(UpdateNewsAdminViewModel::class.java)
        setContentView(R.layout.activity_update_news_admin)

        bindViews()
        initViewModelData()
        setupUIBasedOnStatus()
        setupListeners()
        observeViewModel()
    }

    private fun bindViews() {
        btnValidateUpdate = findViewById(R.id.btnValideUpdate)
        btnChangeImage = findViewById(R.id.btnChangeImage)
        btnSuppr = findViewById(R.id.btnSuppr)
        titleView = findViewById(R.id.titleNews)
        sourceView = findViewById(R.id.sourceNews)
        imageView = findViewById(R.id.pictureNews)
    }

    private fun initViewModelData() {
        viewModel.status = intent.getStringExtra("status")
        viewModel.behavior = intent.getStringExtra("behavior")
        viewModel.order = intent.getIntExtra("order", 0)

        if (viewModel.status == "update") {
            viewModel.newsId = intent.getStringExtra("id") ?: ""
            viewModel.currentImageUrl = intent.getStringExtra("imageUrl") ?: ""
        }
    }

    private fun setupUIBasedOnStatus() {
        if (viewModel.status == "update") {
            setTopBarTitle("Modifier une Actu'")
            showTopBarLeftButton { finish() }
            btnValidateUpdate.text = "Appliquer"
            btnChangeImage.text = "Changer l'image"

            titleView.setText(intent.getStringExtra("title"))
            sourceView.setText(intent.getStringExtra("source"))

            if (viewModel.currentImageUrl.isNotEmpty()) {
                Picasso.get()
                    .load(viewModel.currentImageUrl)
                    .into(imageView)
            }
        } else {
            setTopBarTitle("Créer une Actu'")
            showTopBarLeftButton { finish() }
            btnValidateUpdate.text = "Créer"
            btnChangeImage.text = "Choisir une image"

            btnSuppr.visibility = View.GONE
            val params = btnValidateUpdate.layoutParams as ViewGroup.MarginLayoutParams
            params.marginStart = dpToPx(24)
            btnValidateUpdate.layoutParams = params
        }
    }

    private fun setupListeners() {
        btnChangeImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSuppr.setOnClickListener {
            viewModel.deleteNews()
        }

        btnValidateUpdate.setOnClickListener {
            val title = titleView.text.toString()
            val source = sourceView.text.toString()
            viewModel.saveNews(this, title, source, selectedImageUri)
        }
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.actionState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    setLoadingState(true)
                }
                is UiState.Success -> {
                    setLoadingState(false)
                    // Affiche le message de succès remonté par le ViewModel
                    Toast.makeText(this, state.data, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is UiState.Error -> {
                    setLoadingState(false)
                    showError(state.exception)
                }
                else -> Unit
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        btnValidateUpdate.isEnabled = !isLoading
        btnSuppr.isEnabled = !isLoading
        btnChangeImage.isEnabled = !isLoading
        // Optionnel : Vous pouvez aussi afficher un ProgressBar central ici
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}