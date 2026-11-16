package com.threedreport.app.ui.onboarding

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_error_negative
import _3dreport.composeapp.generated.resources.common_error_number_invalid
import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.app.ui.format.toInputText
import com.threedreport.app.ui.printers.PrinterPreset
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.uiText
import com.threedreport.core.model.PricingSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A impressora respondida no onboarding, aplicada na impressora de exemplo (a que o primeiro orçamento
 * usa): o modelo troca nome e consumo, o valor troca o preço da máquina. `null` em cada um mantém o do
 * exemplo.
 */
data class OnboardingPrinter(val preset: PrinterPreset?, val machinePrice: Double?)

/** O que o primeiro uso grava ao terminar: as configurações de custo e, se a pessoa respondeu, a impressora. */
data class OnboardingResult(val settings: PricingSettings, val printer: OnboardingPrinter?)

/** Os passos do primeiro uso: a impressora e, depois, os custos e a margem. */
enum class OnboardingStep { PRINTER, COSTS }

/**
 * Tudo o que o diálogo de primeiro uso mostra: o passo, as respostas como texto e o erro de cada uma. Os erros acompanham
 * o que se digita; [canContinue] diz se o passo atual está pronto pra seguir.
 */
data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.PRINTER,
    val preset: PrinterPreset? = null,
    val machinePriceText: String = "",
    val energyText: String = "",
    val laborText: String = "",
    val marginText: String = "",
    val machinePriceError: UiText? = null,
    val energyError: UiText? = null,
    val laborError: UiText? = null,
    val marginError: UiText? = null,
) {
    val stepNumber: Int get() = step.ordinal + 1

    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.PRINTER -> machinePriceError == null
            OnboardingStep.COSTS -> energyError == null && laborError == null && marginError == null
        }
}

/**
 * Quatro perguntas na primeira execução, em dois passos curtos (a impressora; os custos e a margem), pra o app não abrir
 * com a conta calibrada pra outra pessoa. Pedido ou produto não entra aqui (decisão 110): a escolha fica no próprio
 * Orçamento.
 *
 * São os números que mais mudam o preço e que ninguém adivinha por padrão: a impressora e quanto ela custou (decisão 108:
 * o exemplo de R$ 2.700 e 380 W errava pra quase todo mundo e ia direto pro primeiro orçamento), energia, o valor da
 * própria hora (que nasce zerado e, sem preencher, faz o trabalho sumir do preço) e a margem. Filamento fica de fora:
 * o cadastro padrão já é utilizável e a tela tem catálogo pronto.
 */
class OnboardingViewModel(private val currentSettings: PricingSettings) {

    private val state = MutableStateFlow(
        OnboardingUiState(
            energyText = currentSettings.energyPricePerKwh.toInputText(),
            marginText = (currentSettings.profitMargin * 100).toInputText(),
        ).validated(),
    )
    val uiState: StateFlow<OnboardingUiState> = state.asStateFlow()

    fun setPreset(preset: PrinterPreset?) = edit { it.copy(preset = preset) }
    fun setMachinePrice(text: String) = edit { it.copy(machinePriceText = text) }
    fun setEnergy(text: String) = edit { it.copy(energyText = text) }
    fun setLabor(text: String) = edit { it.copy(laborText = text) }
    fun setMargin(text: String) = edit { it.copy(marginText = text) }

    /** Segue pro passo dos custos, se as respostas do passo atual estão certas. */
    fun next() {
        if (state.value.canContinue) state.update { it.copy(step = OnboardingStep.COSTS) }
    }

    fun back() {
        state.update { it.copy(step = OnboardingStep.PRINTER) }
    }

    /** O que gravar ao terminar, ou `null` se alguma resposta ainda tem erro. */
    fun finish(): OnboardingResult? {
        val current = state.value
        if (!current.canContinue || current.machinePriceError != null) return null
        val energy = parseDecimal(current.energyText)
        val labor = if (current.laborText.isBlank()) 0.0 else parseDecimal(current.laborText)
        val margin = parseDecimal(current.marginText)
        val machinePrice = if (current.machinePriceText.isBlank()) null else parseDecimal(current.machinePriceText)
        return OnboardingResult(
            settings = currentSettings.copy(
                energyPricePerKwh = energy ?: currentSettings.energyPricePerKwh,
                laborRatePerHour = labor ?: 0.0,
                profitMargin = (margin ?: (currentSettings.profitMargin * 100)) / 100.0,
            ),
            printer = OnboardingPrinter(current.preset, machinePrice).takeIf { current.preset != null || machinePrice != null },
        )
    }

    private fun edit(transform: (OnboardingUiState) -> OnboardingUiState) {
        state.update { transform(it).validated() }
    }
}

/** Recalcula o erro de cada resposta: branco só vale onde a pergunta é opcional. */
private fun OnboardingUiState.validated(): OnboardingUiState {
    fun error(text: String, optional: Boolean): UiText? {
        if (optional && text.isBlank()) return null
        val value = parseDecimal(text) ?: return uiText(Res.string.common_error_number_invalid)
        return uiText(Res.string.common_error_negative).takeIf { value < 0 }
    }
    return copy(
        machinePriceError = error(machinePriceText, optional = true),
        energyError = error(energyText, optional = false),
        laborError = error(laborText, optional = true),
        marginError = error(marginText, optional = false),
    )
}
