package app.vitune.android.lib.providers.innertube.utils

import app.vitune.android.lib.providers.innertube.Innertube
import app.vitune.android.lib.providers.innertube.models.SectionListRenderer

internal fun SectionListRenderer.findSectionByTitle(text: String) = contents?.find {
    val title = it
        .musicCarouselShelfRenderer
        ?.header
        ?.musicCarouselShelfBasicHeaderRenderer
        ?.title
        ?: it
            .musicShelfRenderer
            ?.title

    title
        ?.runs
        ?.firstOrNull()
        ?.text == text
}

internal fun SectionListRenderer.findSectionByStrapline(text: String) = contents?.find {
    it
        .musicCarouselShelfRenderer
        ?.header
        ?.musicCarouselShelfBasicHeaderRenderer
        ?.strapline
        ?.runs
        ?.firstOrNull()
        ?.text == text
}

infix operator fun <T : Innertube.Item> Innertube.ItemsPage<T>?.plus(other: Innertube.ItemsPage<T>) =
    other.copy(
        items = (this?.items?.plus(other.items ?: emptyList()) ?: other.items)
            ?.distinctBy(Innertube.Item::key),
        continuation = other.continuation ?: this?.continuation
    )
