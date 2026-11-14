# Leva 10B, parte 1: fundação de arquitetura

> Especificação de trabalho. Aprovada pelo responsável do projeto em 2026-11-14
> (decisão 130). Nada aqui muda o que quem usa o app vê ou os valores calculados.

## Objetivo

Deixar o código pronto pra receber a nova interface e a tradução sem retrabalho: o
`core` deixa de escrever frase pra tela, a pouca regra de preço que mora no
`QuoteViewModel` vai pro `core`, e o `App.kt` deixa de concentrar criação de
ViewModels e regras de edição.

## Fora de escopo

- Qualquer mudança visual, de texto ou de valor calculado.
- Mudar o formato dos dados, ou atualizar o kotlinx-serialization.
- Reescrever `QuoteViewModel` ou `QuoteScreen` além do necessário pra usar o que o
  `core` passa a oferecer. A reescrita vem na parte das telas.
- Extrair textos pra recursos (vem com cada tela).
- Biblioteca de navegação, ViewModel com ciclo de vida, injeção de dependências.

## A. Core (agente `engenheiro-core`)

### A1. Erro com tipo, sem frase

Hoje `QuoteViewModel.calculate` reconhece o erro "taxa do canal mais imposto chega a
100%" com `error.message.startsWith("A taxa do canal")`. Isso quebra na tradução.

- O `core` ganha uma hierarquia selada de erros de preço que a tela precisa explicar
  a quem usa (comece pelo caso das deduções que chegam a 100%, carregando os
  percentuais envolvidos; inclua outros casos só se já existirem hoje com frase
  escrita pra tela). O `PricingCalculator` lança esse tipo.
- A tela escolhe o texto pelo tipo. **O texto que aparece hoje continua idêntico**,
  só passa a ser montado em `composeApp`.
