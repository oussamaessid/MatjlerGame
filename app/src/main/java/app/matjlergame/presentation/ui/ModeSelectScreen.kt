package app.matjlergame.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.matjlergame.ads.AdManager
import app.matjlergame.domain.model.GameMode
import com.google.android.gms.ads.*

private val HomeInk = Color(0xFF172B35)
private val HomeMuted = Color(0xFF63747A)
private val HomePaper = Color(0xFFF4F6F2)
private val HomeLime = Color(0xFFD9F279)

@Composable
fun ModeSelectScreen(
    adManager: AdManager,
    isLoading: Boolean = false,
    onModeSelected: (GameMode) -> Unit,
    playerName: String = "Mon profil",
    playerAvatar: Int = 0,
    onProfileClicked: () -> Unit = {},
    onRankingClicked: () -> Unit = {},
    onHowToPlayClicked: () -> Unit = {},
    completedModes: Set<GameMode> = emptySet(),
    totalPlayed: Int = 0,
    totalWon: Int = 0
) {
    val context = LocalContext.current
    var bannerLoaded by remember { mutableStateOf(false) }
    val banner = remember(context) {
        AdView(context).apply {
            adUnitId = AdManager.BANNER_MODE_SELECT_AD_UNIT_ID
            setAdSize(AdSize.BANNER)
            adListener = object : AdListener() {
                override fun onAdLoaded() { bannerLoaded = true }
                override fun onAdFailedToLoad(error: LoadAdError) { bannerLoaded = false }
            }
        }
    }
    DisposableEffect(banner) {
        banner.loadAd(AdRequest.Builder().build())
        onDispose { banner.adListener = object : AdListener() {}; banner.destroy() }
    }

    Box(Modifier.fillMaxSize().background(HomePaper).safeDrawingPadding()) {
        BoxWithConstraints(Modifier.align(Alignment.TopCenter).widthIn(max = 960.dp).fillMaxSize()) {
            val minimal = maxHeight < 600.dp && LocalDensity.current.fontScale > 1.5f
            val bannerFits = maxWidth >= 320.dp
            val landscape = maxWidth > maxHeight
            val compact = maxHeight < 620.dp || LocalDensity.current.fontScale > 1.2f
            val gap = if (compact) 10.dp else 18.dp
            Column(Modifier.fillMaxSize().padding(horizontal = if (maxWidth < 360.dp) 12.dp else 20.dp), verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !isLoading, onClick = onProfileClicked).padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        PlayerAvatar(playerAvatar, 38.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("BONJOUR", fontSize = 10.sp, letterSpacing = 1.5.sp, color = HomeMuted)
                            Text(playerName, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                color = HomeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    IconButton(enabled = !isLoading, onClick = onRankingClicked,
                        modifier = Modifier.size(48.dp).background(Color.White, CircleShape)) {
                        Icon(androidx.compose.ui.res.painterResource(app.matjlergame.R.drawable.ic_trophy),
                            "Classements", tint = HomeInk, modifier = Modifier.size(23.dp))
                    }
                    IconButton(enabled = !isLoading, onClick = onHowToPlayClicked,
                        modifier = Modifier.size(48.dp).background(Color.White, CircleShape)) {
                        Icon(Icons.Default.Info, "Comment jouer", tint = HomeInk)
                    }
                }
                if (landscape) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(gap)) {
                            HomeHeading(true)
                            HomeProgress(completedModes.size, totalPlayed, totalWon, true)
                        }
                        HomeChallenges(Modifier.weight(1f).fillMaxHeight(), completedModes, !isLoading, true, onModeSelected)
                    }
                } else {
                    if (!minimal) HomeHeading(compact)
                    HomeProgress(completedModes.size, totalPlayed, totalWon, compact)
                    HomeChallenges(Modifier.weight(1f), completedModes, !isLoading, compact, onModeSelected)
                }
                // A fixed slot prevents the controls moving when an advertisement arrives.
                Box(Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
                    if (bannerLoaded && bannerFits) AndroidView(factory = { banner }, modifier = Modifier.size(320.dp, 50.dp))
                }
            }
        }
        if (isLoading) {
            Box(Modifier.fillMaxSize().background(HomeInk.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = HomeLime)
                    Text("Préparation du défi…", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun HomeHeading(compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("=", modifier = Modifier.background(HomeLime, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 2.dp),
                color = HomeInk, fontSize = if (compact) 28.sp else 36.sp, fontWeight = FontWeight.Black)
            Text("Equal To", color = HomeInk, fontSize = if (compact) 32.sp else 42.sp,
                fontWeight = FontWeight.Black, letterSpacing = (-1).sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        if (!compact) Text("Un peu de logique. Un défi chaque jour.", color = HomeMuted, fontSize = 13.sp)
    }
}

@Composable
private fun HomeProgress(completed: Int, played: Int, won: Int, compact: Boolean) {
    Column(Modifier.fillMaxWidth().background(HomeInk, RoundedCornerShape(22.dp))
        .padding(if (compact) 14.dp else 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (!compact) Text("VOTRE RENDEZ-VOUS", color = HomeLime, fontSize = 10.sp, letterSpacing = 1.sp)
                Text(if (completed == 3) "À demain !" else "Les défis du jour", color = Color.White,
                    fontSize = if (compact) 18.sp else 23.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("$completed / 3", color = HomeLime, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { index ->
                Box(Modifier.weight(1f).height(5.dp).background(
                    if (index < completed) HomeLime else Color.White.copy(alpha = 0.18f), CircleShape))
            }
        }
        if (!compact) Text(if (played > 0) "$played parties  ·  $won victoires  ·  ${won * 100 / played} % de réussite"
            else "Trois niveaux, à votre rythme.", color = Color(0xFFD0DCDB), fontSize = 12.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HomeChallenges(
    modifier: Modifier,
    completed: Set<GameMode>,
    enabled: Boolean,
    compact: Boolean,
    onSelect: (GameMode) -> Unit
) {
    BoxWithConstraints(modifier) {
    val showDetails = maxHeight > (250 * LocalDensity.current.fontScale).dp
    val narrow = maxWidth < 300.dp
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
        Text("Choisissez votre défi", color = HomeInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        GameMode.entries.forEachIndexed { index, mode ->
            val done = mode in completed
            val accent = listOf(Color(0xFFD9F279), Color(0xFFFFE2B9), Color(0xFFE2DDFB))[index]
            Row(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Color.White).border(1.dp, Color(0xFFE2E8DF), RoundedCornerShape(20.dp))
                .clickable(enabled = enabled) { onSelect(mode) }.padding(horizontal = if (narrow) 10.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(if (compact) 36.dp else 44.dp).background(accent, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center) {
                    Text(if (done) "✓" else listOf("+", "×", "÷")[index], fontSize = 25.sp,
                        color = HomeInk, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(listOf("Facile", "Moyen", "Difficile")[index], fontSize = if (compact) 17.sp else 20.sp,
                        fontWeight = FontWeight.Bold, color = HomeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (showDetails) Text(if (done) "Terminé · Voir le résultat" else listOf("Pour bien commencer", "Passez à la vitesse supérieure", "À vous de relever le défi")[index],
                        fontSize = 12.sp, color = HomeMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, if (done) "Voir le résultat" else "Jouer", tint = HomeInk,
                    modifier = Modifier.size(20.dp))
            }
        }
    }
}
}
