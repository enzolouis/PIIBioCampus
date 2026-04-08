package com.fneb.piibiocampus.data.error

/**
 * Hiérarchie d'erreurs métier de l'application.
 * Chaque cas expose un [userMessage] prêt à afficher dans l'UI.
 *
 * Les DAO ne lancent jamais de FirebaseException brute vers le ViewModel :
 * ils mappent toujours vers un AppException via [FirebaseExceptionMapper].
 */
sealed class AppException(val userMessage: String, cause: Throwable? = null) : Exception(userMessage, cause) {

    // ── Authentification ──────────────────────────────────────────────────────

    /** Email ou mot de passe incorrect */
    class InvalidCredentials : AppException("Email ou mot de passe incorrect.")

    /** Compte déjà existant avec cet email */
    class EmailAlreadyInUse : AppException("Un compte existe déjà avec cet email.")

    /** Email non vérifié après inscription */
    class EmailNotVerified : AppException("Vérifie ta boîte mail pour activer ton compte.")

    /** Aucun compte trouvé pour cet email */
    class UserNotFound : AppException("Aucun compte trouvé pour cet email.")

    /** Mot de passe trop faible (< 6 caractères) */
    class WeakPassword : AppException("Le mot de passe doit contenir au moins 10 caractères, une majuscule, un chiffre et un caractère spécial.")

    /** Trop de tentatives de connexion */
    class TooManyRequests : AppException("Trop de tentatives. Réessaie dans quelques minutes.")

    /** Ré-authentification échouée (changement de mot de passe) */
    class ReauthenticationFailed : AppException("Ancien mot de passe incorrect.")

    /** Aucun utilisateur connecté alors qu'on en attend un */
    class NotAuthenticated : AppException("Tu dois être connecté·e pour effectuer cette action.")

    // ── Réseau ────────────────────────────────────────────────────────────────

    /** Pas de connexion réseau */
    class NetworkUnavailable : AppException("Pas de connexion internet. Vérifie ton réseau.")

    /** Requête expirée */
    class Timeout : AppException("La requête a pris trop de temps. Réessaie.")

    // ── Firestore ─────────────────────────────────────────────────────────────

    /** Règles de sécurité Firestore refusent l'accès */
    class PermissionDenied : AppException("Tu n'as pas les droits pour cette action.")

    /** Document Firestore introuvable */
    class DocumentNotFound : AppException("Les données demandées sont introuvables.")

    /** Données corrompues ou format inattendu */
    class InvalidData : AppException("Les données reçues sont invalides.")

    // ── Storage ───────────────────────────────────────────────────────────────

    /** Quota de stockage Firebase dépassé */
    class StorageQuotaExceeded : AppException("L'espace de stockage est saturé.")

    /** Objet Storage introuvable */
    class StorageObjectNotFound : AppException("Le fichier demandé est introuvable.")

    /** Erreur lors du traitement local d'une image */
    class ImageProcessingError(cause: Throwable? = null) : AppException("Impossible de traiter l'image.", cause)

    // ── Export ────────────────────────────────────────────────────────────────

    /** Erreur d'écriture fichier lors de l'export CSV */
    class ExportFailed(cause: Throwable? = null) : AppException("L'export a échoué. Vérifie les permissions de stockage.", cause)

    // ── Générique ─────────────────────────────────────────────────────────────

    /** Tout ce qui n'est pas identifié */
    class Unknown(cause: Throwable? = null) : AppException("Une erreur inattendue s'est produite.", cause)
}