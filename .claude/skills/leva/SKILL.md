---
name: leva
description: Roteiro pra implementar uma leva do roadmap do 3DReport do começo ao fim, com especificação aprovada antes do código e um agente por etapa. Use quando o pedido for "/leva N", "vamos fazer a leva N" ou implementar um conjunto de itens do roadmap.
---

# Fazer uma leva

Uma leva é um conjunto de itens do roadmap que sai junto num PR. O roteiro existe
por dois motivos: o responsável do projeto aprova as decisões antes de qualquer
código, e cada etapa roda num agente com contexto pequeno, que recebe só a
especificação em vez do histórico da conversa.

Você, na sessão principal, especifica, apresenta as decisões, coordena e revisa.
Quem escreve é o agente de cada etapa.

## 1. Especificar

- Leia **só a seção da leva** em `docs/roadmap.md` (ache com `grep -n "^### Leva"`),
  as decisões que ela cita (`docs/decisions/NNN.md`) e o texto dos itens marcados
  *(já no roadmap)* em `docs/roadmap/catalogo.md`.
- Olhe o código que a leva toca o suficiente pra saber o que já existe e pode ser
  reaproveitado. Pra uma varredura ampla, use o agente `Explore`.
- Escreva `docs/levas/NN-nome-curto.md` a partir de `docs/levas/_modelo.md`.

Se o pedido trouxe "Fora de escopo" ou "Prioridade", isso vale mais que o roadmap.

## 2. Aprovar

Apresente ao responsável do projeto as decisões em aberto, cada uma com opções e uma
recomendação. Decisão de interface vem com um esboço do que muda na tela. Nada das
etapas seguintes começa antes da resposta. O que for aprovado entra na especificação
com o número da decisão (o próximo livre no índice `docs/decisions.md`).

Crie a branch `feat/leva-N-nome-curto` a partir da `main`.

## 3. Core

Agente `engenheiro-core`, com o caminho da especificação. Leva sem mudança de
modelo, cálculo ou dados pula esta etapa. Commit: `feat(core): ...`.

## 4. Telas

Agente `construtor-ui`, com o caminho da especificação e o relato da etapa anterior
(os tipos e funções novos). Telas independentes entre si podem ir em agentes
paralelos, um por tela. Commit: `feat(ui): ...`.

## 5. Design

Agente `revisor-design`, com a lista das telas alteradas. Os desvios voltam pro
`construtor-ui` corrigir; repita até não sobrar desvio. Mostre as capturas finais ao
responsável do projeto antes de seguir.

## 6. Documentação

Agente `escriba`, com o caminho da especificação e as decisões numeradas.
Commit: `docs: ...`.

## 7. Revisão

Rode `scripts/check.sh build`. Depois `/code-review` no diff da branch e corrija o
que ele achar. Commit: `fix: correções da revisão de código da leva N`.

Leia você mesmo o diff do `core/` e de `data/`: é onde um erro custa dinheiro a quem
usa o app.

## 8. Entregar

Mostre o resumo e pergunte antes de fazer push e abrir o PR. O corpo do PR segue o
do #7: "O que muda e por quê", as regras novas com o número das decisões, "Nada muda
sozinho" (as garantias de compatibilidade), os commits, e "Pendente antes do merge"
(o teste manual do responsável). Merge por squash. Tag e release só se pedidos.

## Ao chamar um agente

Dê a ele o caminho da especificação, o que as etapas anteriores entregaram, e o que
você quer de volta. Não cole a conversa. Se o agente devolver uma dúvida de produto,
ela é do responsável do projeto: leve a pergunta, não responda por ele.
