# Módulo `composeApp`

Interface do 3DReport com Compose Multiplatform. Depende de `core`.

## Conteúdo

| Caminho | Responsabilidade |
|---|---|
| `src/commonMain/.../App.kt` | Composable raiz: navegação por abas |
| `src/commonMain/.../data/` | Contratos dos repositórios (`expect class`), MVVM |
| `src/commonMain/.../ui/quote` | Tela de Orçamento |
| `src/commonMain/.../ui/filaments` | Tela de Filamentos (cadastro) |
| `src/commonMain/.../ui/printers` | Tela de Impressoras (cadastro) |
| `src/commonMain/.../ui/settings` | Tela de Configurações gerais |
| `src/jvmMain/.../data/` | Persistência real (`actual class`): arquivos JSON em `~/.3dreport/` |
| `src/jvmMain/.../Main.kt` | Entrada do desktop (janela) |

Detalhes da arquitetura MVVM e da persistência: [../docs/architecture.md](../docs/architecture.md).
Decisões de UI/UX: [../docs/decisions.md](../docs/decisions.md).

## Executar e empacotar

```bash
./gradlew :composeApp:run
./gradlew :composeApp:packageDistributionForCurrentOS   # .deb / .msi / .dmg
```
