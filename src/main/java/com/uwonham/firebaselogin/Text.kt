package com.uwonham.firebaselogin

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


/**
 * TextText - A customizable text field component
 *
 * @param value Current text value
 * @param onValueChange Function called when text changes
 * @param modifier Optional modifier for the text field
 * @param label Optional label for the text field
 * @param placeholder Optional placeholder text
 * @param isError Whether the text field is in error state
 * @param singleLine Whether the text field should be single line
 */


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestText(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        isError = isError,
        singleLine = singleLine,

    )
}