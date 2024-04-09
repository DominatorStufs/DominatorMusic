package app.vitune.core.ui.enums

enum class ColorPaletteName(val isDynamic: Boolean) {
    Default(isDynamic = false),
    Dynamic(isDynamic = true),
    MaterialYou(isDynamic = true),
    PureBlack(isDynamic = false),
    AMOLED(isDynamic = true)
}
