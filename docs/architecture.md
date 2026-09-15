# Arquitetura

## Visão geral

Projeto **Kotlin Multiplatform** com dois módulos Gradle:

```
3DReport/
├─ core/                      # KMP puro: domínio e cálculo (sem UI)
│  └─ src/
│     ├─ commonMain/kotlin/com/threedreport/core/
│     │  ├─ model/            # Filament, PrintJob, PricingSettings, Quote
│     │  └─ pricing/          # PricingCalculator
│     └─ commonTest/          # testes unitários (kotlin.test)
├─ composeApp/                # UI Compose Multiplatform
│  └─ src/
│     ├─ commonMain/          # UI compartilhada (App.kt)
│     └─ jvmMain/             # entrada do desktop (Main.kt)
├─ docs/                      # documentação
├─ gradle/libs.versions.toml  # catálogo de versões
├─ LICENSE
└─ README.md
```

Dependência: `composeApp → core`. O `core` nunca depende da UI.

## Módulos

### `core`
- Kotlin puro, todo o código em `commonMain` — roda em qualquer alvo.
- `model/`: classes imutáveis (`data class`) que validam suas entradas no `init`
  (`IllegalArgumentException` para valores negativos/zero inválidos).
- `pricing/PricingCalculator`: função pura `calculate(job, settings): Quote`.
  Fórmulas em [pricing-formulas.md](pricing-formulas.md).
- Alvo atual: `jvm()`.

### `composeApp`
- Compose Multiplatform + Material 3.
- Alvo atual: `jvm()` (desktop). Empacotamento nativo via `compose.desktop`
  (`.deb`, `.msi`, `.dmg`).
- **A tela atual é provisória** e só demonstra a integração com o `core`.
  O padrão de apresentação proposto é MVVM (ViewModel + StateFlow), a ser
  confirmado junto com a proposta de UI/UX.

## Plataformas

| Plataforma | Estado |
|---|---|
| Desktop (JVM) | Ativo |
| Android | Planejado. Estrutura compatível; nada configurado ainda |
| iOS / Web | Não planejado por enquanto |

### Como adicionar Android no futuro
1. Adicionar o plugin Android (AGP) ao catálogo e ao `build.gradle.kts` raiz.
2. No `core`, declarar o alvo Android (plugin `com.android.kotlin.multiplatform.library`).
3. Criar um módulo de aplicação Android (ex.: `androidApp`) que dependa de
   `composeApp`/`core` e contenha a `Activity` chamando `App()`.
4. Nenhuma mudança em `core/commonMain` nem em `composeApp/commonMain` deve ser
   necessária.

Esse passo não foi feito agora para manter o build simples e independente do
Android SDK enquanto o foco é desktop.

## Edição gratuita x paga

Ainda não há separação de código. A ideia é que o cálculo básico de orçamento
(atual `core`) componha a edição gratuita, e recursos pagos entrem em módulos
próprios. O mecanismo (flavors, módulos, licença em runtime) será proposto
quando houver o primeiro recurso pago.

## Convenções

- Pacote base: `com.threedreport`.
- Identificadores em inglês; KDoc e documentação em português.
- Percentuais como fração decimal (`0.10` = 10%).
- Valores monetários em `Double`, arredondados só na exibição.
- Versões centralizadas em `gradle/libs.versions.toml`.
