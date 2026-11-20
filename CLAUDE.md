# 3DReport

App desktop gratuito (Apache 2.0, mantido por doação) pra quem vende impressão 3D:
orçamento, pedidos, catálogo, cadastros e dashboard. Kotlin Multiplatform com Compose
Multiplatform. Hoje só roda em desktop (JVM); Android e nuvem são caminhos prováveis.

## Módulos

| Módulo | O que tem | Regra |
|---|---|---|
| `core/` | Domínio, cálculo de preço, leitura de G-code e STL. Alvos JVM e JS | Nunca depende de UI. Ver `core/CLAUDE.md` |
| `composeApp/` | Telas, ViewModels, repositórios e persistência em JSON | Ver `composeApp/CLAUDE.md` |
| `web/` | Fachada JS do `core` pra calculadora do site | Só adapta entradas; a conta é do `core` |
| `site/` | Site estático (GitHub Pages) | Texto público: ver "Escrita" abaixo |

## Comandos

Use `scripts/check.sh <tarefa>` em vez de `./gradlew` direto: ele imprime só "OK" ou
as falhas, sem encher o contexto com o log do Gradle.

| Objetivo | Comando |
|---|---|
| Compilar e testar tudo (o que o CI roda) | `scripts/check.sh build` |
| Testes do core | `scripts/check.sh :core:allTests` |
| Testes do app | `scripts/check.sh :composeApp:jvmTest` |
| Uma classe de teste | `scripts/check.sh :composeApp:jvmTest --tests "com.threedreport.app.ui.quote.QuoteViewModelTest"` |
| Formatar o código (ktlint) | `scripts/check.sh ktlintFormat` |
| Capturas de tela | `scripts/check.sh :composeApp:renderScreenshots` (grava em `site/assets/screenshots/`) |
| Rodar o app sem tocar nos dados reais | `JAVA_TOOL_OPTIONS=-Dthreedreport.dataDir=<pasta temporária> ./gradlew :composeApp:run` |

Os dados reais de quem usa ficam em `~/.3dreport`. Nunca rode o app nem um teste
apontando pra lá. Depois de criar uma classe de teste, compare `ls -la ~/.3dreport`
antes e depois.

## Onde ler (e o que não ler inteiro)

| Preciso de | Leia |
|---|---|
| O que fazer a seguir | A seção da leva em `docs/roadmap.md` (só ela) |
| Texto completo de um item "*(já no roadmap)*" | `docs/roadmap/catalogo.md`, pela seção citada |
| Uma decisão pelo número | `docs/decisions/NNN.md` (ex.: `docs/decisions/121.md`) |
| Achar uma decisão pelo assunto | `docs/decisions.md` (índice de uma linha por decisão) |
| Fórmulas de preço | A seção relevante de `docs/pricing-formulas.md` |
| Como o código está organizado | A seção relevante de `docs/architecture.md` |
| Aparência e componentes | `docs/design-system.md` |
| O que entra na próxima versão | Só o bloco `[Não publicado]` do `CHANGELOG.md` |

`docs/roadmap/plano-1-historico.md` e as versões antigas do `CHANGELOG.md` são
histórico: só abra se a tarefa pedir.

## Regras de trabalho

- **Aprovar antes de decidir.** Decisão de interface, arquitetura, organização ou
  funcionalidade é apresentada com opções e uma recomendação, e só é implementada
  depois que o responsável do projeto aprova. Decisão aprovada ganha número e arquivo
  em `docs/decisions/`, mais uma linha no índice.
- **Código e documentação no mesmo PR.** README, `docs/` e `CHANGELOG.md` acompanham
  a mudança. Fórmula que muda leva junto o teste e `docs/pricing-formulas.md`.
- **Campo novo nasce neutro** (zero ou desligado): atualizar o app não muda o preço
  de ninguém, e dado gravado por versão anterior continua abrindo. Mudança de formato
  ganha teste de compatibilidade.
- **Versão.** A última publicada é a 2.3.0 (`appVersion` em `gradle.properties`). As
  mudanças acumulam em `[Não publicado]`, e o número da próxima versão é decidido com o
  responsável quando ela for fechada. Não citar versão alvo em título de PR. Não sugerir versão de correção fora disso, salvo problema grave, e perguntar antes.
- **Nunca criar nem empurrar tag `vX.Y.Z` sem perguntar:** a tag dispara a release.
- **Repositório público:** todo push é publicação. Não fazer push nem abrir PR sem
  que tenha sido pedido.
- **Local e conectado.** Em planos e revisões, separar o que roda local do que exige
  conexão, e dizer o que muda no conectado (armazenamento, servidor, LGPD, custo).

## Escrita

- Identificadores em inglês; KDoc, comentários e documentação em português.
- Texto que o público lê (README, site, changelog, descrição de PR e de release, textos
  do app) não usa travessão. Frases curtas, do ponto de vista de quem usa o app.
- Commits no padrão Conventional Commits, em português: `feat(core): ...`,
  `feat(ui): ...`, `docs: ...`, `fix: ...`.

## Como uma leva é feita

O roteiro completo está na skill `/leva` (`.claude/skills/leva/SKILL.md`). Em resumo:
especificar em `docs/levas/`, aprovar, core, telas, revisão de design, documentação,
`/code-review`, PR. Cada etapa tem um agente em `.claude/agents/`:

| Agente | Modelo | Papel |
|---|---|---|
| sessão principal | Fable | Especifica, apresenta as decisões, coordena e revisa |
| `engenheiro-core` | Opus | `core/` e a camada de dados, com teste antes |
| `construtor-ui` | Sonnet | Telas e ViewModels a partir da especificação |
| `revisor-design` | Opus | Confere as capturas contra o sistema de design; não edita |
| `escriba` | Sonnet | Decisões, changelog, roadmap, README e site |

Ajuste pequeno fora de leva não precisa do roteiro: branch, mudança com teste,
`scripts/check.sh build`, PR.
