package io.github.pushkar.chorplay.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class ChorplayTypographyTest {
    private fun Typography.allRoles(): List<TextStyle> =
        listOf(
            displayLarge,
            displayMedium,
            displaySmall,
            headlineLarge,
            headlineMedium,
            headlineSmall,
            titleLarge,
            titleMedium,
            titleSmall,
            bodyLarge,
            bodyMedium,
            bodySmall,
            labelLarge,
            labelMedium,
            labelSmall,
            displayLargeEmphasized,
            displayMediumEmphasized,
            displaySmallEmphasized,
            headlineLargeEmphasized,
            headlineMediumEmphasized,
            headlineSmallEmphasized,
            titleLargeEmphasized,
            titleMediumEmphasized,
            titleSmallEmphasized,
            bodyLargeEmphasized,
            bodyMediumEmphasized,
            bodySmallEmphasized,
            labelLargeEmphasized,
            labelMediumEmphasized,
            labelSmallEmphasized,
        )

    @Test
    fun `every role and its emphasized form use the chosen family`() {
        val roles = chorTypography(FontFamily.Serif).allRoles()
        assertThat(roles).hasSize(30)
        roles.forEach { assertThat(it.fontFamily).isEqualTo(FontFamily.Serif) }
    }

    @Test
    fun `display and headline small keep Material's metrics`() {
        val material = Typography()
        val chor = chorTypography(ChorFontFamily)
        assertThat(chor.displaySmall).isEqualTo(material.displaySmall.copy(fontFamily = ChorFontFamily))
        assertThat(chor.headlineSmall).isEqualTo(material.headlineSmall.copy(fontFamily = ChorFontFamily))
        assertThat(chor.displaySmall.fontSize).isEqualTo(36.sp)
        assertThat(chor.headlineSmall.fontSize).isEqualTo(24.sp)
    }

    @Test
    fun `the system font scale keeps today's values`() {
        val chor = chorTypography(ChorFontFamily)
        assertThat(chor.displayLarge.fontSize).isEqualTo(34.sp)
        assertThat(chor.displayLarge.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(chor.titleLarge.fontSize).isEqualTo(18.sp)
        assertThat(chor.bodyMedium.letterSpacing).isEqualTo(0.25.sp)
        assertThat(chor.labelSmall.fontSize).isEqualTo(11.sp)
        assertThat(chor.displayLargeEmphasized.fontWeight).isEqualTo(FontWeight.ExtraBold)
        assertThat(chor.bodyLargeEmphasized.fontWeight).isEqualTo(FontWeight.Medium)
        assertThat(Typography).isEqualTo(chor)
    }
}
