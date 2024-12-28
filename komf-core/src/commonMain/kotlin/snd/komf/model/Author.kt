package snd.komf.model

import kotlinx.serialization.Serializable

@Serializable
data class Author(
    val name: String,
    val role: AuthorRole
)
enum class AuthorRole {
    WRITER,
    PENCILLER,
    INKER,
    COLORIST,
    LETTERER,
    COVER,
    EDITOR,
    TRANSLATOR;

    companion object {
        fun fromString(value: String): AuthorRole? {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        }
    }
}
