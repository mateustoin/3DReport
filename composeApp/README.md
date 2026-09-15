# Módulo `composeApp`

Interface do 3DReport com Compose Multiplatform. Depende de `core`.

## Conteúdo

| Caminho | Responsabilidade |
|---|---|
| `src/commonMain/.../App.kt` | Composable raiz, compartilhado entre plataformas |
| `src/jvmMain/.../Main.kt` | Entrada do desktop (janela) |

> A tela atual é **provisória**: exibe um orçamento de exemplo calculado pelo
> `core` apenas para validar a integração. A UI definitiva será proposta e
> aprovada antes de ser implementada (ver [../docs/decisions.md](../docs/decisions.md)).

## Executar e empacotar

```bash
./gradlew :composeApp:run
./gradlew :composeApp:packageDistributionForCurrentOS   # .deb / .msi / .dmg
```
