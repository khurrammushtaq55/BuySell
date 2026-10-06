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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class HelpTopic(val title: String, val description: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit
) {
    var selectedTopic by remember { mutableStateOf<HelpTopic?>(null) }

    val topics = listOf(
        HelpTopic("1. Pehli khareedari kaise likhein", "Home screen par 'Phone Khareedna' tap karein. Category choose karein, Model aur IMEI daalein, Seller detail aur payment add kar ke Save karein."),
        HelpTopic("2. Phone kaise bechein", "Home screen par 'Phone Bechna' tap karein. Stock se phone select karein, Customer detail, sale price aur received amount daal kar Save karein."),
        HelpTopic("3. Baqi (udhaar) kaise rakhein", "Khareedate ya bechte waqt jab poori payment na aaye to baqi amount khud calculate ho jaati hai. Wahan 'Baqi kab denge?' date set karein."),
        HelpTopic("4. Customer se baad mein paisay kaise lein", "Home -> 'Khata / Hisaab' kholein. Customer ke naam par tap karein aur 'Paisay lein' button daba kar amount record karein."),
        HelpTopic("5. Purana phone exchange", "Home par 'Purana de kar naya' tap karein. Purana phone ki buy detail aur naya phone ki sale detail ek hi screen par bharein."),
        HelpTopic("6. Staff code kaise add karein", "Settings -> Team -> 'Invite Code Copy' karein aur staff ko WhatsApp par bhejein. Staff app mein code daal kar join karega."),
        HelpTopic("7. Munafa kahan dikhega (Owner)", "Home top bar par Settings ke saath Dashboard icon tap karein. Un mein 'Is mahine ka munafa' card par poora hisaab dikhega.")
    )

    if (selectedTopic != null) {
        AlertDialog(
            onDismissRequest = { selectedTopic = null },
            title = { Text(selectedTopic!!.title, fontWeight = FontWeight.Bold) },
            text = { Text(selectedTopic!!.description, fontSize = 16.sp) },
            confirmButton = {
                TextButton(onClick = { selectedTopic = null }) {
                    Text("Theek Hai")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Madad / Help Topics", fontWeight = FontWeight.Bold) },
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
            Text("Sub se zyada pooche jaane wale sawal:", fontSize = 16.sp, fontWeight = FontWeight.Bold)

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
