# Módulo `composeApp`

Interface do 3DReport com Compose Multiplatform. Depende de `core`.

## Conteúdo

| Caminho | Responsabilidade |
|---|---|
| `src/commonMain/.../App.kt` | Composable raiz: navegação por abas |
| `src/commonMain/.../data/` | Contratos dos repositórios (`expect class`), MVVM |
| `src/commonMain/.../platform/` | Contratos de capacidades de plataforma (`expect fun`): escolher/salvar arquivo, decodificar imagem, formatar data, copiar pra área de transferência, gerar PDF |
| `src/commonMain/.../ui/quote` | Tela de Orçamento (serviços opcionais + formulário de salvar) |
| `src/commonMain/.../ui/history` | Tela de Histórico: consultar, exportar (PDF/copiar, 1 ou vários), baixar foto, excluir |
| `src/commonMain/.../ui/filaments` | Tela de Filamentos (cadastro) |
| `src/commonMain/.../ui/printers` | Tela de Impressoras (cadastro) |
| `src/commonMain/.../ui/services` | Tela de Serviços opcionais (cadastro) |
| `src/commonMain/.../ui/settings` | Tela de Configurações gerais + marca d'água do PDF |
| `src/jvmMain/.../data/` | Persistência real (`actual class`): arquivos JSON + fotos em `~/.3dreport/` |
| `src/jvmMain/.../platform/` | Implementação real (`actual fun`): `java.awt.FileDialog`/`Toolkit`, Skia, `java.time`, Apache PDFBox |
| `src/jvmMain/.../Main.kt` | Entrada do desktop (janela) |

Detalhes da arquitetura MVVM e da persistência: [../docs/architecture.md](../docs/architecture.md).
Decisões de UI/UX: [../docs/decisions.md](../docs/decisions.md).

## Executar e empacotar

```bash
./gradlew :composeApp:run
./gradlew :composeApp:packageDistributionForCurrentOS   # .deb / .msi / .dmg
```
