package app.matjlergame.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.matjlergame.R
import app.matjlergame.data.repository.PlayerRepository
import app.matjlergame.domain.model.GameMode
import app.matjlergame.domain.model.GameTimer
import app.matjlergame.domain.model.LeaderboardPlayer
import app.matjlergame.domain.model.PlayerPeriods
import app.matjlergame.domain.model.RankingPeriod
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RankingPurple = Color(0xFF5140AB)
private val RankingInk = Color(0xFF292343)
private val RankingMuted = Color(0xFF817A95)

internal data class LeaderboardViewState(
    /** null = Général (the three difficulties together). */
    val mode: GameMode? = null,
    val range: RankingPeriod = RankingPeriod.DAY,
    val period: String = PlayerPeriods.today().toString(),
    val players: List<LeaderboardPlayer> = emptyList(),
    val loading: Boolean = false,
    val hasMore: Boolean = false,
    val meName: String = "Joueur",
    val meAvatar: Int = app.matjlergame.domain.model.PlayerAvatars.DEFAULT,
    val me: String = ""
)

@Composable
fun LeaderboardScreen(repository: PlayerRepository, onBack: () -> Unit) {
    var range by rememberSaveable { mutableStateOf(RankingPeriod.DAY) }
    var mode by rememberSaveable { mutableStateOf<GameMode?>(null) }
    BackHandler(onBack = onBack)
    key(range, mode) {
        LeaderboardRoute(repository, range, mode, onBack, { range = it }, { mode = it })
    }
}

