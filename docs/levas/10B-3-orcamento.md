# Leva 10B, parte 3: Orçamento na nova interface

> Especificação de trabalho. A aparência já foi aprovada no protótipo em Compose
> (decisão 127, [design-system.md](../design-system.md)): formulário à esquerda,
> painel de resultado azul-marinho de 380 à direita, barra de composição do preço.
> Esta parte não redesenha nada: termina a reescrita por dentro.

## Objetivo

A tela de Orçamento deixa de ser protótipo: mesmo visual, com textos em recursos
traduzíveis, um estado único, erros por tipo e arquivos menores. Quem usa não deve
notar diferença de comportamento nem de preço.

## Fora de escopo

- Qualquer mudança de fórmula, de `core/` ou do formato dos dados.
- Os botões "Negociar" e "Enviar pro cliente" do mockup: são funcionalidade nova
  (hoje a negociação é o campo de preço combinado, e o envio é a exportação em
  Pedidos). Ficam pra uma leva própria, com decisão.
- Pedidos, Catálogo, Dashboard, Configurações, Sobre e o documento exportado.
- Os rótulos em português que ainda moram no `core` e o prefixo
  `SavedQuote.AUTO_NAME_PREFIX`: item separado da 10B.

## 1. Estado único

- `QuoteViewModel` passa a expor **um** `StateFlow<QuoteUiState>` e um fluxo de
  avisos, no padrão de `composeApp/CLAUDE.md` e dos cadastros. Somem os fluxos
  públicos soltos (`input`, `saveForm`, `importing`, `stlPreview`,
  `collapsedPrintIds`, `notice` e os cadastros repassados).
- `QuoteUiState` agrupa, sem achatar: `input` (o que a pessoa digitou), `form`
  (salvar, cliente, foto, STL), `result` (a conta), `catalog` (filamentos,
  impressoras, serviços, insumos, canais, clientes e configurações que a tela lê),
  `importing`, `stlPreview` e `collapsedPrintIds`.
- A conta continua sendo feita pelo `core` (`OrderPricing.quote`); o ViewModel só
  monta a entrada e guarda o resultado.
- `AppNavigator` e `App.kt` leem a operação em curso pelo estado novo.

## 2. Erros e mensagens por tipo

- `QuoteResult.errorMessage`, `QuoteResult.fieldErrors`,
  `SaveQuoteFormState.blockedMessage` e `SaveQuoteFormState.photoError` deixam de
  ser `String` e passam a ser `UiText`. Nenhuma frase é montada no ViewModel.
- A mensagem da importação de G-code (`GCodeImporter.message`) vira um resultado
  estruturado (o que foi lido, o que foi mantido, impressora reconhecida ou não), e
  a tela escolhe o texto com plural de verdade.
- Os avisos (`UserNotice`) usam `UiText`.

## 3. Textos em recursos

- Todos os textos da pasta `ui/quote/` vão pra `strings.xml` com chaves `quote_*`,
  reaproveitando `common_*`. São cerca de 370 literais hoje.
- Regras de sempre: sem travessão, frases curtas, aspas sem barra, plural com
  `pluralStringResource`, ajuda longa em `HelpText`.
- Os textos mudam o mínimo: a ideia é mover, não reescrever. Texto que hoje
  contraria o sistema de design (parágrafo fixo, "Campo: valor") é ajustado e
  listado no relatório.
- "esgotado" vira "acabou", igual aos cadastros.

## 4. Arquivos menores

`QuoteScreen.kt` (887 linhas) fica só com a moldura da tela. O resto sai pra
arquivos por assunto, cada um abaixo de ~300 linhas:

| Arquivo | Conteúdo |
|---|---|
| `QuoteScreen.kt` | Moldura, cabeçalho, diálogos de confirmação |
| `QuoteExtrasSection.kt` | Mão de obra, serviços e insumos |
| `QuoteSaleSection.kt` | Canal, frete, quantidade, preço combinado |
| `QuoteDetailsSection.kt` | Nome, cliente, prazo, categoria, foto e STL |
| `PrintCard.kt`, `QuoteResultPanel.kt` | Como estão, divididos se passarem de ~300 |

- `ServiceChargeSelector` passa a usar o seletor do kit (sem `FilterChip`, sem
  `.dp` solto).
- Nenhum `Color(0x...)`, `.dp` de layout, raio ou tamanho de fonte na pasta.
- Componente que faltar entra no kit, não na tela.

## 5. Testes

- Os testes existentes (`QuoteViewModelTest`, `MultiPrintQuoteTest`,
  `QuoteLeva10Test`, `QuotePricingErrorMessageTest`) são adaptados ao estado novo
  sem perder nenhum caso nem mudar valor esperado de preço. Onde comparavam frase,
  passam a comparar o tipo ou a chave do texto.
- O exemplo de referência de R$ 17,02 continua passando.
- Volta o teste perdido na fundação: "Ctrl+S sem edição aberta salva a aba".

## Ordem de trabalho

1. **Estado e erros** (seções 1, 2 e 5): ViewModel, importador e testes. A tela é
   ajustada só o necessário pra compilar. Um commit.
2. **Tela e textos** (seções 3 e 4). Um commit.
3. Capturas de Orçamento em claro e escuro, comparadas com as aprovadas: devem ser
   iguais, salvo os ajustes de texto listados.

## Critérios de aceite

- `scripts/check.sh build` OK.
- `grep` por `Text("` com literal e por aspas com letras acentuadas em `ui/quote/`
  não acha nada.
- `QuoteViewModel` tem um único `StateFlow` público.
- Capturas de Orçamento sem diferença visual em relação às aprovadas.
- `ls -la ~/.3dreport` igual antes e depois.

## Só dá pra conferir com o app aberto

Arrastar G-code, atalhos (Ctrl+S, Esc), diálogos de confirmação, "Ver pedidos"
depois de vender, editar e duplicar um pedido, vender um produto do catálogo.
