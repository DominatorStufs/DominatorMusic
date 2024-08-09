package app.vitune.core.ui

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.isAvailableOnDevice
import androidx.compose.ui.unit.sp

@Immutable
data class Typography(
    val xxs: TextStyle,
    val xs: TextStyle,
    val s: TextStyle,
    val m: TextStyle,
    val l: TextStyle,
    val xxl: TextStyle,
    internal val fontFamily: BuiltInFontFamily
) {
    fun copy(color: Color) = Typography(
        xxs = xxs.copy(color = color),
        xs = xs.copy(color = color),
        s = s.copy(color = color),
        m = m.copy(color = color),
        l = l.copy(color = color),
        xxl = xxl.copy(color = color),
        fontFamily = fontFamily
    )

    companion object : Saver<Typography, List<Any>> {
        override fun restore(value: List<Any>) = typographyOf(
            Color((value[0] as Long).toULong()),
            value[1] as BuiltInFontFamily,
            value[2] as Boolean
        )

        override fun SaverScope.save(value: Typography) = listOf(
            value.xxs.color.value.toLong(),
            value.fontFamily,
            value.xxs.platformStyle?.paragraphStyle?.includeFontPadding ?: false
        )
    }
}

private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

@Composable
fun googleFontsAvailable(): Boolean {
    val context = LocalContext.current

    return runCatching {
        googleFontsProvider.isAvailableOnDevice(context.applicationContext)
    }.getOrElse {
        it.printStackTrace()
        if (it is IllegalStateException) Log.e("Typography", "Google Fonts certificates don't match. Is the user using a VPN?")
        false
    }
}

private val poppinsFonts = listOf(
    Font(
        resId = R.font.poppins_w300,
        weight = FontWeight.Light
    ),
    Font(
        resId = R.font.poppins_w400,
        weight = FontWeight.Normal
    ),
    Font(
        resId = R.font.poppins_w500,
        weight = FontWeight.Medium
    ),
    Font(
        resId = R.font.poppins_w600,
        weight = FontWeight.SemiBold
    ),
    Font(
        resId = R.font.poppins_w700,
        weight = FontWeight.Bold
    )
)

private val poppinsFontFamily = FontFamily(poppinsFonts)

enum class BuiltInFontFamily(internal val googleFont: GoogleFont?) : Parcelable {
    Poppins(null),
    Roboto(GoogleFont("Roboto")),
    Montserrat(GoogleFont("Montserrat")),
    Nunito(GoogleFont("Nunito")),
    Rubik(GoogleFont("Rubik")),
    System(null);

    override fun writeToParcel(parcel: Parcel, flags: Int) = parcel.writeString(name)
    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<BuiltInFontFamily> {
        override fun createFromParcel(parcel: Parcel) = BuiltInFontFamily.valueOf(parcel.readString()!!)
        override fun newArray(size: Int): Array<BuiltInFontFamily?> = arrayOfNulls(size)
    }
}

private fun googleFontsFamilyFrom(font: BuiltInFontFamily) = font.googleFont?.let {
    FontFamily(
        listOf(
            Font(
                googleFont = it,
                fontProvider = googleFontsProvider,
                weight = FontWeight.Light
            ),
            Font(
                googleFont = it,
                fontProvider = googleFontsProvider,
                weight = FontWeight.Normal
            ),
            Font(
                googleFont = it,
                fontProvider = googleFontsProvider,
                weight = FontWeight.Medium
            ),
            Font(
                googleFont = it,
                fontProvider = googleFontsProvider,
                weight = FontWeight.SemiBold
            ),
            Font(
                googleFont = it,
                fontProvider = googleFontsProvider,
                weight = FontWeight.Bold
            )
        ) + poppinsFonts
    )
}

fun typographyOf(
    color: Color,
    fontFamily: BuiltInFontFamily,
    applyFontPadding: Boolean
): Typography {
    val textStyle = TextStyle(
        fontFamily = when {
            fontFamily == BuiltInFontFamily.System -> FontFamily.Default
            fontFamily.googleFont != null -> googleFontsFamilyFrom(fontFamily)
            else -> poppinsFontFamily
        },
        fontWeight = FontWeight.Normal,
        color = color,
        platformStyle = PlatformTextStyle(includeFontPadding = applyFontPadding)
    )

    return Typography(
        xxs = textStyle.copy(fontSize = 12.sp),
        xs = textStyle.copy(fontSize = 14.sp),
        s = textStyle.copy(fontSize = 16.sp),
        m = textStyle.copy(fontSize = 18.sp),
        l = textStyle.copy(fontSize = 20.sp),
        xxl = textStyle.copy(fontSize = 32.sp),
        fontFamily = fontFamily
    )
}
