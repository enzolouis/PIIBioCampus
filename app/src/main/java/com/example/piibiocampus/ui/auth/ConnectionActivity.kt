package com.example.piibiocampus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.piibiocampus.R
import com.example.piibiocampus.ui.MainActivity
import com.example.piibiocampus.ui.admin.DashboardAdminActivity
import com.example.piibiocampus.utils.Extensions.toast
import com.example.piibiocampus.utils.Validators
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ConnectionActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var pseudoZone: EditText
    private lateinit var passwordZone: EditText
    private lateinit var connectBtn: Button
    private lateinit var createAccountBtn: TextView
    private lateinit var resetPassWordBtn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connexion)

        pseudoZone = findViewById(R.id.txtIdentifiant)
        passwordZone = findViewById(R.id.txtMdp)
        connectBtn = findViewById(R.id.btnConnexion)
        createAccountBtn = findViewById(R.id.btnAlreadyAccount)
        resetPassWordBtn = findViewById(R.id.btnResetPassWord)

        createAccountBtn.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }

        resetPassWordBtn.setOnClickListener {
            startActivity(Intent(this, ResetPassWordActivity::class.java))
        }

        connectBtn.setOnClickListener {
            val email = pseudoZone.text.toString().trim()
            val password = passwordZone.text.toString().trim()

            if (!Validators.areEmailAndPasswordValid(email, password)) {
                toast("Veuillez remplir tous les champs")
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }
        viewModel.checkCurrentUserAndFetchRoleIfNeeded()
        // Observe le state flow
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {

                        is AuthUiState.Authenticated -> {
                            when (state.role) {
                                "USER" -> startActivity(
                                    Intent(
                                        this@ConnectionActivity,
                                        MainActivity::class.java
                                    )
                                )

                                "ADMIN", "SUPER_ADMIN" -> startActivity(
                                    Intent(
                                        this@ConnectionActivity,
                                        DashboardAdminActivity::class.java
                                    )
                                )

                                else -> toast("Rôle inconnu : ${state.role}")
                            }
                            finish()
                        }

                        is AuthUiState.Error -> {
                            toast(
                                "Erreur de connexion : ${state.throwable.message}",
                                android.widget.Toast.LENGTH_LONG
                            )
                        }

                        else -> Unit
                    }
                }
            }
        }

        val db = FirebaseFirestore.getInstance()

