# Leva 10B, parte 6: Dashboard na nova interface

> Especificação de trabalho. Direção visual: decisão 127
> ([design-system.md](../design-system.md)). Layout: decisão 133. Mockup de
> referência no canvas de design (prancha "Dashboard · 1 · Blocos").

## Objetivo

O Dashboard reescrito com o sistema de design, mostrando os mesmos números de hoje.
Nenhuma conta nova: tudo vem de `QuoteSummary` e `CatalogSummary`, do `core`.

## Fora de escopo

- Gráfico de vendas ao longo do tempo (decisão 133) e qualquer número novo.
- `core/`, fórmulas e formato dos dados.

## Tela

- **Cabeçalho:** título "Dashboard" e o período como seletor segmentado do kit
  (7 dias, 30 dias, este mês, mês passado, este ano, tudo). Se não couber na
  largura, vira lista suspensa. Sem botão âmbar: a tela não tem ação principal.
- **Quatro blocos de resumo** (`SummaryBlock`): vendas, total vendido, lucro e
  lucro por hora de máquina. O lucro usa o tom de âmbar pra texto sobre fundo claro
  (é um dos três usos do âmbar). Sem dado, o bloco mostra um traço e não some.
- **Duas colunas abaixo:**
  - à esquerda, "Peças que mais deram lucro" com a tabela do kit: peça, vendas,
    lucro e lucro por hora, valores à direita com algarismos tabulares;
  - à direita, blocos de pares rótulo e valor:
    - **Em aberto:** orçamentos esperando resposta (quantidade e valor) e a parcela
      dos orçamentos que virou venda;
    - **Quanto cada hora rendeu:** hora de máquina e hora de trabalho, com a hora
      configurada como referência quando houver;
    - **Mais usados:** filamento mais usado e descontos dados;
    - **Catálogo** e **quem mais recebeu desconto**, quando houver dado, no mesmo
      formato (hoje são seções da tela e continuam aparecendo).
- Em janela estreita as duas colunas viram uma.
- Explicações de como cada número é calculado saem dos parágrafos fixos e vão pra
  `HelpHint` ao lado do título do bloco.
- **Estado vazio:** uma frase e a ação que resolve ("Novo orçamento" ou "Ver
  pedidos"), nos dois casos de hoje: nada salvo no período, e nada vendido no
  período.
- Um bloco novo do kit pra par rótulo e valor, se o kit ainda não tiver.

## Estado, textos e testes

- `DashboardViewModel` com um `StateFlow<DashboardUiState>`; valores já formatados
  ou prontos pra formatar, sem conta na tela.
- Textos em `strings.xml` (`dashboard_*`), plural de verdade ("1 negociado",
  "2 negociados"), sem travessão.
- Testes do ViewModel adaptados sem perder caso; novo teste pro estado vazio de
  cada tipo e pra troca de período.
- Capturas: com dados (claro e escuro) e os dois estados vazios.

## Critérios de aceite

- `scripts/check.sh build` OK.
- Nenhum literal de texto, `Color(0x...)`, `.dp` de layout ou raio em
  `ui/dashboard/`.
- Os números das capturas batem com os de antes da reescrita pro mesmo conjunto de
  dados.
- `ls -la ~/.3dreport` igual antes e depois.

## Ajustes depois da revisão

Revisão de design de 2026-11-16 (decisão [135](../decisions/135.md), que complementa a 133): sem vendas no período os quatro blocos continuam zerados; "Novo orçamento" é âmbar só na primeira vez; "Quanto cada hora rendeu" só com hora de trabalho; "sem dado" em meia-risca apagada; lucro negativo em `error`; "Orçamentos que viraram venda" some sem orçamento criado no período. O texto acima não foi reescrito.
