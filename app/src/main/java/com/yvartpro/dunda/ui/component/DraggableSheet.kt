package com.yvartpro.dunda.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Generic Modal Bottom Sheet
 *
 * @param title The title displayed at the top of the sheet.
 * @param onDismiss Called whn the sheet is dismissed.
 * @param content Dynamic content inside the sheet
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraggableSheet(
  title: String,
  onDismiss: () -> Unit,
  content: @Composable ColumnScope.()-> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = { onDismiss() },
    modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall, fontSize = 20.sp)
        Spacer(Modifier.height(2.dp))
      }
    content()
    }
}


