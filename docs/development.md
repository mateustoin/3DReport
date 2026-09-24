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
| Rodar só testes do composeApp (persistência) | `./gradlew :composeApp:jvmTest` |
| Compilar e testar tudo | `./gradlew build` |
| Gerar a calculadora do site | `./gradlew :web:syncSiteCalculator` |
| Gerar instalador do SO atual | `./gradlew :composeApp:packageDistributionForCurrentOS` |
| Limpar build | `./gradlew clean` |

- Relatório de testes: `core/build/reports/tests/jvmTest/index.html`.
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

## Fluxo de mudanças

1. Decisões de UI/UX, arquitetura, organização e funcionalidades são propostas e
   aprovadas antes da implementação e registradas em [decisions.md](decisions.md).
2. Código e documentação mudam no mesmo commit; o `README.md` é mantido atualizado.
3. `./gradlew build` deve passar antes de commitar.
4. Ao fechar uma leva de funcionalidades, sempre: bump de versão (ver
   "Versionamento" abaixo) + entrada nova em [`CHANGELOG.md`](../CHANGELOG.md)
   — não é opcional, mesmo pra levas pequenas.
5. Ideias de fora chegam como issue com a label `triagem` (decisão 83). A
   triagem responde na issue (aceita, mesclada, já existe ou descartada),
   registra o resultado no [roadmap](roadmap.md) com crédito a quem sugeriu
   e tira a label. Ver [CONTRIBUTING.md](../CONTRIBUTING.md#sugerindo-ideias).

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
- **MAJOR** (`x`) — quebra de compatibilidade; sem caso de uso previsto ainda.

Ao fechar uma leva, atualize a versão **nos dois lugares** (fonte única
mantida manualmente em sincronia, sem geração automática):

- `gradle.properties` → `appVersion`
- [`composeApp/.../app/AppVersion.kt`](../composeApp/src/commonMain/kotlin/com/threedreport/app/AppVersion.kt) → `APP_VERSION`

A versão aparece no rodapé do app e no instalador nativo (`packageVersion`).

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
[roadmap.md](roadmap.md#3-instaladores-desktop)).

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
   git tag v1.0.0        # mesma versão do gradle.properties/AppVersion.kt
   git push origin v1.0.0
   ```
4. O workflow builda o `.deb` (Ubuntu), `.msi` (Windows) e `.dmg` (macOS) em
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
