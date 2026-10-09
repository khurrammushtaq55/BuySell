package com.mmushtaq04.buysell.presentation.screens.help

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.ui.theme.BuySellTheme

data class HelpTopic(val title: String, val description: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit = {}
) {
    var selectedTopic by remember { mutableStateOf<HelpTopic?>(null) }

    val topics = listOf(
        HelpTopic(stringResource(R.string.help_topic_1_title), stringResource(R.string.help_topic_1_desc)),
        HelpTopic(stringResource(R.string.help_topic_2_title), stringResource(R.string.help_topic_2_desc)),
        HelpTopic(stringResource(R.string.help_topic_3_title), stringResource(R.string.help_topic_3_desc)),
        HelpTopic(stringResource(R.string.help_topic_4_title), stringResource(R.string.help_topic_4_desc)),
        HelpTopic(stringResource(R.string.help_topic_5_title), stringResource(R.string.help_topic_5_desc)),
        HelpTopic(stringResource(R.string.help_topic_6_title), stringResource(R.string.help_topic_6_desc)),
        HelpTopic(stringResource(R.string.help_topic_7_title), stringResource(R.string.help_topic_7_desc)),
        HelpTopic(stringResource(R.string.help_topic_8_title), stringResource(R.string.help_topic_8_desc)),
        HelpTopic(stringResource(R.string.help_topic_9_title), stringResource(R.string.help_topic_9_desc)),
        HelpTopic(stringResource(R.string.help_topic_10_title), stringResource(R.string.help_topic_10_desc))
    )

    if (selectedTopic != null) {
        AlertDialog(
            onDismissRequest = { selectedTopic = null },
            title = { Text(selectedTopic!!.title, fontWeight = FontWeight.Bold) },
            text = { Text(selectedTopic!!.description, fontSize = 16.sp) },
            confirmButton = {
                TextButton(onClick = { selectedTopic = null }) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.help_frequently_asked), fontSize = 16.sp, fontWeight = FontWeight.Bold)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(topics) { topic ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTopic = topic },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = topic.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HelpScreenPreview() {
    BuySellTheme {
        HelpScreen()
    }
}