// Structure des données avec famille comme liste de maps
        val censusData = hashMapOf(
            "ordre" to listOf(
                hashMapOf(
                    "name" to "Passeriformes",
                    "description" to listOf("Ordre des passereaux"),
                    "id" to "order1",
                    "urlImage" to "",
                    "famille" to listOf(
                        hashMapOf(
                            "name" to "Corvidae",
                            "description" to listOf("Plume sur le bc", "Pattes robustes"),
                            "id" to "family1",
                            "urlImage" to "",
                            "genre" to listOf(
                                hashMapOf(
                                    "name" to "Corvus",
                                    "description" to listOf(
                                        "Patte couverte d'écaille à l'avant et à l'arrière",
                                        "Plumes raides",
                                        "Plumes s'étendant jusqu'au narine"
                                    ),
                                    "id" to "TdR6K0ByJvrQTNyumfgp",
                                    "urlImage" to "",
                                    "espece" to listOf(
                                        hashMapOf(
                                            "name" to "Corvus frugilegus",
                                            "description" to listOf(
                                                "Petite taille (environ 50cm)",
                                                "Plumage entièrement noir avec teinte métallique",
                                                "Bec prononcé avec bases dénudées"
                                            ),
                                            "id" to "70MjkBg28v6gZXalgGtS",
                                            "urlImage" to ""
                                        ),
                                        hashMapOf(
                                            "name" to "Corvus corone",
                                            "description" to listOf(
                                                "Petite taille (environ 50cm)",
                                                "Plumage entièrement noir",
                                                "Bec noir"
                                            ),
                                            "id" to "7HTW1odLktPXjt4PWI6W",
                                            "urlImage" to ""
                                        ),
                                        hashMapOf(
                                            "name" to "Corvus cornix",
                                            "description" to listOf(
                                                "Petite taille (environ 50cm)",
                                                "Plumage noir et cendrée",
                                                "Bec noir"
                                            ),
                                            "id" to "iaYPQWGPzO8cdG8GyjCO",
                                            "urlImage" to ""
                                        ),
                                        hashMapOf(
                                            "name" to "Corvus corax",
                                            "description" to listOf(
                                                "Très grande taille (environ 130cm)",
                                                "Plumage entièrement noir avec teinte bleu ou violette",
                                                "Bec fort, noir et pointu"
                                            ),
                                            "id" to "wyVP76KPeIZL44hqYvoD",
                                            "urlImage" to ""
                                        )
                                    )
                                ),
                                hashMapOf(
                                    "name" to "Pyrrhocorax",
                                    "description" to listOf(
                                        "Plumage noir avec patte de couleur vive",
                                        "Aile longue et large",
                                        "Zone montagneuse",
                                        "Bec incurvé vers le bas"
                                    ),
                                    "id" to "FJDlr55CCwOHAdFm9Qf1",
                                    "urlImage" to "",
                                    "espece" to listOf(
                                        hashMapOf(
                                            "name" to "Pyrrhocorax graculus",
                                            "description" to listOf("Bec couleur jaune"),
                                            "id" to "RcK3nFZZuTHFa61BaPYp",
                                            "urlImage" to ""
                                        ),
                                        hashMapOf(
                                            "name" to "Pyrrhocorax pyrrhocorax",
                                            "description" to listOf("Bec couleur vermillon (rouge-orange)"),
                                            "id" to "ZRprlMkLsilrmOFZCvjF",
                                            "urlImage" to ""
                                        )
                                    )
                                ),
                                hashMapOf(
                                    "name" to "Oriolus",
                                    "description" to listOf(
                                        "Plumage des mâles orné de couleurs brillantes jaune",
                                        "Plumage des femelles plus sombre, gris olivâtres et rayées sur le ventre"
                                    ),
                                    "id" to "L6E9eRxBB3dJMie5atOU",
                                    "urlImage" to "",
                                    "espece" to listOf(
                                        hashMapOf(
                                            "name" to "Oriolus oriolus",
                                            "description" to emptyList<String>(),
                                            "id" to "JzslNl0vXXClQGB9Rdy4",
                                            "urlImage" to ""
                                        )
                                    )
                                ),
                                hashMapOf(
                                    "name" to "Pica",
                                    "description" to listOf(
                                        "Pie bavarde",
                                        "Plumage noir avec reflet métallique bleuté au dessus du corps, tête poitrine et blanc au niveau du ventre et des flancs"
                                    ),
                                    "id" to "QDrVEl0iWN6bEe9H4loG",
                                    "urlImage" to "",
                                    "espece" to listOf(
                                        hashMapOf(
                                            "name" to "Pica pica",
                                            "description" to emptyList<String>(),
                                            "id" to "NlXsiYxgsw1i0aYbNCtw",
                                            "urlImage" to ""
                                        )
                                    )
                                ),
                                hashMapOf(
                                    "name" to "Coleus",
                                    "description" to listOf(
                                        "Petit taille (inférieur à 50cm)",
                                        "Plumage majoritairement noir avec côté de la tête gris à reflets bleu",
                                        "Iris des yeux bleu très clair ou gris, très visible"
                                    ),
                                    "id" to "Tx3FIz3QLvQ6iiK30WJ6",
                                    "urlImage" to "",
                                    "espece" to listOf(
                                        hashMapOf(
                                            "name" to "Coleus monedula",
                                            "description" to emptyList<String>(),
                                            "id" to "7JkkFsCrz9BRG7Q43Nzv",
                                            "urlImage" to ""
                                        )
                                    )
                                ),
                                hashMapOf(
                                    "name" to "Nucifraga",
                                    "description" to listOf(
                                        "Corps brun foncé parsemé de taches blanches",
                                        "Bec noir et calotte brun foncé",
                                        "Plumes arrières noires avec zone de blanc en dessous"
                                    ),
                                    "id" to "jHgyaPghnJRZDYWUVmKV",
                                    "urlImage" to "",
                                    "espece" to listOf(
                                        hashMapOf(
                                            "name" to "Nucifraga caryocatactes",
                                            "description" to emptyList<String>(),
                                            "id" to "tR6YrTCRAgppVCsrxAlt",
                                            "urlImage" to ""
                                        )
                                    )
                                ),
                                hashMapOf(
                                    "name" to "Garrulus",
                                    "description" to listOf(
                                        "Plumage coloré, rayé de noir et blanc sur la tête dont les plumes peuvent se dresser",
                                        "Bec prolongé d'une bande noire sous l'œil",
                                        "Plumes du corps brun rosé à brun clair"
                                    ),
                                    "id" to "qDqp7G7JhK6FOoYgXqsU",
                                    "urlImage" to "",
                                    "espece" to listOf(
                                        hashMapOf(
                                            "name" to "Garrulus glandarius",
                                            "description" to emptyList<String>(),
                                            "id" to "JNLomydEAj97GUqiUV72",
                                            "urlImage" to ""
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

// Ajout à la collection
        db.collection("census")
            .add(censusData)
            .addOnSuccessListener { documentReference ->
                Log.d("Census", "Document ajouté avec ID : ${documentReference.id}")
            }
            .addOnFailureListener { exception ->
                Log.e("Census", "Erreur lors de l'ajout", exception)
            }
    }
}