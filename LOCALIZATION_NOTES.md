# Localization & String Resource Guidelines

## Overview
All user-facing strings across the BuySell application are centralized in Android XML string resources.
Hardcoded strings in Composable UI components are strictly avoided.

## String Resource Directories
1. **`values/strings.xml`**: Default / English
2. **`values-ur/strings.xml`**: Standard Urdu (Urdu script)
3. **`values-b+ur+Latn/strings.xml`**: Roman Urdu (Latin script for everyday shopkeepers)
4. **`values-es/strings.xml`**: Spanish
5. **`values-fr/strings.xml`**: French
6. **`values-hi/strings.xml`**: Hindi
7. **`values-ar/strings.xml`**: Arabic (RTL)
8. **`values-zh-rCN/strings.xml`**: Chinese Simplified

## Rules for New UI Features
Whenever adding new screens or UI fields in the future:
1. Define new string keys in `res/values/strings.xml` using `stringResource(R.string.key_name)` in Jetpack Compose.
2. Mirror all newly created string keys across all 8 locale `strings.xml` files (`values-ur`, `values-b+ur+Latn`, `values-es`, `values-fr`, `values-hi`, `values-ar`, `values-zh-rCN`).
3. Maintain plain language terminology (e.g., *Phone Khareedna*, *Phone Bechna*, *Khata*, *Mera Stock*).
