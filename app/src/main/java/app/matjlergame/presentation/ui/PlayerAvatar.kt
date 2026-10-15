package app.matjlergame.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import app.matjlergame.domain.model.PlayerAvatars
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.matjlergame.R

object AvatarGallery {
    val names = listOf("Renard", "Panda", "Tigre", "Grenouille", "Koala", "Poulpe", "Lion", "Robot") +
        (1..48).map { "Ludo " + it.toString().padStart(2, '0') }
    val icons = listOf(R.drawable.avatar_fox, R.drawable.avatar_panda, R.drawable.avatar_tiger,
        R.drawable.avatar_frog, R.drawable.avatar_koala, R.drawable.avatar_octopus, R.drawable.avatar_lion, R.drawable.avatar_robot,
        R.drawable.avatar_ludo_01, R.drawable.avatar_ludo_02, R.drawable.avatar_ludo_03, R.drawable.avatar_ludo_04, R.drawable.avatar_ludo_05, R.drawable.avatar_ludo_06, R.drawable.avatar_ludo_07, R.drawable.avatar_ludo_08, R.drawable.avatar_ludo_09, R.drawable.avatar_ludo_10, R.drawable.avatar_ludo_11, R.drawable.avatar_ludo_12, R.drawable.avatar_ludo_13, R.drawable.avatar_ludo_14, R.drawable.avatar_ludo_15, R.drawable.avatar_ludo_16, R.drawable.avatar_ludo_17, R.drawable.avatar_ludo_18, R.drawable.avatar_ludo_19, R.drawable.avatar_ludo_20, R.drawable.avatar_ludo_21, R.drawable.avatar_ludo_22, R.drawable.avatar_ludo_23, R.drawable.avatar_ludo_24, R.drawable.avatar_ludo_25, R.drawable.avatar_ludo_26, R.drawable.avatar_ludo_27, R.drawable.avatar_ludo_28, R.drawable.avatar_ludo_29, R.drawable.avatar_ludo_30, R.drawable.avatar_ludo_31, R.drawable.avatar_ludo_32, R.drawable.avatar_ludo_33, R.drawable.avatar_ludo_34, R.drawable.avatar_ludo_35, R.drawable.avatar_ludo_36, R.drawable.avatar_ludo_37, R.drawable.avatar_ludo_38, R.drawable.avatar_ludo_39, R.drawable.avatar_ludo_40, R.drawable.avatar_ludo_41, R.drawable.avatar_ludo_42, R.drawable.avatar_ludo_43, R.drawable.avatar_ludo_44, R.drawable.avatar_ludo_45, R.drawable.avatar_ludo_46, R.drawable.avatar_ludo_47, R.drawable.avatar_ludo_48)
    val displayOrder = (8 until PlayerAvatars.COUNT).toList() + (0 until 8).toList()
    init {
        check(names.size == icons.size && icons.size == PlayerAvatars.COUNT)
    }
}

@Composable
fun PlayerAvatar(index: Int, size: Dp = 56.dp, modifier: Modifier = Modifier) {
    val safeIndex = index.takeIf { PlayerAvatars.isValid(it) } ?: PlayerAvatars.DEFAULT
    Image(painterResource(AvatarGallery.icons[safeIndex]), contentDescription = "Avatar ${AvatarGallery.names[safeIndex]}",
        modifier = modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop)
}
