package app.vigil.content

import app.vigil.domain.Catalog
import app.vigil.domain.Preset
import java.util.Locale

/** A catalog vow's words: its name, a one-line rule, its "life" (why people keep it) and a word for the hard days. */
data class CatalogText(val name: String, val line: String, val life: String, val hard: String, val unit: String = "")

object Content {
    fun russian(locale: Locale) = locale.language == "ru"

    fun text(p: Preset, locale: Locale): CatalogText = text(p.id, locale) ?: CatalogText(p.id, "", "", "")

    fun text(id: String, locale: Locale): CatalogText? = (if (russian(locale)) catalogRu else catalogEn)[id]

    /** Every catalog entry has its words in both languages (checked by a test). */
    internal fun missing(): List<String> = Catalog.all.map { it.id }.filter { it !in catalogRu || it !in catalogEn }
}
