# Sistema de design

A aparência do 3DReport, do app ao site. Vale pra toda tela, diálogo e documento
exportado. Direção "Oficina", escolhida pelo responsável do projeto em 2026-11-14
(decisão 127), a partir de três opções desenhadas.

- Mockups de referência: <https://claude.ai/artifact/NCG94gTm52RzYpRNhqv1w8>. O
  código das duas telas da direção escolhida está em
  [`design/referencia-orcamento.dc.html`](design/referencia-orcamento.dc.html) e
  [`design/referencia-pedidos.dc.html`](design/referencia-pedidos.dc.html) (não abrem
  sozinhos no navegador; servem pra consultar medidas, cores e estrutura).
- Ícone: [`design/icone-camadas.svg`](design/icone-camadas.svg) (decisão 128).

## A ideia

Uma ferramenta de trabalho com identidade: azul-marinho e âmbar, números grandes onde
o número é a resposta, e tudo o mais quieto. Quem abre o app deve ver primeiro
**quanto cobrar** e **o que entregar hoje**. Referências de acabamento: Mercury,
Shopify admin, Linear.

Três regras resolvem a maior parte das dúvidas:

1. **O âmbar é raro.** Marca a ação principal da tela, o lucro e o item ativo da
   navegação. Se aparece em mais de três lugares numa tela, está sobrando.
2. **O texto é curto.** Rótulo claro dispensa explicação. Ajuda longa fica sob
   demanda (dica ou "Saiba mais"), nunca em parágrafo fixo. Tela não tem subtítulo
   dizendo o que ela faz.
3. **Nada é desenhado na tela.** Cor, espaço, raio e tipo vêm dos tokens; componente
   vem do kit. Se falta algo, entra no kit.

## Cores

Os nomes são os papéis usados no código (`ui/theme/`). A coluna "M3" diz em qual papel
do `ColorScheme` do Material 3 a cor entra; as demais ficam num objeto próprio do tema.

### Superfícies e texto

| Papel | Claro | Escuro | M3 |
|---|---|---|---|
| `background` | `#F6F8FB` | `#0B1420` | background, surface |
| `card` | `#FFFFFF` | `#111D2C` | surfaceContainerLowest |
| `column` (fundo de coluna do kanban, de campo desligado) | `#ECF0F5` | `#0F1B2A` | surfaceContainer |
| `line` (borda e divisor) | `#DCE2EA` | `#22324A` | outlineVariant |
| `text` | `#0F1B2B` | `#E8EEF5` | onBackground, onSurface |
| `textMuted` | `#536173` | `#9DAEC2` | onSurfaceVariant |
| `primary` (links, foco, seleção) | `#0B5FA8` | `#7DB8F5` | primary |
| `onPrimary` | `#FFFFFF` | `#06213A` | onPrimary |
| `tint` (fundo de selo neutro e de marcador) | `#E6EFF8` | `#16283D` | primaryContainer |
| `error` | `#B3261E` | `#FFB4AB` | error |

### Marca (iguais nos dois temas)

| Papel | Valor | Uso |
|---|---|---|
| `navy` | `#0C2742` (claro), `#081526` (escuro) | Barra lateral e painel de resultado |
| `navyRaised` | `#143657` (claro), `#12263D` (escuro) | Item ativo e bloco dentro do azul-marinho |
| `onNavy` | `#EAF1F8` | Texto sobre o azul-marinho |
| `onNavyMuted` | `#A9BCD0` | Texto secundário sobre o azul-marinho |
| `amber` | `#F2A541` | Ação principal, lucro, ícone do item ativo |
| `onAmber` | `#2A1A02` | Texto sobre o âmbar |

O âmbar como **texto sobre fundo claro** não tem contraste: nesse caso use `#C97A12`.

### Situação do pedido e prazo

| Situação | Claro | Escuro |
|---|---|---|
| Orçado | `#8292A6` | `#8292A6` |
| Aprovado | `#0B5FA8` | `#7DB8F5` |
| Imprimindo | `#C97A12` | `#F2A541` |
| Pronto | `#1F7A4D` | `#6FCF9F` |
| Entregue, Cancelado | `textMuted` | `textMuted` |

| Prazo | Fundo claro / texto | Fundo escuro / texto |
|---|---|---|
| No prazo | `#E3ECF5` / `#3B4A5C` | `#1C2C40` / `#B7C6D8` |
| Hoje | `#FCEBD0` / `#7A4706` | `#4A3210` / `#FFD79A` |
| Atrasado | `#FBDAD6` / `#8E1B13` | `#55201B` / `#FFC2BB` |

Situação e prazo nunca dependem só da cor: sempre vêm com o texto ("Atrasado 2 dias").

### Composição do preço

A barra e a legenda usam quatro grupos, nesta ordem: material e máquina `#7DB8F5`,
trabalho e serviços `#4C8FD0`, taxas e impostos `#6C7F95`, lucro `#F2A541`. O
detalhamento item a item fica na conta completa, não na barra.

## Tipografia

Duas famílias, embutidas no app (licença OFL), pra aparência ser a mesma em qualquer
sistema:

- **Space Grotesk** (500, 600, 700): marca, títulos de tela e de seção, e números em
  destaque.
- **Inter** (400, 500, 600): todo o resto.

