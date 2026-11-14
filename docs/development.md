# Guia de desenvolvimento

## Pré-requisitos

| Ferramenta | Versão | Observação |
|---|---|---|
| JDK | 21 | Necessário. Verifique com `java -version` |
| Gradle | — | Não instale: use o wrapper `./gradlew` (baixa o Gradle 9.7.1) |
| Android SDK | — | **Não** é necessário hoje (só desktop) |

Versões das bibliotecas: [gradle/libs.versions.toml](../gradle/libs.versions.toml)
(Kotlin 2.4.20, Compose Multiplatform 1.12.0, Material 3 1.9.0).

## IDE recomendada

**Android Studio** ou **IntelliJ IDEA** (com o plugin Kotlin Multiplatform).
Elas trazem autocompletar, navegação, depuração, preview de Compose e execução de
testes pela interface para KMP. Basta abrir a pasta raiz do projeto; o Gradle
sincroniza sozinho.

**VS Code** funciona para editar e rodar tudo pelo terminal (comandos abaixo), mas
o suporte a Kotlin/KMP no VS Code é limitado (análise de código e depuração
inferiores). Use-o se preferir, mas a experiência será melhor no Android Studio.

## Comandos

Todos na raiz do projeto (no Windows use `gradlew.bat`).

| Objetivo | Comando |
|---|---|
| Executar o app desktop | `./gradlew :composeApp:run` |
| Rodar todos os testes | `./gradlew allTests` |
| Rodar só testes do core (JVM e JS) | `./gradlew :core:allTests` |
| Rodar só testes do composeApp | `./gradlew :composeApp:jvmTest` |
| Compilar, conferir o estilo e testar tudo | `./gradlew build` |
| Conferir o estilo (ktlint) | `./gradlew ktlintCheck` |
| Corrigir o estilo automaticamente | `./gradlew ktlintFormat` |
| Relatório de cobertura de testes | `./gradlew koverHtmlReport` |
| Gerar a calculadora do site | `./gradlew :web:syncSiteCalculator` |
| Gerar instalador do SO atual | `./gradlew :composeApp:packageDistributionForCurrentOS` |
| Limpar build | `./gradlew clean` |

- Uma classe de teste só:
  `./gradlew :composeApp:jvmTest --tests "com.threedreport.app.ui.quote.QuoteViewModelTest"`
  (no `core`, use `:core:jvmTest`).
- `scripts/check.sh <tarefa>` roda a mesma tarefa e mostra só "OK" ou as falhas, com
  o log completo num arquivo temporário. É o jeito recomendado pra quem trabalha com
  assistente de IA, porque o log do Gradle é longo.
- Pra abrir o app sem tocar nos seus dados reais, aponte a pasta de dados pra outro
  lugar: `JAVA_TOOL_OPTIONS=-Dthreedreport.dataDir=/tmp/3dreport-teste ./gradlew :composeApp:run`.
- Relatório de testes: `core/build/reports/tests/jvmTest/index.html`.
- Estilo: o ktlint confere o código (regras no `.editorconfig` da raiz) e o `./gradlew build`
  falha se algo estiver fora do padrão. `./gradlew ktlintFormat` corrige o que dá pra
  corrigir sozinho. Arquivos gerados (em `build/`) ficam de fora.
- Instaladores gerados: `composeApp/build/compose/binaries/main/<formato>/`
  (ex.: `.../deb/3dreport_1.0.0_amd64.deb`, `.../msi/3DReport-1.0.0.msi`,
  `.../dmg/3DReport-1.0.0.dmg`). **`packageDistributionForCurrentOS` só gera
  o formato do SO em que está rodando** — o `jpackage` (por trás do
  empacotamento) não faz cross-compilation. Pra ter os 3 formatos, é preciso
  rodar em cada SO (ou usar o workflow de CI — ver "Cortando um release"
  abaixo).
- Dados do app (filamentos, impressoras, configurações): `~/.3dreport/*.json`.
  Apague a pasta para resetar para os valores padrão.

- Calculadora do site (decisão 98): depois de gerar, rode
  `python3 -m http.server -d site` e abra `http://localhost:8000/calculadora.html`
  (abrir o HTML direto do disco também funciona). `site/assets/calc/` é gerado e
  fica fora do git; o `kotlin-js-store/yarn.lock` (dependências JS do Kotlin/JS)
  é versionado.

