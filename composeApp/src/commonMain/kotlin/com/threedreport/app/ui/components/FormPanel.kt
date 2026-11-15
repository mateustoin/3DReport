package com.threedreport.app.ui.components

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_cancel
import _3dreport.composeapp.generated.resources.common_save
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

/**
 * Estrutura das telas de cadastro: o [content] (cabeçalho e lista) numa coluna de até 760 de largura, com rolagem,
 * e, quando há [panel], o painel do formulário à direita, com a lista ainda visível ao lado. Abaixo de 960 de
 * largura útil o painel ocupa a área toda. Sem [panel], só a lista.
 */
@Composable
fun ListScreenLayout(panel: (@Composable () -> Unit)?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        when {
            panel == null -> ListColumn(content)

            maxWidth < AppDimens.sidePanelFullWidthBelow -> Box(Modifier.fillMaxSize()) { panel() }

            else -> Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight()) { ListColumn(content) }
                Box(Modifier.width(AppDimens.sidePanelWidth).fillMaxHeight()) { panel() }
            }
        }
    }
}

@Composable
private fun ListColumn(content: @Composable ColumnScope.() -> Unit) {
    val space = MaterialTheme.spacing
    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            modifier = Modifier
                .padding(start = space.xxl, end = space.xxl, top = AppDimens.screenTopPadding, bottom = space.xxl)
                .widthIn(max = AppDimens.listMaxWidth)
                .fillMaxWidth(),
            content = content,
        )
    }
}

/**
 * Painel do formulário dos cadastros: fundo `card`, borda `line` à esquerda, título, os campos (com rolagem) e o
 * rodapé com Salvar (âmbar) e Cancelar. Ao abrir, o foco vai pro primeiro campo, pra Tab seguir daí.
 * Dentro do [ListScreenLayout].
 */
@Composable
fun FormPanel(title: String, onSave: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    val firstField = remember { FocusRequester() }
    val scroll = rememberScrollState()
    LaunchedEffect(Unit) { runCatching { firstField.requestFocus() } }
    Row(modifier = modifier.fillMaxSize().background(colors.card)) {
        VerticalDivider(color = colors.line, thickness = AppDimens.borderWidth)
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            // Da altura do botão do cabeçalho da lista, pra o título ficar na mesma linha dele.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimens.screenTopPadding)
                    .height(AppDimens.controlHeight)
                    .padding(horizontal = space.xl),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(title, style = MaterialTheme.appType.panelTitle, color = MaterialTheme.colorScheme.onSurface)
            }
            // O divisor só aparece com o conteúdo rolado, pra o campo não passar por baixo do título sem separação.
            if (scroll.value > 0) HorizontalDivider(color = colors.line, thickness = AppDimens.borderWidth) else Spacer(Modifier.height(AppDimens.borderWidth))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .focusRequester(firstField)
                    .focusGroup()
                    .padding(horizontal = space.xl, vertical = space.xs),
                verticalArrangement = Arrangement.spacedBy(space.sm),
                content = content,
            )
            HorizontalDivider(color = colors.line, thickness = AppDimens.borderWidth)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = space.xl, vertical = space.md),
                horizontalArrangement = Arrangement.spacedBy(space.xs),
            ) {
                AppButton(stringResource(Res.string.common_save), onSave, kind = ButtonKind.Primary)
                AppButton(stringResource(Res.string.common_cancel), onCancel)
            }
        }
    }
}

/**
 * [FormSection] pro painel dos cadastros: numerada, com 24 de espaço da seção de cima (12 do painel mais 12 daqui).
 * A primeira cola no título do painel.
 */
@Composable
fun PanelSection(number: Int, title: String, help: HelpText? = null, content: @Composable () -> Unit) {
    FormSection(
        number = number,
        title = title,
        modifier = if (number > 1) Modifier.padding(top = MaterialTheme.spacing.sm) else Modifier,
        help = help,
        content = content,
    )
}
