# Como contribuir

Obrigado pelo interesse em contribuir com o 3DReport! Este documento cobre o
básico para propor mudanças. Para setup, comandos e detalhes técnicos do dia
a dia, veja [docs/development.md](docs/development.md).

## Antes de codar

- **Bugs:** abra uma issue usando o template de bug report. Se já souber a
  causa e quiser corrigir, pode abrir a issue e o PR juntos.
- **Funcionalidades novas:** abra uma issue usando o template de feature
  request antes de implementar (ver [Sugerindo ideias](#sugerindo-ideias)
  abaixo). O projeto mantém um backlog vivo em
  [docs/roadmap.md](docs/roadmap.md) e um histórico de decisões em
  [docs/decisions.md](docs/decisions.md): mudanças de UI/UX, arquitetura,
  organização e funcionalidades passam por aprovação do responsável do
  projeto antes de entrar no código, então alinhar antes evita retrabalho.
- **Dúvidas de uso:** não é bug nem feature, mas abra uma
  [Discussion](https://github.com/mateustoin/3DReport/discussions) (se
  habilitada) ou uma issue mesmo assim, sem problema.

## Sugerindo ideias

Testou o app e teve uma ideia? Ótimo, e não precisa saber programar pra
contribuir com ela.

1. **Confira o [roadmap](docs/roadmap.md)** (Ctrl+F ajuda). Se a ideia já
   estiver lá, ainda vale abrir a issue pra complementar: um segundo pedido
   pela mesma coisa pesa na prioridade.
2. **Abra uma issue por ideia** usando o template "Sugerir uma
   funcionalidade". Conte o problema que você tem no dia a dia antes da
   solução: é o problema que ajuda a decidir.
3. **Triagem.** O responsável do projeto lê a issue e responde nela com um
   destes resultados:
   - **Aceita:** vira item novo no roadmap;
   - **Mesclada:** complementa um item que já existia;
   - **Já existe:** a issue é fechada apontando o item do roadmap;
   - **Descartada:** o motivo fica registrado no roadmap, pra ideia não
     voltar à fila sem argumento novo.
4. **Crédito.** Item vindo de sugestão externa leva o seu nome no roadmap,
   no padrão "(sugerido por Fulano na issue #N, data)". A issue fica aberta
   enquanto o item estiver pendente e é fechada quando ele for implementado.

Não crie arquivos de ideias no repositório: o
[roadmap](docs/roadmap.md) é a única lista de ideias do projeto, e uma
segunda lista paralela acaba desatualizada (decisão 83 em
[docs/decisions.md](docs/decisions.md)). Quer implementar uma ideia
aprovada? Comente na issue antes de começar e siga
[Enviando uma mudança](#enviando-uma-mudança).

## Ambiente de desenvolvimento

Pré-requisito único: **JDK 21**. Não precisa instalar Gradle (o wrapper
`./gradlew` cuida disso). Detalhes completos, IDE recomendada e todos os
comandos em [docs/development.md](docs/development.md#comandos). Resumo:

```bash
./gradlew :composeApp:run   # executa o app desktop
./gradlew allTests          # roda os testes
./gradlew build             # compila e testa tudo (deve passar antes de commitar)
```

## Enviando uma mudança

1. Faça um fork do repositório e crie uma branch a partir de `main`.
2. Siga o estilo de código já existente no arquivo que você está editando
   (sem formatador automático configurado ainda).
3. Se mudar uma fórmula de cálculo, atualize o teste **e**
   [docs/pricing-formulas.md](docs/pricing-formulas.md) no mesmo commit.
4. Rode `./gradlew build` localmente antes de abrir o PR: o CI roda o mesmo
   comando.
5. Mensagens de commit seguem o padrão
   [Conventional Commits](https://www.conventionalcommits.org/pt-br/) (`feat:`,
   `fix:`, `docs:`, `refactor:`, `test:`, `chore:`), como no histórico do
   projeto.
6. Abra o Pull Request preenchendo o template, descrevendo o que mudou e por
   quê, e referenciando a issue relacionada quando houver.

## Licença

Ao contribuir, você concorda que sua contribuição será licenciada sob a
[Apache License 2.0](LICENSE), a mesma do projeto.
