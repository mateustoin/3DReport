# 3DReport

Aplicativo **gratuito e de código aberto** para criação de **orçamentos de
impressão 3D**. A partir do filamento usado, da impressora escolhida (cada uma
com seu próprio perfil salvo) e dos dados da peça (comprimento em metros e
tempo de impressão), calcula o **valor de produção** e o **valor de venda**
com a margem de lucro desejada.

> **Status:** motor de cálculo implementado e testado; telas de orçamento
> (com salvar e exportar), histórico de orçamentos, cadastro de filamentos,
> cadastro de impressoras e configurações gerais funcionando, com
> persistência real em disco. Veja o backlog em [docs/roadmap.md](docs/roadmap.md).

## Funcionalidades

| Funcionalidade | Estado |
|---|---|
| Cálculo de custo de produção e preço de venda | Pronto (`core`) |
| Tela de orçamento (escolhe filamento + impressora, entra comprimento/tempo → produção/venda) | Pronto |
| Salvar orçamento (nome, foto e link do modelo opcionais) | Pronto |
| Histórico de orçamentos (consultar, baixar foto, excluir) | Pronto |
| Exportar orçamento — PDF e copiar/colar (nome + valor de venda + foto no PDF) | Pronto |
| Cadastro de filamentos (catálogo salvo) | Pronto |
| Cadastro de impressoras (perfis salvos: consumo, manutenção, investimento) | Pronto |
| Configurações gerais do negócio (energia, falhas, acabamento, margem) | Pronto |
| Persistência em disco (`~/.3dreport/`, arquivos JSON + fotos) | Pronto |
| Marca d'água personalizada no PDF (texto, configurável em Configurações) | Pronto |
| Exportar vários orçamentos selecionados num PDF só (um por página) | Pronto |
| Serviços opcionais no orçamento (pintura, lixamento, embalagem/spray etc.) | Pronto |
| Taxa de marketplace (ex.: Shopee), ajusta o valor de venda | Pronto |
| Link do modelo clicável (abre no navegador) | Pronto |
| Versão do app + rodapé com autor/GitHub/doação + ajuda | Pronto |
| Confirmação antes de excluir (filamentos, impressoras, serviços, histórico) | Pronto |
| Peso da peça no Histórico (uso interno, calculado da densidade/comprimento) | Pronto |

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

## Instaladores

Instaladores nativos (`.deb`/`.msi`/`.dmg`) são gerados por CI a cada release
e publicados em [GitHub Releases](https://github.com/mateustoin/3DReport/releases)
— ainda não há um release publicado (ver [docs/roadmap.md](docs/roadmap.md)).
Como não são assinados digitalmente (custo recorrente incompatível com o
modelo 100% gratuito/doação), o Windows pode avisar "Editor desconhecido"
(clique em "Mais informações → Executar assim mesmo") e o macOS pode
bloquear a abertura na primeira vez (clique direito → "Abrir").

## Estrutura

```
core/        Domínio e motor de cálculo (Kotlin Multiplatform puro, sem UI)
composeApp/  Interface Compose Multiplatform (desktop)
docs/        Documentação
```

## Documentação

| Documento | Conteúdo |
|---|---|
| [CHANGELOG.md](CHANGELOG.md) | O que mudou em cada versão |
| [docs/development.md](docs/development.md) | Setup, IDE, executar, testar, empacotar |
| [docs/architecture.md](docs/architecture.md) | Módulos, plataformas, convenções |
| [docs/pricing-formulas.md](docs/pricing-formulas.md) | Parâmetros e fórmulas de cálculo |
| [docs/decisions.md](docs/decisions.md) | Decisões aprovadas e pendentes |
| [docs/roadmap.md](docs/roadmap.md) | Backlog de evoluções e próximas implementações |
| [core/README.md](core/README.md) | Módulo `core` |
| [composeApp/README.md](composeApp/README.md) | Módulo `composeApp` |

## Licença

Código aberto sob [Apache License 2.0](LICENSE). Gratuito, sem edição paga —
veja [Apoie o projeto](#apoie-o-projeto).
