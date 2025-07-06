package com.pythonn.androidshowlimitorderbn.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.pythonn.androidshowlimitorderbn.ui.viewmodel.MarketDataViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: MarketDataViewModel,
    onNavigateToSpotDetails: () -> Unit,
    onNavigateToFuturesDetails: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val pages = listOf(
        TabPage("Spot", Icons.Default.Home) { SpotMarketScreen(viewModel = viewModel, onNavigateToOrderBookDetails = onNavigateToSpotDetails) },
        TabPage("Futures", Icons.Default.Build) { FuturesMarketScreen(viewModel = viewModel, onNavigateToOrderBookDetails = onNavigateToFuturesDetails) }
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            pages.forEachIndexed { index, page ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(page.title) },
                    icon = { Icon(page.icon, contentDescription = page.title) }
                )
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            pages[page].content()
        }
    }
}

data class TabPage(
    val title: String,
    val icon: ImageVector,
    val content: @Composable () -> Unit
)