### Rodando pela IDE
- Android Studio/IntelliJ: abra `Main.kt` e clique no ▶ ao lado de `fun main()`,
  ou use o painel Gradle → `composeApp > Tasks > compose desktop > run`.
- Testes: ▶ ao lado da classe de teste.

## Testes

- Framework: `kotlin.test` (multiplataforma), em `src/commonTest`.
- O motor de cálculo é validado contra os valores da planilha de referência
  ([pricing-formulas.md](pricing-formulas.md)). Toda mudança de fórmula deve
  atualizar teste **e** documentação.
- Cobertura (Kover, só mede; não há limite mínimo): `./gradlew koverHtmlReport` gera o
  relatório agregado de `core` e `composeApp` em `build/reports/kover/html/index.html`
  (por módulo: `./gradlew :core:koverHtmlReport`). O CI publica esse HTML como artefato
  do workflow. No `core` só conta o JVM.
- Em `composeApp/src/jvmTest`, a pasta de dados de cada teste vem da propriedade
  de sistema `threedreport.dataDir` (um diretório temporário por teste). Ela
  precisa estar setada **antes** de qualquer repositório ser construído: sempre
  em `@BeforeTest` (ou dentro do próprio teste), nunca como campo de classe
  (`val repo = LocalStorage()...`), porque o repositório já lê a pasta no
  momento em que é criado, e um campo de classe é avaliado antes do
  `@BeforeTest` rodar.

## Prints do site

Os prints de `site/assets/screenshots/` e o banner `site/assets/og-banner.png`
saem do próprio app, renderizado sem janela com dados de exemplo fictícios
(decisão 120). Quando a interface mudar, gere de novo:

```bash
# Com fotos: aponte pra uma pasta com G-codes que tenham miniatura
./gradlew :composeApp:renderScreenshots -Pscreenshots.gcodes=/caminho/dos/gcodes

# Sem fotos
./gradlew :composeApp:renderScreenshots
```

Confira cada imagem antes de commitar. O teste só roda por essa task: no
`./gradlew build` ele não faz nada.

## Fluxo de mudanças

1. Decisões de UI/UX, arquitetura, organização e funcionalidades são propostas e
   aprovadas antes da implementação. Cada uma ganha um arquivo em
   [`decisions/`](decisions/) e uma linha no índice [decisions.md](decisions.md).
2. Código e documentação mudam no mesmo commit; o `README.md` é mantido atualizado.
3. `./gradlew build` deve passar antes de commitar.
4. Ao fechar uma leva de funcionalidades, sempre: bump de versão (ver
   "Versionamento" abaixo) + entrada nova em [`CHANGELOG.md`](../CHANGELOG.md)
   — não é opcional, mesmo pra levas pequenas.
