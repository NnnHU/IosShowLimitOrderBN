package com.pythonn.androidshowlimitorderbn.ui.screens

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pythonn.androidshowlimitorderbn.ui.viewmodel.MarketDataViewModel
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockViewModel: MarketDataViewModel = mock()

    @Test
    fun mainScreen_tabsDisplayedAndClickable() {
        composeTestRule.setContent {
            MainScreen(
                viewModel = mockViewModel,
                onNavigateToSpotDetails = { /* Do nothing for test */ },
                onNavigateToFuturesDetails = { /* Do nothing for test */ },
                onNavigateToAbout = { /* Do nothing for test */ }
            )
        }

        // Verify Spot tab is displayed and initially selected
        composeTestRule.onNodeWithText("Spot").assertExists().assertIsSelected()

        // Verify Futures tab is displayed and not selected
        composeTestRule.onNodeWithText("Futures").assertExists().assertIsNotSelected()

        // Click on Futures tab
        composeTestRule.onNodeWithText("Futures").performClick()

        // Verify Futures tab is now selected
        composeTestRule.onNodeWithText("Futures").assertIsSelected()
        composeTestRule.onNodeWithText("Spot").assertIsNotSelected()
    }
}