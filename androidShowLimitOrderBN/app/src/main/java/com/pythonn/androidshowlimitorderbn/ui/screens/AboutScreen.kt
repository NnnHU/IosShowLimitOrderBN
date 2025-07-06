
package com.pythonn.androidshowlimitorderbn.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pythonn.androidshowlimitorderbn.ui.viewmodel.MarketDataViewModel

@Composable
fun AboutScreen(viewModel: MarketDataViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("About This App")
        // Add more content here
    }
}
