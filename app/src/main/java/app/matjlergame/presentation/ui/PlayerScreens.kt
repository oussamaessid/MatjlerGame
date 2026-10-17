package app.matjlergame.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.matjlergame.data.repository.PlayerRepository
import app.matjlergame.domain.model.GameMode
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun PlayerScreen(repository: PlayerRepository, ranking: Boolean, setup: Boolean = false, onBack: () -> Unit) {
    if (ranking) {
        LeaderboardScreen(repository, onBack)
        return
    }
    var name by rememberSaveable { mutableStateOf(repository.profile().name) }
    var avatar by rememberSaveable { mutableStateOf(repository.profile().avatar) }
    var error by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    BackHandler { if (!saving) onBack() }
    var edited by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(repository.currentProfile) {
        if (!edited) { name = repository.currentProfile.name; avatar = repository.currentProfile.avatar }
    }
    val valid = name.trim().length in 2..24 && name.none { it.isISOControl() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!setup) FilledTonalIconButton(enabled = !saving, onClick = onBack) {
                // Icon only; the description keeps it readable by screen readers.
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
            }
            Text(if (ranking) "Classement" else if (setup) "Bienvenue dans Equal To" else "Mon profil", style = MaterialTheme.typography.headlineMedium)
        }
        if (!ranking) {
            Text(if (setup) "Votre compte invité est créé automatiquement. Personnalisez votre profil pour rejoindre les autres joueurs."
                else "Un pseudo et un avatar pour vous retrouver dans le classement.")
            PlayerAvatar(avatar, 104.dp, Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))
            Text("Compte invité • conservé sur cet appareil", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value = name, onValueChange = { name = it; edited = true; error = false },
                label = { Text("Pseudo") }, singleLine = true, isError = !valid || error, enabled = !saving,
                supportingText = { Text(if (error) saveError else "De 2 à 24 caractères. Même pseudo possible avec un autre avatar") }, modifier = Modifier.fillMaxWidth())
            Text("Choisissez votre avatar")
            Text("48 avatars Ludo et 8 avatars classiques", style = MaterialTheme.typography.bodySmall)
            val avatarGridState = rememberLazyGridState(
                initialFirstVisibleItemIndex = AvatarGallery.displayOrder.indexOf(avatar).coerceAtLeast(0))
            LazyVerticalGrid(state = avatarGridState, columns = GridCells.Adaptive(minSize = 72.dp),
                modifier = Modifier.fillMaxWidth().height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AvatarGallery.displayOrder.size, key = { AvatarGallery.displayOrder[it] }) { position ->
                    val index = AvatarGallery.displayOrder[position]
                    Column(Modifier
                        .selectable(selected = avatar == index, enabled = !saving,
                            role = androidx.compose.ui.semantics.Role.RadioButton,
                            onClick = { avatar = index; edited = true })
                        .border(if (avatar == index) 3.dp else 1.dp,
                            if (avatar == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(vertical = 8.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        PlayerAvatar(index, 56.dp)
                        Text(AvatarGallery.names[index], style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
            Button(enabled = valid && !saving, onClick = {
                saving = true
                scope.launch {
                    try {
                        val problem = repository.saveGlobalProfile(name, avatar)
                        if (problem == null) onBack() else { error = true; saveError = problem }
                    } finally { saving = false }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (saving) "Enregistrement…" else if (setup) "Créer mon profil" else "Enregistrer")
            }
            if (setup) TextButton(enabled = !saving, onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Continuer avec mon profil par défaut")
            }
            if (error) Text(saveError, color = MaterialTheme.colorScheme.error)
            Text("Profil conservé sur cet appareil. Effacer les données de l’application supprime le profil.")
        }
    }
}