@Composable
private fun LeaderboardRoute(
    repository: PlayerRepository, range: RankingPeriod, mode: GameMode?, onBack: () -> Unit,
    onPeriod: (RankingPeriod) -> Unit, onMode: (GameMode?) -> Unit
) {
    var state by remember { mutableStateOf(LeaderboardViewState(mode, range, range.id(PlayerPeriods.today()), loading = true,
        me = repository.currentProfile.id, meName = repository.currentProfile.name, meAvatar = repository.currentProfile.avatar)) }
    var revision by remember { mutableStateOf(0) }
    var append by remember { mutableStateOf(false) }
    var cursor by remember { mutableStateOf<JSONObject?>(null) }
    LaunchedEffect(revision) {
        state = state.copy(loading = true)
        try {
            // Silent: network problems only leave the cached or empty list on screen.
            if (!append) repository.syncScores()
            val page = repository.globalRanking(range, mode, if (append) cursor else null)
            check(page.getString("mode") == (mode?.name ?: PlayerRepository.ALL))
            val entries = page.getJSONArray("entries")
            val incoming = (0 until entries.length()).map { index ->
                val row = entries.getJSONObject(index)
                LeaderboardPlayer(row.getString("uid"), row.getString("name"), row.getInt("avatar"),
                    row.getInt("correctCount"), row.getLong("totalMs"))
            }
            cursor = page.optJSONObject("cursor")
            state = state.copy(players = if (append) (state.players + incoming).distinctBy { it.uid } else incoming,
                period = page.getString("period"), loading = false, hasMore = cursor != null)
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { state = state.copy(loading = false, hasMore = false) }
    }
    LeaderboardLayout(state, onBack, onPeriod, onMode,
        onRefresh = { append = false; revision++ }, onLoadMore = { append = true; revision++ })
}

@Composable
internal fun LeaderboardLayout(
    state: LeaderboardViewState, onBack: () -> Unit,
    onPeriod: (RankingPeriod) -> Unit, onMode: (GameMode?) -> Unit,
    onRefresh: () -> Unit, onLoadMore: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color(0xFFF3F5FA))) {
        Box(Modifier.fillMaxWidth().height(310.dp).background(Brush.linearGradient(
            listOf(Color(0xFF241B54), RankingPurple))))
        LazyColumn(Modifier.fillMaxSize().safeDrawingPadding().testTag("leaderboard_list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack,
                        modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.16f))) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(24.dp)) {
                        Text("EQUAL TO", Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(60.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center) {
                        Trophy(Modifier.size(36.dp), Color(0xFFFFD474))
                    }
                    Column {
                        Text("Le podium", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Des victoires. Du talent. Votre place.", color = Color.White.copy(alpha = 0.86f), fontSize = 13.sp)
                    }
                }
            }
            item {
                val ownIndex = state.players.indexOfFirst { it.uid == state.me }
                Surface(shape = RoundedCornerShape(22.dp), color = Color.White, shadowElevation = 4.dp) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PlayerAvatar(state.meAvatar, 52.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("VOTRE PROFIL", color = RankingMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            Text(state.meName, color = RankingInk, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (ownIndex >= 0) "${state.players[ownIndex].wins} victoire(s) · ${resultTime(state.players[ownIndex].durationMs)}"
                                else "Chaque victoire compte", color = RankingMuted, fontSize = 12.sp)
                        }
                        if (ownIndex >= 0) Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFFFF1CB)) {
                            Text("#${ownIndex + 1}", Modifier.padding(12.dp), color = Color(0xFF996716), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        }
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 1.dp) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("CHOISISSEZ VOTRE CLASSEMENT", color = RankingMuted, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFF2EFF9)).padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            RankingChoice("Aujourd’hui", state.range == RankingPeriod.DAY, { onPeriod(RankingPeriod.DAY) }, Modifier.weight(1f))
                            RankingChoice("Semaine", state.range == RankingPeriod.WEEK, { onPeriod(RankingPeriod.WEEK) }, Modifier.weight(1f))
                            RankingChoice("Mois", state.range == RankingPeriod.MONTH, { onPeriod(RankingPeriod.MONTH) }, Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (listOf<GameMode?>(null) + GameMode.entries).forEach { mode ->
                                RankingChoice(mode.label(), mode == state.mode, { onMode(mode) }, Modifier.weight(1f), compact = true)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(periodLabel(state.period, state.range), color = RankingInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Tous les joueurs · ${state.mode.label()}", color = RankingMuted, fontSize = 12.sp)
                            }
                            IconButton(onClick = onRefresh, enabled = !state.loading) {
                                Icon(Icons.Default.Refresh, contentDescription = "Actualiser le classement", tint = RankingPurple)
                            }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Les champions", color = RankingInk, fontSize = 19.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    if (!state.loading) {
                        Text("${state.players.size} affiché${if (state.players.size == 1) "" else "s"}", color = RankingMuted, fontSize = 12.sp)
                    }
                }
            }
            when {
                state.loading && state.players.isEmpty() -> item {
                    RankingStateCard("Le classement arrive…", "Nous recherchons les joueurs de cette période.", loading = true)
                }
                state.players.isEmpty() -> item {
                    RankingStateCard("À vous de jouer !", if (state.mode == null) "Gagnez une partie pour entrer dans ce classement."
                        else "Gagnez une partie en mode ${state.mode.label()} pour entrer dans ce classement.",
                        action = onRefresh)
                }
                else -> {
                    if (state.players.size >= 3) item { RankingPodium(state.players.take(3)) }
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                            Text("RANG / JOUEUR", Modifier.weight(1f), color = RankingMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("VICTOIRES / TEMPS", color = RankingMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    itemsIndexed(state.players, key = { _, player -> player.uid }) { index, player ->
                        RankingPlayerRow(index + 1, player, player.uid == state.me)
                    }
                    if (state.hasMore) item {
                        OutlinedButton(onClick = onLoadMore, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                            Text(if (state.loading) "Chargement…" else "Voir plus de joueurs")
                        }
                    }
                }
            }
            item {
                Row(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Trophy(Modifier.size(20.dp), RankingPurple)
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Les victoires d’abord, la rapidité ensuite.", color = RankingInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("À égalité, le temps total le plus court passe devant. Général additionne Facile, Moyen et Difficile.",
                            color = RankingMuted, fontSize = 12.sp)
                        Text("Périodes à l’heure de Tunis · pauses comprises", color = RankingMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingChoice(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier, compact: Boolean = false) {
    Surface(onClick = onClick, modifier = modifier, selected = selected,
        shape = RoundedCornerShape(if (compact) 12.dp else 11.dp),
        color = if (selected) RankingPurple else if (compact) Color(0xFFF3F5FA) else Color.Transparent,
        contentColor = if (selected) Color.White else RankingMuted) {
        Box(Modifier.heightIn(min = 44.dp).padding(horizontal = 3.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(text, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun RankingStateCard(title: String, message: String, loading: Boolean = false, action: (() -> Unit)? = null) {
    Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(76.dp).clip(CircleShape).background(Color(0xFFF0EAFB)), contentAlignment = Alignment.Center) {
                if (loading) CircularProgressIndicator(Modifier.size(30.dp), color = RankingPurple, strokeWidth = 3.dp)
                else Trophy(Modifier.size(36.dp), RankingPurple)
            }
            Text(title, color = RankingInk, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(message, color = RankingMuted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
            if (action != null) Button(onClick = action, colors = ButtonDefaults.buttonColors(containerColor = RankingPurple),
                shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("Réessayer")
            }
        }
    }
}

@Composable
private fun RankingPodium(players: List<LeaderboardPlayer>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        listOf(1, 0, 2).forEach { index ->
            val player = players[index]
            val medal = listOf(Color(0xFFBB861A), Color(0xFF75869B), Color(0xFFB07C55))[index]
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                if (index == 0) Trophy(Modifier.padding(bottom = 6.dp).size(26.dp), Color(0xFFE7AD35))
                Surface(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                    color = if (index == 0) Color(0xFFFFF3D6) else Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, medal.copy(alpha = 0.22f))) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = if (index == 0) 22.dp else 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Surface(shape = CircleShape, border = androidx.compose.foundation.BorderStroke(3.dp, medal)) {
                            Box(Modifier.padding(3.dp)) { PlayerAvatar(player.avatar, if (index == 0) 62.dp else 48.dp) }
                        }
                        Text(player.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp,
                            color = RankingInk, fontWeight = FontWeight.Bold)
                        Text("${player.wins} victoire${if (player.wins == 1) "" else "s"}", color = RankingMuted, fontSize = 11.sp)
                        Text(resultTime(player.durationMs), color = RankingInk, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        Text("0${index + 1}", color = medal, fontSize = if (index == 0) 34.sp else 27.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingPlayerRow(rank: Int, player: LeaderboardPlayer, me: Boolean = false) {
    val medal = when (rank) { 1 -> Color(0xFFBB861A); 2 -> Color(0xFF75869B); 3 -> Color(0xFFB07C55); else -> RankingPurple }
    Surface(shape = RoundedCornerShape(20.dp), color = if (me) RankingPurple.copy(alpha = 0.08f) else Color.White,
        border = if (me) androidx.compose.foundation.BorderStroke(1.5.dp, RankingPurple) else null) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(medal.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                Text("$rank", color = medal, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
            PlayerAvatar(player.avatar, 42.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(player.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = RankingInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (me) Text("C’est vous", color = RankingPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("${player.wins} victoire${if (player.wins == 1) "" else "s"}", color = RankingInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F3F8)) {
                    Text(resultTime(player.durationMs), Modifier.padding(horizontal = 7.dp, vertical = 4.dp), color = RankingMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun Trophy(modifier: Modifier, tint: Color) {
    Icon(painterResource(R.drawable.ic_trophy), contentDescription = null, modifier = modifier, tint = tint)
}

private fun GameMode?.label() = when (this) {
    null -> "Général"
    GameMode.EASY -> "Facile"
    GameMode.MEDIUM -> "Moyen"
    GameMode.HARD -> "Difficile"
}

private fun periodLabel(period: String, range: RankingPeriod): String {
    val french = { pattern: String -> DateTimeFormatter.ofPattern(pattern, Locale.FRENCH) }
    val label = { id: String ->
        when (range) {
            RankingPeriod.DAY -> LocalDate.parse(id).format(french("d MMMM yyyy"))
            RankingPeriod.WEEK -> RankingPeriod.weekStart(id).let { start ->
                "Du ${start.format(french("d MMMM"))} au ${start.plusDays(6).format(french("d MMMM yyyy"))}" }
            RankingPeriod.MONTH -> YearMonth.parse(id).format(french("MMMM yyyy"))
        }
    }
    return runCatching { label(period) }.getOrElse { label(range.id(PlayerPeriods.today())) }.replaceFirstChar { it.uppercaseChar() }
}

private fun resultTime(duration: Long): String = GameTimer.format(duration) + "." + (duration.coerceAtLeast(0) % 1000 / 100)