Números em dinheiro, peso e tempo usam **algarismos tabulares** (`tnum`) da própria
fonte. Não se usa fonte monoespaçada.

| Estilo | Família | Tamanho / peso | Uso |
|---|---|---|---|
| `displayPrice` | Space Grotesk | 52 / 700 | O preço no painel de resultado |
| `headline` | Space Grotesk | 26 / 700 | Título da tela |
| `statValue` | Space Grotesk | 22 a 24 / 600 | Números dos blocos de resumo |
| `sectionTitle` | Space Grotesk | 16 / 600 | Título de seção e de coluna |
| `body` | Inter | 14 / 400 | Texto e valor de campo |
| `bodyStrong` | Inter | 14 / 600 | Nome do item numa lista ou cartão |
| `label` | Inter | 12 / 500 | Rótulo de campo |
| `caption` | Inter | 13 / 400 | Texto secundário |
| `overline` | Inter | 11 / 500, maiúsculas, espaçamento 0,08em | Grupo da barra lateral, rótulo sobre número |

## Espaço e forma

- **Escala de espaço:** 4, 8, 12, 16, 20, 24, 32. Nenhum valor fora dela.
  Entre campos de um grupo: 12. Entre seções: 24. Margem da tela: 32 nas laterais, 26
  no topo.
- **Raios:** 6 (selo), 8 (botão, campo, item da navegação), 10 (cartão, bloco), 12
  (coluna do kanban, diálogo). O ícone do app usa 22% do lado.
- **Borda:** 1 px em `line`. Sombra só em menu, dica e diálogo.
- **Alturas:** campo e botão 40; botão principal do painel de resultado 46; item da
  navegação 36.
- **Largura:** a barra lateral tem 212; o painel de resultado, 380. Formulário usa
  grade de três colunas e não passa de 760 de largura.

## Componentes do kit

| Componente | Regra |
|---|---|
| Barra lateral | Fundo `navy` nos dois temas. Marca "3D" em branco e "Report" em âmbar. Grupos em `overline`. Item ativo: fundo `navyRaised`, texto branco em 600 e ícone âmbar |
| Cabeçalho de tela | Título em `headline` à esquerda; à direita, o seletor da tela e a ação principal. Sem subtítulo |
| Seção de formulário | Marcador redondo numerado (24, fundo `tint`, número em `primary`) e título em `sectionTitle`. Sem ícone decorativo |
| Campo | Rótulo em `label` acima, caixa de 40 com borda `line` e fundo `card`. Borda `primary` ao passar o mouse, anel de 2 px no foco. Erro: borda `error` e mensagem curta abaixo |
| Botão | Principal: fundo `amber`, texto `onAmber` em 600, um por tela. Secundário: fundo `card`, borda `line`. Sobre o azul-marinho: transparente com borda `navyRaised` |
| Seletor de duas opções | Segmentado com borda `line`; a opção ativa tem fundo `primary` |
| Painel de resultado | Coluna fixa à direita, fundo `navy`: rótulo em `overline`, preço em `displayPrice`, dois blocos (lucro em âmbar e mínimo sem prejuízo), barra de composição, e as ações no pé |
| Bloco de resumo | Número em `statValue` e legenda em `caption`, fundo `card`, borda `line` |
| Coluna do kanban | Fundo `column`, raio 12, faixa de 3 px no topo na cor da situação, título e total |
| Cartão de pedido | Fundo `card`, borda `line`: nome em `bodyStrong`, número e cliente em `caption`, selo de prazo e valor |
| Linha de lista | Pra cadastros: uma linha por item, ações ao passar o mouse ou num menu. Não é cartão |
| Estado vazio | Uma frase e a ação que resolve. Sem ilustração |

## Comportamento

- **Uma ação principal por tela**, em âmbar. As demais são secundárias ou vão pra um menu.
- **Foco visível** em tudo que recebe teclado. Tab percorre os campos na ordem de leitura.
- **Botão só de ícone** tem descrição pra leitor de tela e dica ao passar o mouse.
- **Movimento:** transições de 120 a 180 ms em hover, seleção e troca de conteúdo.
  Nada se move sem a pessoa ter feito algo.
- **Preferência lembrada:** uma escolha de visualização (por exemplo, lista ou kanban
  em Pedidos, decisão 129) vale até a pessoa trocar de novo.
- **Tema escuro** não é o claro invertido: as superfícies sobem de tom
  (`background` < `column` < `card`), e a barra lateral continua a mais escura.

## O que não fazer

- Card em volta de tudo, ou card dentro de card.
- Ícone decorando título de seção.
- Parágrafo de ajuda fixo embaixo de campo, ou frase explicando a tela.
- Mais de um botão âmbar por tela.
- Gradiente, emoji, ou caractere fazendo papel de ícone ("+", "▾", "⋮").
- Rótulos empilhados no formato "Campo: valor".
- Cor, `dp` de layout, raio ou tamanho de fonte escritos direto numa tela.

## Ideias guardadas

- **Etiqueta de preço feita de camadas**
  ([`design/ideia-etiqueta.svg`](design/ideia-etiqueta.svg)). Segunda opção de ícone
  na escolha de 2026-11-14. É a única que fala de venda, não só de impressão. Pode
  servir como símbolo de uma área (preços, catálogo), selo no site ou ícone de uma
  edição futura.
