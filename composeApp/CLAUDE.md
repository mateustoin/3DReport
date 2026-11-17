# composeApp

Telas (`ui/`), repositórios (`data/`) e o que depende do sistema (`platform/`).
`commonMain` não usa `java.*`; isso fica em `jvmMain`.

## Dados (`data/`)
- Cada repositório é uma interface mais uma implementação sobre `data/store`
  (`RecordCollection` pra listas, `DocumentValue` pra documento único). Reaproveite
  `CatalogRepository.kt` pra cadastro novo.
- A gravação é atômica e em segundo plano (`WriteBehindFile`). Quem muda dado chama o
  repositório a partir da thread da tela; não escreva arquivo por fora.
- Nos testes, defina `threedreport.dataDir` em `@BeforeTest` e só depois construa o
  repositório. Nunca como campo da classe: o repositório lê a pasta ao ser criado.

## Telas (`ui/<tela>/`)
- Padrão pra tela nova ou reescrita: um `<Tela>ViewModel` em Kotlin puro que expõe
  **um** `StateFlow<<Tela>UiState>` e um fluxo de avisos; a `<Tela>Screen` só observa
  e envia eventos. Nada de conta de preço nem de regra de negócio em `@Composable`.
- O ViewModel recebe repositórios e `PlatformServices` por construtor, e os
  dispatchers também (os testes usam `Unconfined` e `FakePlatform`).
- Mensagem de erro não vem de `exception.message`: trate o tipo e escolha o texto.
- Telas antigas (Orçamento, Pedidos) ainda usam vários `StateFlow` separados. Elas
  adotam o padrão quando forem reescritas, não antes.

## Aparência
- Siga `docs/design-system.md`. Cor, espaçamento, raio e tipografia vêm do tema
  (`ui/theme/`); componente vem do kit. Não escreva `Color(0x...)`, `.dp` solto de
  layout nem `RoundedCornerShape(...)` numa tela.
- Antes de criar um componente, procure em `ui/components/` e no kit.
- Ícones só de `ui/icons/AppIcons.kt`. Sem emoji e sem caractere fazendo papel de
  ícone ("+", "▾", "⋮").
- Texto da interface vai em recurso traduzível, não em literal no código.
- No `strings.xml`, aspas e apóstrofo vão sem barra (`"%1$s"`, não `\"%1$s\"`): o Compose
  Multiplatform não trata o escape do Android, e a barra aparece na tela.

## Conferir
- `scripts/check.sh :composeApp:jvmTest`
- Mudou tela: gere as capturas com `scripts/check.sh :composeApp:renderScreenshots`
  e olhe as imagens.
