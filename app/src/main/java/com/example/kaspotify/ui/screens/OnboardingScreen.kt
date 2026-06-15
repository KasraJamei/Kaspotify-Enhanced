package com.example.kaspotify.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class OnboardPage(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val body: String,
    val accent: Color
)

private val pages = listOf(
    OnboardPage(
        Icons.Filled.LibraryMusic,
        "Welcome to Kaspotify",
        "Your on-device music, beautifully played — and it all works fully offline.",
        Color(0xFF5B8DEF)
    ),
    OnboardPage(
        Icons.Filled.AutoAwesome,
        "Your whole library",
        "Songs, albums, artists and favorites. Sort A–Z and jump with the side index. " +
            "Quality badges show each track's bitrate.",
        Color(0xFF1DB954)
    ),
    OnboardPage(
        Icons.Filled.Favorite,
        "Now Playing",
        "Tap the mini-player to open it. Double-tap the artwork to like or unlike, scrub the " +
            "timeline, swipe to set a sleep timer, and open the EQ or visualizer.",
        Color(0xFFFF5A5F)
    ),
    OnboardPage(
        Icons.Filled.Tune,
        "Make it yours",
        "Swipe between Home, Search and Playlists. Build playlists, share songs, and turn features " +
            "on or off any time in Settings.",
        Color(0xFFB66CFF)
    )
)

/**
 * Full-screen first-launch guide: a swipeable carousel introducing the app. Each slide has its own
 * accent color (glowing icon halo + a soft backdrop wash), the content parallax-fades as you swipe,
 * and [onFinish] is called on Skip or after the last slide. Re-openable later from Settings.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex
    val accent by animateColorAsState(pages[pagerState.currentPage].accent, label = "accent")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(
                Brush.verticalGradient(
                    0f to accent.copy(alpha = 0.22f),
                    0.5f to Color.Transparent,
                    1f to Color.Transparent
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TextButton(
            onClick = onFinish,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.align(Alignment.Center)
        ) { index ->
            val page = pages[index]
            // Parallax: fade & shrink the page content as it slides away from center.
            val pageOffset = (pagerState.currentPage - index) + pagerState.currentPageOffsetFraction
            val dist = abs(pageOffset).coerceIn(0f, 1f)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 36.dp)
                    .graphicsLayer {
                        alpha = 1f - dist
                        val s = 0.85f + (1f - dist) * 0.15f
                        scaleX = s
                        scaleY = s
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Soft glow halo behind the icon.
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(page.accent.copy(alpha = 0.45f), Color.Transparent)
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .background(page.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            page.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    page.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    page.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { i ->
                    val selected = i == pagerState.currentPage
                    val dotWidth by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dot")
                    Box(
                        modifier = Modifier
                            .size(width = dotWidth, height = 8.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(
                                if (selected) accent
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (isLast) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLast) "Get started" else "Next")
            }
        }
    }
}
