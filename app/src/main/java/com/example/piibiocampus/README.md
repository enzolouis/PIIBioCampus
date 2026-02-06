# Piibiocampus - Architecture et Explications

## Objectif

Cette application Android utilise **Firebase** pour l’authentification et suit une architecture **MVVM** moderne avec **StateFlow**, séparant clairement la logique métier de l’UI.  
Chaque Activity est légère et observe son ViewModel pour mettre à jour l’interface.

---

## Architecture mise en place

### Packages principaux

- `ui.auth`  
  Contient toutes les Activities liées à l’authentification et les ViewModels correspondants.

    - **ConnectionActivity**
        - Activity de connexion utilisateur.
        - Observe `AuthViewModel` pour gérer la session persistante Firebase et naviguer selon le rôle (USER / ADMIN / SUPER_ADMIN).

    - **CreateAccountActivity**
        - Activity pour créer un compte.
        - Utilise `AuthViewModel` pour la création de compte et l’écriture dans Firestore.

    - **ResetPassWordActivity**
        - Activity pour la réinitialisation de mot de passe.
        - Utilise `ResetPasswordViewModel` pour envoyer les emails de reset et gérer le cooldown entre requêtes.

- `ui.map`  
  Contient les Activities liées à la carte et navigation principale de l’utilisateur (ex : `MapActivity`).

- `ui.admin`  
  Contient les Activities réservées aux administrateurs (ex : `DashboardAdminActivity`).

- `data.dao`  
  Contient les classes d’accès aux données Firebase (ex : `UserDao`).
    - Toutes les interactions avec Firebase Auth et Firestore passent par ici pour **centraliser la logique métier**.

- `utils`
    - `Extensions` : fonctions d’extension utiles (ex : `toast`).
    - `Validators` : validation des champs email/mot de passe.

---

## ViewModels et StateFlow

- `AuthViewModel`
    - Centralise toute la logique Auth (connexion, création de compte, rôle utilisateur).
    - Expose un `StateFlow<AuthUiState>` observé par les Activities.

- `ResetPasswordViewModel`
    - Gère la réinitialisation de mot de passe.
    - Expose un `StateFlow<ResetPasswordUiState>` pour l’UI.

---

## Gestion de la session persistante

- Firebase Auth conserve automatiquement la session de l’utilisateur connecté.
- `ConnectionActivity` vérifie au lancement si un utilisateur est déjà connecté via `checkCurrentUserAndFetchRoleIfNeeded()` dans `AuthViewModel`.
- Navigation automatique vers l’Activity appropriée selon le rôle de l’utilisateur.

---

## Avantages de cette architecture

1. **Séparation nette UI / logique métier** (MVVM + ViewModel)
2. **Testabilité facilitée** (toute la logique Firebase est centralisée)
3. **UI réactive** grâce aux `StateFlow`
4. **Session persistante** gérée proprement via Firebase Auth
5. **CoolDown pour ResetPassWord** géré dans le ViewModel

---

## Flux global

User Interaction -> Activity -> ViewModel -> DAO (Firebase) -> ViewModel -> StateFlow -> Activity (UI)

---

## Notes importantes

- `repeatOnLifecycle(Lifecycle.State.STARTED)` est utilisé pour observer les StateFlows sans gaspiller de ressources.
- Les erreurs Firebase sont centralisées dans les ViewModels et transmises à l’UI via les StateFlows.
- Toutes les opérations Firebase se font **asynchrones** avec `kotlinx.coroutines` et `await()` pour éviter les callbacks dans les Activities.
