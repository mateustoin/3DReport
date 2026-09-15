# Roadmap

Lista viva de evoluções e próximas implementações. Diferente de
[decisions.md](decisions.md) (o que já foi decidido), este documento é um
**rascunho de backlog**: itens aqui ainda não têm data nem prioridade
definida. Um item só sai daqui pra `decisions.md` quando alguém propuser
como implementar, o responsável do projeto aprovar, e a mudança entrar no
código (ver [development.md](development.md#fluxo-de-mudanças)).

> Este primeiro rascunho foi montado juntando pendências que já apareceram em
> conversas anteriores. Ainda precisa da sua revisão: o que prioriza, o que
> remove, e principalmente **o que falta** — funcionalidades de produto que só
> você tem em mente.

## Funcionalidades do produto

- [ ] **Salvar/exportar um orçamento finalizado.** Hoje a tela de Orçamento só
      calcula em tempo real; não há como guardar um orçamento pronto pra
      mandar pro cliente (o mockup original de UI/UX até tinha um botão
      "Salvar orçamento" que nunca foi implementado). Formato de saída a
      decidir: PDF, texto pra copiar/colar, impressão direta, etc.
- [ ] **Histórico de orçamentos.** Lista dos orçamentos já calculados/salvos,
      pra consultar depois sem precisar refazer as contas.
- [ ] **Taxas de marketplace (ex.: Shopee) e custo de embalagem/spray.**
      Estava fora de escopo por ser "recurso pago" (decisão 7, hoje
      substituída) — não há mais essa barreira, só falta decidir como esses
      custos entram na fórmula (ver planilha de referência).
- [ ] **Edição/exclusão com confirmação.** Nas telas de Filamentos e
      Impressoras, "Excluir" age na hora, sem diálogo de confirmação — risco
      de exclusão acidental de um perfil configurado com calma.

## Plataformas

- [ ] **Android.** Já é a plataforma planejada desde a decisão 1; os passos
      estão descritos em [architecture.md](architecture.md#como-adicionar-android-no-futuro).
      Principal trabalho: um `actual` de `data/` para Android (`DataStore`
      ou arquivo em `Context.filesDir`, já que a persistência atual usa
      `java.io.File` com `user.home`, específico de desktop).
- [ ] **Instaladores desktop.** `./gradlew :composeApp:packageDistributionForCurrentOS`
      já gera `.deb`/`.msi`/`.dmg`, mas nunca foi publicado um release — vale
      decidir um fluxo (ex.: GitHub Releases) quando o repositório for público.

## Infraestrutura e qualidade (agora que o projeto é open source)

- [ ] **CI no GitHub Actions.** Rodar `./gradlew build` (compila + testa
      `core` e `composeApp`) a cada push/PR. Hoje isso só roda manualmente.
      Importante pra dar confiança a quem for revisar/contribuir de fora.
- [ ] **CONTRIBUTING.md.** Como rodar, testar e propor mudanças — hoje só
      existe [docs/development.md](development.md), voltado a você mesmo.
- [ ] **Badges no README.** Build (CI), licença (Apache 2.0) e o botão de
      apoio (Buy Me a Coffee) já linkado — comuns em repositórios públicos.

## Observações técnicas (não são pedidos de mudança, só pontos a reavaliar se algo doer na prática)

- Persistência em arquivo JSON (decisão 14) foi escolhida por simplicidade;
  se o volume de dados crescer muito ou vier a precisar de consultas mais
  complexas, migrar para SQLDelight é a alternativa que já foi cogitada.
- Valores monetários em `Double` (decisão 16): reavaliar só se aparecer um
  bug real de arredondamento — não é esperado no uso atual.
