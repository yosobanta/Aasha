package com.example.aasha.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AashaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: AashaButtonStyle = AashaButtonStyle.Primary
) {
    when (style) {
        AashaButtonStyle.Primary -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(52.dp),
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(text = text, style = MaterialTheme.typography.titleMedium)
            }
        }
        AashaButtonStyle.Secondary -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.height(52.dp),
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                )
            ) {
                Text(text = text, style = MaterialTheme.typography.titleMedium)
            }
        }
        AashaButtonStyle.Tertiary -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.height(52.dp),
                enabled = enabled
            ) {
                Text(text = text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

enum class AashaButtonStyle {
    Primary, Secondary, Tertiary
}
