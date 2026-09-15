# 3DReport

Aplicativo para criação de **orçamentos de impressão 3D**. A partir dos dados do
fatiador (filamento, comprimento em metros e tempo de impressão) e dos custos da
operação (energia, manutenção, falhas, acabamento, retorno do investimento na
máquina), calcula o **valor de produção** e o **valor de venda** com a margem de
lucro desejada.

> **Status:** base do projeto. O motor de cálculo está implementado e testado;
> a interface atual é provisória e a UI/UX definitiva ainda será definida.

## Funcionalidades

| Funcionalidade | Edição | Estado |
|---|---|---|
| Cálculo de custo de produção e preço de venda | Gratuita | Motor pronto (`core`), sem UI |
| Interface de criação de orçamento | Gratuita | A definir |
| Taxas de marketplace, embalagem etc. | Paga | Planejado |

## Stack

- Kotlin 2.4.20 · Kotlin Multiplatform
- Compose Multiplatform 1.12.0 · Material 3
- Gradle 9.7.1 (wrapper) · JDK 21
- Plataforma: desktop (Windows/Linux/macOS). Android planejado.

## Início rápido

```bash
./gradlew :composeApp:run   # executa o app desktop
./gradlew allTests          # roda os testes
./gradlew build             # compila e testa tudo
```

Pré-requisito: JDK 21. IDE recomendada: Android Studio ou IntelliJ IDEA (VS Code
funciona via terminal). Detalhes em [docs/development.md](docs/development.md).

## Estrutura

```
core/        Domínio e motor de cálculo (Kotlin Multiplatform puro, sem UI)
composeApp/  Interface Compose Multiplatform (desktop)
docs/        Documentação
```

## Documentação

| Documento | Conteúdo |
|---|---|
| [docs/development.md](docs/development.md) | Setup, IDE, executar, testar, empacotar |
| [docs/architecture.md](docs/architecture.md) | Módulos, plataformas, convenções |
| [docs/pricing-formulas.md](docs/pricing-formulas.md) | Parâmetros e fórmulas de cálculo |
| [docs/decisions.md](docs/decisions.md) | Decisões aprovadas e pendentes |
| [core/README.md](core/README.md) | Módulo `core` |
| [composeApp/README.md](composeApp/README.md) | Módulo `composeApp` |

## Licença

Software proprietário — todos os direitos reservados. Uma Edição Gratuita, com as
funcionalidades básicas de orçamento, poderá ser distribuída em formato
executável. Veja [LICENSE](LICENSE).
