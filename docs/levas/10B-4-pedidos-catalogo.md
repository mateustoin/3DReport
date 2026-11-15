# Leva 10B, parte 4: Pedidos e Catálogo na nova interface

> Especificação de trabalho. Direção visual: decisão 127
> ([design-system.md](../design-system.md)). Layout: decisões 129 e 132. Mockups de
> referência: kanban em [`design/referencia-pedidos.dc.html`](../design/referencia-pedidos.dc.html);
> tabela de pedidos e grade do catálogo no canvas de design (pranchas "D").

## Objetivo

Pedidos e Catálogo (hoje a mesma tela, `ui/history/`, com 757 linhas) reescritos uma
vez: visual novo, estado único, textos em recursos, e a visualização lembrada.
Nenhuma mudança no que é guardado nem nas contas.

## Fora de escopo

- `core/`, fórmulas e formato dos pedidos.
- O documento exportado (PDF e imagem) e o texto do WhatsApp: parte seguinte.
- Dashboard, Configurações e Sobre.
- Funcionalidade nova. Tudo o que a tela faz hoje continua: busca, filtros,
  arrastar no kanban, mover de situação, exportar, WhatsApp, copiar, baixar foto e
  STL, editar detalhes, editar cálculo, duplicar, prazo, guardar no catálogo,
  atualizar preço, transformar em pedido, vender, excluir, seleção múltipla e
  exportar catálogo.

## 1. Estado e textos

- `QuoteHistoryViewModel` expõe um `StateFlow<HistoryUiState>` e um fluxo de avisos
  `UiNotice` (o mesmo do Orçamento; `UserNotice` deixa de existir se ninguém mais
  usar). Mensagens em `UiText`, erro escolhido pelo tipo.
- Linhas, cartões, totais dos blocos de resumo e totais das colunas são calculados
  no ViewModel; a tela só desenha.
- Todos os textos de `ui/history/` em `strings.xml` (`orders_*`, `catalog_*`,
  `export_*`, `common_*`). Sem travessão, aspas sem barra, plural de verdade.
- Arquivos por assunto, cada um abaixo de ~300 linhas. Pedidos e Catálogo podem
  continuar dividindo o ViewModel, mas cada um ganha a sua tela.

## 2. Pedidos

- **Cabeçalho:** título, seletor Lista ou Kanban e o botão âmbar "Novo orçamento"
  (leva à tela de Orçamento). Sem subtítulo e sem a frase "Arraste um card...".
- **Blocos de resumo** (nas duas visualizações): em andamento, a receber, pra
  entregar hoje, atrasado. Os dois últimos usam as cores de prazo.
- **Filtros:** busca, situação e período numa linha só, com os campos do kit.
- **Kanban:** como no mockup. Coluna com fundo `column`, raio 12, faixa de 3 px na
  cor da situação, título e total. Cartão com nome, número e cliente, selo de prazo
  e valor; foto pequena quando houver. Menu de ações no hover ou foco. Arrastar
  continua mudando a situação.
- **Lista (decisão 132):** tabela com cabeçalho de colunas: número, peça, cliente,
  situação (ponto colorido e texto), prazo (selo) e valor à direita. Ordenada pelo
  prazo, sem prazo no fim. Ações no hover ou foco, cobrindo o valor, como nos
  cadastros. Clicar na linha abre os detalhes.
- **Visualização lembrada (decisão 129):** preferência gravada junto das outras
  preferências do app, nascendo em kanban. Campo novo neutro, com teste de
  compatibilidade com arquivo de preferências antigo.
- Situação e prazo sempre com texto, nunca só cor.

## 3. Catálogo

- **Cabeçalho:** título, seletor Grade ou Lista, "Exportar catálogo" como botão
  secundário e o botão âmbar "Novo produto" (leva ao Orçamento no modo produto).
- **Filtros:** busca e categoria (lista suspensa). O filtro de período sai do
  Catálogo se hoje não fizer sentido pra produto; se fizer, fica.
- **Grade (padrão, decisão 132):** cartões com foto em destaque (ou um fundo neutro
  sem ilustração quando não há foto), nome, categoria, preço e "Vender" como botão
  secundário. Demais ações no hover ou foco, num menu.
- **Lista:** linhas no molde dos cadastros, com miniatura, categoria, lucro e
  preço; "Vender" na linha.
- **Visualização lembrada:** mesma regra de Pedidos, nascendo em grade.
- **Seleção múltipla** pra exportar o catálogo continua, sem caixa de marcação
  sempre visível em cada cartão: entra num modo de seleção a partir do botão de
  exportar.
- O aviso de "atualizar preço" continua, sem âmbar.

## 4. Diálogos

Detalhes do pedido, atualizar preço, prazo vencido e confirmações usam `AppDialog`
e os campos do kit. Sem mudar o que cada um faz.

## 5. Testes e capturas

- Testes do ViewModel adaptados ao estado novo, sem perder caso. Novos: ordenação
  da tabela por prazo, totais dos blocos de resumo, preferência de visualização
  lembrada em cada tela, arquivo de preferências antigo abrindo no padrão.
- Capturas: pedidos em kanban e em lista, catálogo em grade e em lista, claro e
  escuro, e o diálogo de detalhes.

## Critérios de aceite

- `scripts/check.sh build` OK.
- Nenhum literal de texto, `Color(0x...)`, `.dp` de layout ou raio em `ui/history/`.
- Um único `StateFlow` público no ViewModel.
- `ls -la ~/.3dreport` igual antes e depois.

## Só dá pra conferir com o app aberto

Arrastar cartão entre colunas, hover e foco das ações, exportar e salvar arquivo,
abrir o WhatsApp, vender um produto e voltar, e a preferência sobrevivendo a fechar
e abrir o app.
