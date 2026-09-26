package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StudioTab

@Composable
fun BottomStudioNav(
    activeTab: StudioTab,
    onTabSelected: (StudioTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = Color(0xFF131622),
        tonalElevation = 8.dp
    ) {
        val scrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudioTab.entries.forEach { tab ->
                val isSelected = tab == activeTab
                val icon = getTabIcon(tab)

                Box(
                    modifier = Modifier
                        .height(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) Color(0xFF4F46E5).copy(alpha = 0.25f)
                            else Color.Transparent
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) Color(0xFF818CF8) else Color(0xFF94A3B8),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            color = if (isSelected) Color(0xFFFFFFFF) else Color(0xFF94A3B8),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

private fun getTabIcon(tab: StudioTab): ImageVector {
    return when (tab) {
        StudioTab.CHARACTER -> Icons.Default.Face
        StudioTab.BACKGROUND -> Icons.Default.Image
        StudioTab.VOICE -> Icons.Default.Mic
        StudioTab.LIP_SYNC -> Icons.Default.RecordVoiceOver
        StudioTab.TIMELINE -> Icons.Default.ViewTimeline
        StudioTab.EFFECTS -> Icons.Default.AutoAwesome
        StudioTab.PREVIEW -> Icons.Default.PlayArrow
        StudioTab.EXPORT -> Icons.Default.IosShare
    }
}
