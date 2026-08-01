package snd.komf.providers.kodansha.model

import kotlinx.serialization.Serializable

// Azuki exposes creators without role information (`credits` is consistently null),
// so every creator is mapped to the configured default author role.
@Serializable
data class KodanshaCreator(
    val name: String,
    val uuid: String? = null,
    val slug: String? = null,
)
