# 3DReport

Aplicativo **gratuito e de código aberto** para criação de **orçamentos de
impressão 3D**. A partir do filamento usado, da impressora escolhida (cada uma
com seu próprio perfil salvo) e dos dados da peça (comprimento em metros e
tempo de impressão), calcula o **valor de produção** e o **valor de venda**
com a margem de lucro desejada.

> **Status:** motor de cálculo implementado e testado; telas de orçamento,
> cadastro de filamentos, cadastro de impressoras e configurações gerais
> funcionando, com persistência real em disco. Veja pendências em
> [docs/decisions.md](docs/decisions.md).

## Funcionalidades

| Funcionalidade | Estado |
|---|---|
| Cálculo de custo de produção e preço de venda | Pronto (`core`) |
| Tela de orçamento (escolhe filamento + impressora, entra comprimento/tempo → produção/venda) | Pronto |
| Cadastro de filamentos (catálogo salvo) | Pronto |
| Cadastro de impressoras (perfis salvos: consumo, manutenção, investimento) | Pronto |
| Configurações gerais do negócio (energia, falhas, acabamento, margem) | Pronto |
| Persistência em disco (`~/.3dreport/`, arquivos JSON) | Pronto |
| Taxas de marketplace, custo de embalagem/spray etc. | Planejado |

## Apoie o projeto

O 3DReport é e sempre será gratuito e de código aberto — não há edição paga
nem assinatura. Se ele te ajuda, considere uma doação voluntária:
[Buy Me a Coffee](https://buymeacoffee.com/mateustoin).

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

Código aberto sob [Apache License 2.0](LICENSE). Gratuito, sem edição paga —
veja [Apoie o projeto](#apoie-o-projeto).