- `ChannelFeeSchedule` tem um aviso com frase pronta ("...Revise... em
  Configurações."): o `core` passa a devolver o que aconteceu como dado, e a frase
  vai pra `composeApp`.
- `web/.../WebCalculator.kt` repete a mesma checagem de 100% com o mesmo texto:
  passa a usar o tipo do `core`. A frase que a calculadora do site mostra não muda.
- Os `require` que são só proteção interna (valor negativo etc.) continuam como estão.

### A2. Montar o pedido no core

Em `QuoteViewModel.calculate` (por volta das linhas 1029 a 1085 na `main`), depois
de ler e validar os campos, há regra de negócio antes de chamar o
`PricingCalculator`: o preço da peça sai do preço fechado menos serviços e frete
cobrado; o frete grátis vira custo absorvido; produto do catálogo não tem frete nem
urgência; urgência só vale com acréscimo configurado.

- Essa derivação vira uma função do `core` (em `pricing/`) que recebe os valores já
  lidos e tipados e devolve o `Quote`. O nome e a forma ficam a seu critério; ela
  precisa ser fácil de chamar do `web` também.
- O ViewModel fica com: ler texto, validar formato, resolver ids do cadastro, e
  chamar essa função. A mensagem de "esse preço não cobre serviços e frete" continua
  sendo erro de campo do ViewModel, mas a comparação que decide isso é do `core`.
- A sugestão de preço arredondado (`QuoteViewModel`, por volta da linha 566) vira
  função pura do `core`, com teste.

### A3. Composição do preço

Os quatro grupos da barra do painel de resultado (material e máquina, trabalho e
serviços, taxas e impostos, lucro; ver `docs/design-system.md`) viram função do
`core` sobre `Quote`, com a garantia testada de que somam o total que o cliente paga.
O protótipo da interface tem uma versão provisória em
`composeApp/.../ui/quote/PriceComposition.kt` na branch `feat/nova-interface` (veja
com `git show feat/nova-interface:<caminho>`, junto com `PriceCompositionTest.kt`);
use como referência do comportamento esperado, não copie sem conferir a conta.

### Garantias

- Todos os testes existentes passam **sem mudar nenhum valor esperado**. Se um teste
  precisar mudar, é sinal de mudança de comportamento: pare e relate.
- O exemplo de referência de `docs/pricing-formulas.md` (R$ 17,02) continua igual.
- Testes novos pra cada função nova, em `core/src/commonTest` (rodam em JVM e JS).
- `docs/pricing-formulas.md` ganha a descrição da composição em quatro grupos.

## B. Estrutura do app (agente `construtor-ui`)

Tudo em `composeApp/src/commonMain/kotlin/com/threedreport/app/`. Não toca em
`ui/quote/QuoteViewModel.kt`, `ui/quote/QuoteScreen.kt` nem em `core/`: a parte A
mexe neles ao mesmo tempo, em outra cópia do repositório.

### B1. Destinos

`AppDestination` (enum em `ui/navigation/`) passa a carregar, além do rótulo e do
grupo, o ícone e o ícone do estado ativo, no lugar do `when` separado que hoje os
escolhe. O conteúdo de cada destino continua num único `when (destination)`, que o
compilador confere. Não criar registro nem mapa de telas.

### B2. ViewModels sob demanda

Uma classe comum (não `@Composable`), criada uma vez no `App()`, com um ViewModel
por propriedade `by lazy`. Hoje os 17 nascem de uma vez na abertura. Atenção ao que
precisa existir cedo: o que a barra lateral observa (pendência de Configurações, o
estado "Editando") e o que roda ao abrir (`UpdateViewModel.checkOnStart`, tema,
moeda). Se criar cedo for necessário pra manter o comportamento, crie cedo e diga
qual e por quê.

### B3. Regras de rascunho e edição fora do Composable

`App.kt` tem, dentro do `@Composable`, a lógica que decide o que acontece ao editar,
duplicar, vender ou guardar no catálogo quando há um rascunho ou uma edição em
andamento (`unlessEditing`, `unlessDraft`, `endEditing`, `cancelEditing`,
`saveEditing`, `originOf`, e os estados `pendingDiscard`, `pendingEndEditing`,
`confirmingEditDiscard`). Não tem teste.

- Vira uma classe comum que guarda o destino atual e esses estados (com
  `mutableStateOf` do runtime do Compose ou `StateFlow`, o que ficar mais simples de
  testar) e expõe as ações. O `App()` só observa e desenha os diálogos.
- Testes em `composeApp/src/jvmTest` cobrindo: editar sem rascunho; editar com edição
  já aberta e alterada (pede pra encerrar); duplicar com rascunho (pede pra
  descartar); cancelar edição com e sem alteração; salvar edição volta pra origem
  certa (Pedidos ou Catálogo).

### Garantias

- Comportamento idêntico: atalhos, diálogos, a volta pra tela de origem, o
  "Editando" na barra lateral, o ponto de pendência, o arrastar de G-code.
- `ScreenshotRenderer` continua funcionando (ele chama `App(...)` com `startAt` e
  `prepareQuote`).
- `scripts/check.sh :composeApp:jvmTest` passa; `scripts/check.sh ktlintCheck` passa.

## Decisões

| Nº | Decisão | Situação |
|---|---|---|
| 130 | Fundação de arquitetura antes da nova interface: erro com tipo no core, montagem do pedido e composição do preço no core, destinos num enum com um `when`, ViewModels sob demanda e regras de edição numa classe testada | aprovada em 2026-11-14 |

## Critérios de aceite

- `scripts/check.sh build` passa (inclui ktlint e todos os testes).
- Nenhum valor esperado de teste existente mudou.
- `grep -rn 'startsWith("A taxa' composeApp/` não acha nada.
- `App.kt` não cria mais os ViewModels um a um nem contém as regras de edição.
- Teste manual do responsável: abrir o app com dados reais copiados, navegar por
  todas as telas, editar e salvar um pedido, duplicar com rascunho aberto.