5. Ideias de fora chegam como issue com a label `triagem` (decisão 83). A
   triagem responde na issue (aceita, mesclada, já existe ou descartada),
   registra o resultado no [roadmap](roadmap.md) com crédito a quem sugeriu
   e tira a label. Ver [CONTRIBUTING.md](../CONTRIBUTING.md#sugerindo-ideias).

### Roteiro de uma leva

Uma leva do [roadmap](roadmap.md) segue sempre as mesmas etapas, cada uma num commit:

1. **Especificação** em [`levas/`](levas/), a partir de [`levas/_modelo.md`](levas/_modelo.md):
   escopo, o que fica de fora, mudanças de modelo, telas e critérios de aceite.
2. **Aprovação** das decisões em aberto pelo responsável do projeto.
3. **Core e dados**, com teste antes do código (`feat(core): ...`).
4. **Telas** (`feat(ui): ...`).
5. **Revisão de design** sobre as capturas de tela.
6. **Documentação**: decisões, changelog, roadmap, README e site (`docs: ...`).
7. **Revisão de código** e correções (`fix: ...`).
8. **PR** a partir da branch `feat/leva-N-nome`, com merge por squash.

Quem desenvolve com o Claude Code tem esse roteiro pronto no comando `/leva N`
(`.claude/skills/leva/`), com um agente por etapa em `.claude/agents/`. As regras
que os agentes seguem estão no [`CLAUDE.md`](../CLAUDE.md) da raiz e nos de cada módulo.

## Versionamento

O app segue **SemVer** (decisão 53 em [decisions.md](decisions.md), que revisa
a decisão 27). O tipo de bump depende do que a leva entrega, não do tamanho
dela:

- **PATCH** (`z`) — leva que só corrige bug(s) e/ou só atualiza documentação,
  sem funcionalidade nova nem mudança de comportamento visível ao usuário.
- **MINOR** (`y`) — leva que entrega funcionalidade nova ou melhoria visível
  (se a leva mistura correção com funcionalidade nova, o bump é MINOR — a
  funcionalidade nova já exige isso; PATCH é só pra leva que **não** tem nada
  de funcionalidade nova).
- **MAJOR** (`x`) — quebra de compatibilidade. Primeiro uso na 2.0.0
  (decisão 104): o formato dos dados mudou sem conversão, e a pasta antiga é
  guardada em `~/.3dreport-v1`.

Ao fechar uma leva, atualize a versão em **um lugar só**: `gradle.properties` →
`appVersion`. A tarefa `generateAppVersion` do `composeApp` gera a constante
`APP_VERSION` a partir dele a cada build (decisão 108; antes eram dois arquivos
mantidos à mão).

A versão aparece na barra lateral, na tela Sobre, no diálogo de erro e no
instalador nativo (`packageVersion`).

## Changelog

[`CHANGELOG.md`](../CHANGELOG.md) (raiz do repo) segue o formato
[Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) (decisão 31 em
[decisions.md](decisions.md)): uma seção por versão (mais recente no topo),
com subseções `Adicionado`/`Alterado`/`Corrigido` conforme fizer sentido.
Toda leva de funcionalidades ganha sua entrada **no mesmo commit** do bump de
versão — escreva do ponto de vista de quem usa o app (o quê mudou e por quê
importa), não em termos de arquivos/classes internos (isso já está em
`decisions.md`/`architecture.md`). Serve de rascunho pronto pra colar como
release notes quando um release for de fato publicado no GitHub (ver
[roadmap.md](roadmap/catalogo.md#3-instaladores-desktop)).

## Cortando um release

Instaladores dos 3 SOs (decisão 33 em [decisions.md](decisions.md)) são
gerados pelo workflow [`.github/workflows/release.yml`](../.github/workflows/release.yml),
disparado só por **push de uma tag** `vX.Y.Z` (nunca em push normal de
branch). Passo a passo:

1. Feche a leva normalmente: código + docs + bump de versão + `CHANGELOG.md`
   (passos acima), tudo commitado e já em `main`.
2. **Antes de criar a tag, sempre confirme com o responsável do projeto** se é
   pra cortar o release agora ou seguir implementando e só taguear depois —
   não crie/empurre a tag por conta própria.
3. Com o aval:
   ```bash
   git tag v1.0.0        # mesma versão do appVersion em gradle.properties
   git push origin v1.0.0
   ```
4. Antes de tudo o workflow confere que a tag bate com o `appVersion` de
   `gradle.properties` (tag `v2.2.0` exige `appVersion=2.2.0`) e roda
   `./gradlew build` (estilo e testes). Se falhar, nada é gerado.
   Depois builda o `.deb` (Ubuntu), `.msi` (Windows) e dois `.dmg` (macOS
   Apple Silicon e Intel, com o sufixo no nome) em
   paralelo, um runner por SO — resolve a limitação de
   `packageDistributionForCurrentOS` sem precisar de máquina Windows/macOS
   própria — e junta os três num **GitHub Release em rascunho** (`draft`),
   com a descrição já preenchida a partir da seção correspondente do
   `CHANGELOG.md`.
5. Revise o rascunho em github.com/mateustoin/3DReport/releases e clique em
   "Publish release" quando estiver satisfeito (o workflow nunca publica
   sozinho).

Sem assinatura de código: o `.msi` dispara aviso do SmartScreen do Windows
("Editor desconhecido" → "Mais informações → Executar assim mesmo") e o
`.dmg` dispara aviso do Gatekeeper do macOS — clique direito → "Abrir"
costuma resolver, mas em versões mais recentes do macOS (confirmado na
prática, decisão 60) só funciona indo em Ajustes do Sistema → Privacidade
e Segurança → "Abrir mesmo assim". Um
certificado de assinatura tem custo recorrente, o que não combina com o
modelo 100% financiado por doação (decisão 15) — fica registrado como
limitação conhecida, não como pendência.
