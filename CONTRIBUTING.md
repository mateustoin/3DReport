# Como contribuir

Obrigado pelo interesse em contribuir com o 3DReport! Este documento cobre o
básico para propor mudanças. Para setup, comandos e detalhes técnicos do dia
a dia, veja [docs/development.md](docs/development.md).

## Antes de codar

- **Bugs:** abra uma issue usando o template de bug report. Se já souber a
  causa e quiser corrigir, pode abrir a issue e o PR juntos.
- **Funcionalidades novas:** abra uma issue usando o template de feature
  request antes de implementar. O projeto mantém um backlog vivo em
  [docs/roadmap.md](docs/roadmap.md) e um histórico de decisões em
  [docs/decisions.md](docs/decisions.md): mudanças de UI/UX, arquitetura,
  organização e funcionalidades passam por aprovação do responsável do
  projeto antes de entrar no código, então alinhar antes evita retrabalho.
- **Dúvidas de uso:** não é bug nem feature, mas abra uma
  [Discussion](https://github.com/mateustoin/3DReport/discussions) (se
  habilitada) ou uma issue mesmo assim, sem problema.

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
