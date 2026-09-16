# Módulo `composeApp`

Interface do 3DReport com Compose Multiplatform. Depende de `core`.

## Conteúdo

| Caminho | Responsabilidade |
|---|---|
| `src/commonMain/.../App.kt` | Composable raiz: navegação por abas + rodapé fixo (versão, autor, GitHub, doação, ajuda) |
| `src/commonMain/.../AppVersion.kt` | Constante `APP_VERSION` (SemVer, mantida em sincronia com `gradle.properties`) |
| `src/commonMain/.../data/` | Contratos dos repositórios (`expect class`), MVVM |
| `src/commonMain/.../platform/` | Contratos de capacidades de plataforma (`expect fun`): escolher/salvar arquivo, decodificar imagem, formatar data, copiar pra área de transferência, gerar PDF, abrir URL no navegador |
| `src/commonMain/.../ui/components/LinkText.kt` | `Text` clicável (sublinhado) que abre uma URL via `platform/openUrl` |
| `src/commonMain/.../ui/components/ConfirmDialog.kt` | Diálogo genérico de confirmação, usado antes de excluir (Filamentos, Impressoras, Serviços, Histórico) |
| `src/commonMain/.../ui/format/PercentFormat.kt` | `Double.toPercentText()` — formata fração decimal como percentual (ex.: `0.15` → `"15%"`) |
| `src/commonMain/.../ui/format/WeightFormat.kt` | `Double.toWeightText()` — formata gramas com vírgula decimal (ex.: `12.3` → `"12,3 g"`) |
| `src/commonMain/.../ui/quote` | Tela de Orçamento (serviços opcionais, checkbox de marketplace, formulário de salvar com link do modelo clicável) |
| `src/commonMain/.../ui/history` | Tela de Histórico: consultar, exportar (PDF/copiar, 1 ou vários), baixar foto, excluir (com confirmação), link do modelo clicável, peso da peça (uso interno) |
| `src/commonMain/.../ui/filaments` | Tela de Filamentos (cadastro) |
| `src/commonMain/.../ui/printers` | Tela de Impressoras (cadastro) |
| `src/commonMain/.../ui/services` | Tela de Serviços opcionais (cadastro) |
| `src/commonMain/.../ui/settings` | Tela de Configurações gerais (incl. taxa de marketplace) + marca d'água do PDF |
| `src/jvmMain/.../data/` | Persistência real (`actual class`): arquivos JSON + fotos em `~/.3dreport/` |
| `src/jvmMain/.../platform/` | Implementação real (`actual fun`): `java.awt.FileDialog`/`Toolkit`/`Desktop`, Skia, `java.time`, Apache PDFBox |
| `src/jvmMain/.../Main.kt` | Entrada do desktop (janela) |

Detalhes da arquitetura MVVM e da persistência: [../docs/architecture.md](../docs/architecture.md).
Decisões de UI/UX: [../docs/decisions.md](../docs/decisions.md).

## Executar e empacotar

```bash
./gradlew :composeApp:run
./gradlew :composeApp:packageDistributionForCurrentOS   # .deb / .msi / .dmg
```
