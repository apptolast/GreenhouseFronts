package com.apptolast.greenhousefronts.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apptolast.greenhousefronts.data.feedback.FeedbackCategory
import com.apptolast.greenhousefronts.data.feedback.FeedbackDraft
import com.apptolast.greenhousefronts.presentation.ui.theme.GreenhouseTheme
import com.apptolast.greenhousefronts.presentation.viewmodel.SendSuggestionEvent
import com.apptolast.greenhousefronts.presentation.viewmodel.SendSuggestionUiState
import com.apptolast.greenhousefronts.presentation.viewmodel.SendSuggestionViewModel
import greenhousefronts.composeapp.generated.resources.Res
import greenhousefronts.composeapp.generated.resources.suggestion_back_cd
import greenhousefronts.composeapp.generated.resources.suggestion_category_bug
import greenhousefronts.composeapp.generated.resources.suggestion_category_improvement
import greenhousefronts.composeapp.generated.resources.suggestion_category_label
import greenhousefronts.composeapp.generated.resources.suggestion_category_other
import greenhousefronts.composeapp.generated.resources.suggestion_category_suggestion
import greenhousefronts.composeapp.generated.resources.suggestion_description_label
import greenhousefronts.composeapp.generated.resources.suggestion_description_placeholder
import greenhousefronts.composeapp.generated.resources.suggestion_exit_dialog_body
import greenhousefronts.composeapp.generated.resources.suggestion_exit_dialog_cancel
import greenhousefronts.composeapp.generated.resources.suggestion_exit_dialog_confirm
import greenhousefronts.composeapp.generated.resources.suggestion_exit_dialog_title
import greenhousefronts.composeapp.generated.resources.suggestion_intro_body
import greenhousefronts.composeapp.generated.resources.suggestion_intro_title
import greenhousefronts.composeapp.generated.resources.suggestion_send_button
import greenhousefronts.composeapp.generated.resources.suggestion_send_success
import greenhousefronts.composeapp.generated.resources.suggestion_sending
import greenhousefronts.composeapp.generated.resources.suggestion_title_label
import greenhousefronts.composeapp.generated.resources.suggestion_title_placeholder
import greenhousefronts.composeapp.generated.resources.suggestion_topbar_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SendSuggestionScreen(
    viewModel: SendSuggestionViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val successMessage = stringResource(Res.string.suggestion_send_success)
    // Used to fire-and-forget the success snackbar in parallel with the
    // auto-navigate-back delay. `showSnackbar` is a suspend fn that returns
    // when the snackbar dismisses (~4s default), so calling it sequentially
    // would block the navigate-back for 4s — too long.
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SendSuggestionEvent.NavigateBack -> onNavigateBack()

                SendSuggestionEvent.SentSuccessfully -> {
                    scope.launch { snackbarHostState.showSnackbar(successMessage) }
                    delay(1500)
                    onNavigateBack()
                }

                is SendSuggestionEvent.SendFailed -> {
                    // Repository already produced a translated, user-friendly
                    // string — pipe it straight to the snackbar. Block the
                    // collect loop here so a quick repeated tap on Enviar
                    // doesn't queue multiple identical snackbars.
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    SendSuggestionContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackPressed = viewModel::onBackPressed,
        onCategoryChange = viewModel::setCategory,
        onTitleChange = viewModel::setTitle,
        onDescriptionChange = viewModel::setDescription,
        onSend = viewModel::send,
        onDismissExitDialog = viewModel::dismissExitConfirm,
        onConfirmExit = viewModel::confirmExit,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SendSuggestionContent(
    uiState: SendSuggestionUiState,
    snackbarHostState: SnackbarHostState,
    onBackPressed: () -> Unit,
    onCategoryChange: (FeedbackCategory) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismissExitDialog: () -> Unit,
    onConfirmExit: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.suggestion_topbar_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.suggestion_back_cd),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(Res.string.suggestion_intro_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.suggestion_intro_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            CategoryDropdown(
                selected = uiState.draft.category,
                onCategoryChange = onCategoryChange,
                enabled = !uiState.isSending,
            )

            Spacer(Modifier.height(16.dp))

            // Title — multiline but capped at 3 lines so it stays a "title" rather
            // than swallowing the description's job.
            OutlinedTextField(
                value = uiState.draft.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(Res.string.suggestion_title_label)) },
                placeholder = { Text(stringResource(Res.string.suggestion_title_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isSending,
            )

            Spacer(Modifier.height(16.dp))

            // Description — sized to clearly invite a long answer (8 lines visible).
            OutlinedTextField(
                value = uiState.draft.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(Res.string.suggestion_description_label)) },
                placeholder = { Text(stringResource(Res.string.suggestion_description_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp),
                minLines = 8,
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isSending,
            )

            Spacer(Modifier.height(24.dp))

            // While `isSending` we keep the button visually enabled (so the
            // primary background and the spinner stay clearly visible) — the
            // ViewModel's `send()` short-circuits internally on a duplicate
            // tap because `canSend` already excludes the in-flight case.
            // When `canSend` is false because of validation (title too short,
            // empty description), `isSending` is also false → the button
            // correctly renders disabled/greyed.
            Button(
                onClick = onSend,
                enabled = uiState.canSend || uiState.isSending,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (uiState.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = stringResource(Res.string.suggestion_sending),
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(Res.string.suggestion_send_button),
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (uiState.showExitConfirm) {
        AlertDialog(
            onDismissRequest = onDismissExitDialog,
            title = { Text(stringResource(Res.string.suggestion_exit_dialog_title)) },
            text = { Text(stringResource(Res.string.suggestion_exit_dialog_body)) },
            confirmButton = {
                TextButton(onClick = onConfirmExit) {
                    Text(
                        text = stringResource(Res.string.suggestion_exit_dialog_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExitDialog) {
                    Text(stringResource(Res.string.suggestion_exit_dialog_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: FeedbackCategory,
    onCategoryChange: (FeedbackCategory) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
    ) {
        OutlinedTextField(
            value = categoryLabel(selected),
            onValueChange = { /* read-only */ },
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(Res.string.suggestion_category_label)) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                )
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            FeedbackCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(categoryLabel(category)) },
                    onClick = {
                        expanded = false
                        onCategoryChange(category)
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun categoryLabel(category: FeedbackCategory): String = when (category) {
    FeedbackCategory.BUG -> stringResource(Res.string.suggestion_category_bug)
    FeedbackCategory.SUGGESTION -> stringResource(Res.string.suggestion_category_suggestion)
    FeedbackCategory.IMPROVEMENT -> stringResource(Res.string.suggestion_category_improvement)
    FeedbackCategory.OTHER -> stringResource(Res.string.suggestion_category_other)
}

@Preview
@Composable
private fun PreviewSendSuggestionEmpty() {
    GreenhouseTheme(darkTheme = true) {
        SendSuggestionContent(
            uiState = SendSuggestionUiState(
                isLoadingProfile = false,
                email = "pablo@invernaderos.com",
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBackPressed = {},
            onCategoryChange = {},
            onTitleChange = {},
            onDescriptionChange = {},
            onSend = {},
            onDismissExitDialog = {},
            onConfirmExit = {},
        )
    }
}

@Preview
@Composable
private fun PreviewSendSuggestionFilled() {
    GreenhouseTheme(darkTheme = true) {
        SendSuggestionContent(
            uiState = SendSuggestionUiState(
                draft = FeedbackDraft(
                    category = FeedbackCategory.BUG,
                    title = "El gráfico de temperatura no se actualiza al volver de background",
                    description = "Cuando dejo la app en segundo plano más de 5 minutos y vuelvo, la curva de temperatura se queda congelada hasta que hago pull-to-refresh manualmente.",
                ),
                isLoadingProfile = false,
                email = "pablo@invernaderos.com",
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBackPressed = {},
            onCategoryChange = {},
            onTitleChange = {},
            onDescriptionChange = {},
            onSend = {},
            onDismissExitDialog = {},
            onConfirmExit = {},
        )
    }
}

@Preview
@Composable
private fun PreviewSendSuggestionExitDialog() {
    GreenhouseTheme(darkTheme = true) {
        SendSuggestionContent(
            uiState = SendSuggestionUiState(
                draft = FeedbackDraft(
                    category = FeedbackCategory.SUGGESTION,
                    title = "Modo oscuro automático",
                    description = "Estaría bien que la app cambiase entre claro/oscuro siguiendo el sistema.",
                ),
                isLoadingProfile = false,
                showExitConfirm = true,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBackPressed = {},
            onCategoryChange = {},
            onTitleChange = {},
            onDescriptionChange = {},
            onSend = {},
            onDismissExitDialog = {},
            onConfirmExit = {},
        )
    }
}
