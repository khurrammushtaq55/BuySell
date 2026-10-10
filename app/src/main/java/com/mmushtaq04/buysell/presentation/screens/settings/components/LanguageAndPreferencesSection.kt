package com.mmushtaq04.buysell.presentation.screens.settings.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.data.sync.DailySummaryWorker
import com.mmushtaq04.buysell.data.sync.MonthlySummaryWorker
import com.mmushtaq04.buysell.presentation.components.PremiumFeatureGate
import com.mmushtaq04.buysell.util.AppPreferencesManager

@Composable
fun LanguageAndPreferencesSection(
    currencySymbol: String,
    isMonthlyReportLocked: Boolean = false,
    onUnlockClick: () -> Unit = {},
    onUpdateCurrency: (symbol: String, code: String) -> Unit,
    onNavigateToHelp: () -> Unit
) {
    val context = LocalContext.current

    val languages = AppPreferencesManager.supportedLanguages
    val currentTag = remember { AppPreferencesManager.getAppLanguageTag(context) }
    var selectedLanguageOption by remember {
        mutableStateOf(languages.find { it.tag.equals(currentTag, ignoreCase = true) } ?: languages.first())
    }
    var expandedLanguageDropdown by remember { mutableStateOf(false) }

    val themeOptions = AppPreferencesManager.supportedThemes
    val currentThemeMode = remember { AppPreferencesManager.getAppThemeMode(context) }
    var selectedThemeOption by remember {
        mutableStateOf(themeOptions.find { it.mode.equals(currentThemeMode, ignoreCase = true) } ?: themeOptions.first())
    }
    var expandedThemeDropdown by remember { mutableStateOf(false) }

    var showBuyCostInSell by remember {
        mutableStateOf(runCatching { AppPreferencesManager.isShowBuyCostInSellEnabled(context) }.getOrDefault(true))
    }

    SettingsSectionCard(
        title = stringResource(R.string.settings_section_lang),
        icon = Icons.Default.Translate
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.settings_app_language), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

            Box {
                OutlinedTextField(
                    value = selectedLanguageOption.displayName,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.settings_label_selected_language)) },
                    modifier = Modifier.fillMaxWidth().clickable { expandedLanguageDropdown = true },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { expandedLanguageDropdown = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                )

                DropdownMenu(
                    expanded = expandedLanguageDropdown,
                    onDismissRequest = { expandedLanguageDropdown = false }
                ) {
                    languages.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                selectedLanguageOption = option
                                expandedLanguageDropdown = false
                                AppPreferencesManager.setAppLanguageTag(context, option.tag)
                                Toast.makeText(context, "Language updated: ${option.displayName}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(stringResource(R.string.settings_app_theme), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

            Box {
                OutlinedTextField(
                    value = stringResource(selectedThemeOption.displayNameResId),
                    onValueChange = {},
                    label = { Text(stringResource(R.string.settings_app_theme)) },
                    modifier = Modifier.fillMaxWidth().clickable { expandedThemeDropdown = true },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { expandedThemeDropdown = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                )

                DropdownMenu(
                    expanded = expandedThemeDropdown,
                    onDismissRequest = { expandedThemeDropdown = false }
                ) {
                    themeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.displayNameResId)) },
                            onClick = {
                                selectedThemeOption = option
                                expandedThemeDropdown = false
                                AppPreferencesManager.setAppThemeMode(context, option.mode)
                            }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(stringResource(R.string.settings_store_currency), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(stringResource(R.string.settings_store_currency_sub), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            var expandedCurrencyDropdown by remember { mutableStateOf(false) }
            var showCustomCurrencyDialog by remember { mutableStateOf(false) }
            var customCurrencyInput by remember { mutableStateOf("") }

            Box {
                OutlinedTextField(
                    value = currencySymbol,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.settings_store_currency)) },
                    modifier = Modifier.fillMaxWidth().clickable { expandedCurrencyDropdown = true },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { expandedCurrencyDropdown = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                )

                DropdownMenu(
                    expanded = expandedCurrencyDropdown,
                    onDismissRequest = { expandedCurrencyDropdown = false }
                ) {
                    AppPreferencesManager.supportedCurrencies.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text("${preset.flag} ${preset.name} (${preset.symbol})") },
                            onClick = {
                                expandedCurrencyDropdown = false
                                onUpdateCurrency(preset.symbol, preset.code)
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_currency_custom_option)) },
                        onClick = {
                            expandedCurrencyDropdown = false
                            showCustomCurrencyDialog = true
                        }
                    )
                }
            }

            if (showCustomCurrencyDialog) {
                AlertDialog(
                    onDismissRequest = { showCustomCurrencyDialog = false },
                    title = { Text(stringResource(R.string.settings_custom_currency_dialog_title), fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(
                            value = customCurrencyInput,
                            onValueChange = { customCurrencyInput = it },
                            label = { Text(stringResource(R.string.settings_custom_currency_label)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (customCurrencyInput.isNotBlank()) {
                                    onUpdateCurrency(customCurrencyInput.trim(), "CUSTOM")
                                    showCustomCurrencyDialog = false
                                    customCurrencyInput = ""
                                }
                            },
                            enabled = customCurrencyInput.isNotBlank()
                        ) {
                            Text(stringResource(R.string.action_save))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomCurrencyDialog = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val updated = !showBuyCostInSell
                        showBuyCostInSell = updated
                        AppPreferencesManager.setShowBuyCostInSellEnabled(context, updated)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_show_buy_price), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(stringResource(R.string.settings_show_buy_price_sub), fontSize = 12.sp, color = Color.Gray)
                }
                Switch(
                    checked = showBuyCostInSell,
                    onCheckedChange = { updated ->
                        showBuyCostInSell = updated
                        AppPreferencesManager.setShowBuyCostInSellEnabled(context, updated)
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            var isDailySummaryEnabled by remember { mutableStateOf(AppPreferencesManager.isDailySummaryEnabled(context)) }
            var dailySummaryTime by remember { mutableStateOf(AppPreferencesManager.getDailySummaryTime(context)) }
            var showTimePickerDialog by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val updated = !isDailySummaryEnabled
                        isDailySummaryEnabled = updated
                        AppPreferencesManager.setDailySummaryEnabled(context, updated)
                        DailySummaryWorker.scheduleDailySummary(context)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_daily_summary_title), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(stringResource(R.string.settings_daily_summary_sub), fontSize = 12.sp, color = Color.Gray)
                }
                Switch(
                    checked = isDailySummaryEnabled,
                    onCheckedChange = { updated ->
                        isDailySummaryEnabled = updated
                        AppPreferencesManager.setDailySummaryEnabled(context, updated)
                        DailySummaryWorker.scheduleDailySummary(context)
                    }
                )
            }

            if (isDailySummaryEnabled) {
                val formattedTime = remember(dailySummaryTime) {
                    val h = dailySummaryTime.first
                    val m = dailySummaryTime.second
                    val ampm = if (h >= 12) "PM" else "AM"
                    val displayHour = when {
                        h == 0 -> 12
                        h > 12 -> h - 12
                        else -> h
                    }
                    String.format(java.util.Locale.getDefault(), "%02d:%02d %s", displayHour, m, ampm)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePickerDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_daily_summary_time_label), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = formattedTime,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (showTimePickerDialog) {
                    DisposableEffect(Unit) {
                        val timePicker = android.app.TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                dailySummaryTime = hourOfDay to minute
                                AppPreferencesManager.setDailySummaryTime(context, hourOfDay, minute)
                                DailySummaryWorker.scheduleDailySummary(context, hourOfDay, minute)
                                showTimePickerDialog = false
                            },
                            dailySummaryTime.first,
                            dailySummaryTime.second,
                            false
                        )
                        timePicker.setOnCancelListener { showTimePickerDialog = false }
                        timePicker.setOnDismissListener { showTimePickerDialog = false }
                        timePicker.show()
                        onDispose {
                            timePicker.dismiss()
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            var isMonthlySummaryEnabled by remember { mutableStateOf(AppPreferencesManager.isMonthlySummaryEnabled(context)) }
            var monthlySchedule by remember { mutableStateOf(AppPreferencesManager.getMonthlySummarySchedule(context)) }
            var showMonthlyTimePicker by remember { mutableStateOf(false) }
            var showMonthlyDayDialog by remember { mutableStateOf(false) }

            PremiumFeatureGate(
                isLocked = isMonthlyReportLocked,
                featureName = stringResource(R.string.settings_monthly_summary_title),
                onUnlockClick = onUnlockClick
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val updated = !isMonthlySummaryEnabled
                            isMonthlySummaryEnabled = updated
                            AppPreferencesManager.setMonthlySummaryEnabled(context, updated)
                            MonthlySummaryWorker.scheduleMonthlySummary(context)
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_monthly_summary_title), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(stringResource(R.string.settings_monthly_summary_sub), fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = isMonthlySummaryEnabled,
                        onCheckedChange = { updated ->
                            isMonthlySummaryEnabled = updated
                            AppPreferencesManager.setMonthlySummaryEnabled(context, updated)
                            MonthlySummaryWorker.scheduleMonthlySummary(context)
                        }
                    )
                }
            }

            if (isMonthlySummaryEnabled) {
                val (currentDay, currentHour, currentMin) = monthlySchedule

                val formattedDayText = if (currentDay == 31) stringResource(R.string.settings_monthly_day_last_pill) else stringResource(R.string.settings_monthly_day_pill, currentDay)

                val formattedTimeText = remember(currentHour, currentMin) {
                    val ampm = if (currentHour >= 12) "PM" else "AM"
                    val displayHour = when {
                        currentHour == 0 -> 12
                        currentHour > 12 -> currentHour - 12
                        else -> currentHour
                    }
                    String.format(java.util.Locale.getDefault(), "%02d:%02d %s", displayHour, currentMin, ampm)
                }

                // Row 1: Day Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMonthlyDayDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_monthly_summary_day_label), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = formattedDayText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Row 2: Time Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMonthlyTimePicker = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_monthly_summary_time_label), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = formattedTimeText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Independent Dialog 1: Day Selector Dialog (1 to 31)
                if (showMonthlyDayDialog) {
                    var tempDay by remember { mutableStateOf(currentDay) }
                    val daysList = (1..31).toList()

                    AlertDialog(
                        onDismissRequest = { showMonthlyDayDialog = false },
                        title = { Text(stringResource(R.string.settings_monthly_day_dialog_title), fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.settings_monthly_day_dialog_msg), fontSize = 13.sp, color = Color.Gray)
                                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                                    LazyColumn {
                                        items(daysList) { dayNum ->
                                            val labelText = if (dayNum == 31) stringResource(R.string.settings_monthly_last_day_label) else "$dayNum"
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { tempDay = dayNum }
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = tempDay == dayNum,
                                                    onClick = { tempDay = dayNum }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(labelText, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    monthlySchedule = Triple(tempDay, currentHour, currentMin)
                                    AppPreferencesManager.setMonthlySummarySchedule(context, tempDay, currentHour, currentMin)
                                    MonthlySummaryWorker.scheduleMonthlySummary(context, tempDay, currentHour, currentMin)
                                    showMonthlyDayDialog = false
                                }
                            ) {
                                Text(stringResource(R.string.action_save))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showMonthlyDayDialog = false }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    )
                }

                // Independent Dialog 2: Time Picker Dialog
                if (showMonthlyTimePicker) {
                    DisposableEffect(Unit) {
                        val timePicker = android.app.TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                monthlySchedule = Triple(currentDay, hourOfDay, minute)
                                AppPreferencesManager.setMonthlySummarySchedule(context, currentDay, hourOfDay, minute)
                                MonthlySummaryWorker.scheduleMonthlySummary(context, currentDay, hourOfDay, minute)
                                showMonthlyTimePicker = false
                            },
                            currentHour,
                            currentMin,
                            false
                        )
                        timePicker.setOnCancelListener { showMonthlyTimePicker = false }
                        timePicker.setOnDismissListener { showMonthlyTimePicker = false }
                        timePicker.show()
                        onDispose {
                            timePicker.dismiss()
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SettingClickableRow(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = stringResource(R.string.help_title),
                subtitle = stringResource(R.string.settings_help_row_sub)
            ) {
                onNavigateToHelp()
            }
        }
    }
}
