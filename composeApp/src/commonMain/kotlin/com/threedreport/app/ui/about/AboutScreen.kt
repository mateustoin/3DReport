package com.threedreport.app.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.threedreport.app.ui.components.LinkText
import com.threedreport.app.ui.components.SectionTitle
import com.threedreport.app.ui.icons.AppIcons

const val GITHUB_URL = "https://github.com/mateustoin/3DReport"
const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/mateustoin"
private const val AUTHOR_NAME = "Mateus Antonio da Silva"

/** Largura de leitura das telas de texto (Sobre, Configurações). */
val READING_MAX_WIDTH = 720.dp

/**
 * Sobre (decisão 111): o que ficava no rodapé de toda tela (versão, autor, GitHub, apoio) e a ajuda, que
 * era um diálogo. [extra] é o que outras partes do app penduram aqui, como a verificação de
 * atualizações.
 */
@Composable
fun AboutScreen(version: String, extra: @Composable () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = READING_MAX_WIDTH).fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
            Text("3DReport v$version", style = MaterialTheme.typography.titleLarge)
            Text(
                "Aplicativo gratuito e de código aberto pra orçar e vender impressão 3D. Feito por $AUTHOR_NAME.",
                style = MaterialTheme.typography.bodyMedium,
            )
            LinkText(text = "Ver código-fonte no GitHub", url = GITHUB_URL)
            LinkText(text = "☕ Apoiar o projeto no Buy Me a Coffee", url = BUY_ME_A_COFFEE_URL)

            extra()

            SectionTitle(AppIcons.Info, "Como usar")
            Text(
                "• Orçamento: arraste o G-code pra janela (ou use \"Escolher arquivo\") e peso, tempo, foto, " +
                    "impressora e filamento vêm preenchidos. Escolha se é pedido de cliente ou produto do catálogo, " +
                    "o canal de venda e os serviços, e salve.",
            )
            Text(
                "• Pedidos: o que os clientes pediram, em lista ou Kanban. Exporte PDF, imagem pro WhatsApp ou o " +
                    "texto; \"Ações\" tem mover de etapa, editar, duplicar e guardar no catálogo.",
            )
            Text("• Catálogo: as peças que você oferece, com preço. \"Vender\" cria o pedido a partir do produto.")
            Text("• Dashboard: vendas pela data em que o cliente fechou, lucro por hora e o que mais vende.")
            Text("• Filamentos, Impressoras e Serviços: seus cadastros, usados no Orçamento.")
            Text(
                "• Configurações: custos (energia, sua hora, margem, imposto), canais de venda com a taxa de cada " +
                    "um, documentos pro cliente (marca, logo, contato), tema, moeda e backup " +
                    "(com cópia automática diária).",
            )

            SectionTitle(AppIcons.Tune, "Atalhos de teclado")
            Text("• Ctrl/Cmd+1 a 8: pula direto pra cada tela da barra lateral, na ordem (Orçamento a Configurações).")
            Text("• Ctrl/Cmd+S: salva o orçamento (no Orçamento, inclusive editando um pedido).")
            Text("• Ctrl/Cmd+N: começa um orçamento novo (pergunta antes se houver algo preenchido).")
            Text("• Esc: fecha o formulário aberto em Filamentos, Impressoras ou Serviços.")
        } }
    }
}
