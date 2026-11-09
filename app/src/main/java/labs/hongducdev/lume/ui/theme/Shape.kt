package labs.hongducdev.lume.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive shapes matching sameerasw/essentials:
 * - extraSmall: 4.dp for inner list items in 24.dp containers
 * - small: 12.dp for pickers and textfields
 * - medium: 16.dp for cards and dialogs
 * - large: 20.dp for FABs and primary controls
 * - extraLarge: 28.dp for full-width action buttons
 */
val ToolkitShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

object ExpressiveRadius {
    val Container = 24.dp
    val Card = 24.dp
    val Squircle = 16.dp
    val Pill = 999.dp
    val Item = 4.dp
    val Button = 14.dp
}
