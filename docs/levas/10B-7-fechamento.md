# Leva 10B, parte 7: fechamento da 2.2

> Especificação de trabalho. O que falta da Leva 10B depois das telas (partes 1 a 6)
> pra fechar a versão 2.2 (decisão 136). Direção visual: decisão 127
> ([design-system.md](../design-system.md)).

## Objetivo

Terminar a preparação pra tradução (nenhum texto pro público escrito no código nem no
`core`), levar o visual novo ao que o cliente recebe e ao site, e trocar o ícone.

## Itens

### A. Sem mudança pra quem usa (pode seguir sem nova aprovação)

1. **Rótulos fora do `core`.** `OrderStatus`, `QuoteKind` e `ThemeMode` perdem o
   `label` em português. O rótulo passa pra interface (`strings.xml`, uma função
   por enum em `ui/text/` ou equivalente) e pro que exporta. O módulo `web` não pode
   quebrar: se a calculadora do site usa algum rótulo, ele passa a vir da fachada.
   Os valores gravados em disco (nomes dos enums) não mudam, com teste de
   compatibilidade.
2. **PDF, mensagem e imagem com textos em recursos.** `QuotePdfExporter`,
   `QuoteImageExporter` e o texto da mensagem pro cliente leem os textos de
   `strings.xml` fora da tela (`getString`), com plural de verdade. **A aparência e o
   conteúdo não mudam nesta etapa:** o PDF, a imagem e a mensagem gerados antes e
   depois têm de sair com o mesmo texto, conferido por teste sobre um orçamento de
   exemplo.
3. **Número lido com o separador da moeda nos cadastros e no Orçamento.** Achado da
   revisão de código: `PrinterListViewModel`, `FilamentListViewModel`,
   `ServiceListViewModel`, `ConsumableListViewModel`, `QuoteViewModel` e
   `PrinterFormState` leem com vírgula fixa. Passam a ler com o separador da moeda
   escolhida, como Configurações, canais e primeiro uso já fazem. Em real nada muda.
   Testes com BRL e com uma moeda de decimal ".".
4. **Configurações em janela estreita.** Com a lista de seções em 264, os rótulos
   dos campos truncam numa janela de 900 de largura. A lista volta pra 200 e a nota
   "Não salvo" vai pra baixo do nome da seção.

### B. Precisa de aprovação antes de implementar

5. **Nome automático do pedido sem depender do idioma** (muda o formato gravado).
   Hoje o pedido sem nome ganha o prefixo "Orçamento - " e é reconhecido por esse
   texto. Proposta: campo novo `autoNamed` no registro, que nasce `false`; ao abrir
   dado antigo, o registro cujo nome bate com o padrão antigo é marcado uma vez
   (migração com teste, regra da decisão 106); o nome mostrado continua igual.
6. **Datas e números pelo idioma.** `DateFormat.jvm.kt` usa `pt-BR` fixo. Proposta:
   o formato passa a sair de um ponto só (idioma do app), que na 2.2 continua
   português. Nada muda na tela; é só preparo pra Leva 11B.
7. **PDF e imagem no visual novo.** Opções de layout apresentadas pro responsável do
   projeto, e só a escolhida é implementada (em cima do item 2).
8. **Ícone novo** (decisão 128). Refinamentos apresentados nos tamanhos reais, e o
   escolhido vira os arquivos do instalador, da janela e do site.
9. **Site:** capturas novas do app, ícone e marca novos, textos atualizados. O layout
   das páginas não muda (redesenho fica pra depois).

## Fora de escopo

- Tradução pra inglês (Leva 11B).
- Leva 10C e atualização por dentro do app (versão seguinte, decisão 136).
- Redesenho do site.
- Fórmulas de preço.

## Decisões

| Nº | Decisão | Situação |
|---|---|---|
| | PDF e imagem: opções de layout antes de implementar | aprovada em 2026-11-16 |
| | Ícone: ver refinamentos antes de gerar os arquivos | aprovada em 2026-11-16 |
| | Site na 2.2: capturas, ícone, marca e textos, sem redesenho | aprovada em 2026-11-16 |
| | Nome automático com campo `autoNamed` e migração | proposta |
| | Formato de data e número num ponto só | proposta |

## Modelo e cálculo (core e dados)

- Item 1: enums sem `label`; nomes gravados iguais.
- Item 5 (se aprovado): `SavedQuote.autoNamed: Boolean = false`, migração na leitura,
  teste com arquivo da 2.1.
- Nenhuma fórmula muda e nenhum preço muda sozinho.

## Local ou conectado

Tudo local. Nada novo sai do computador.

## Critérios de aceite

- `scripts/check.sh build` OK.
- `grep` por literal de texto pro público em `core/src/commonMain` (rótulos de enum) e
  nos exportadores não acha nada fora de exceções listadas.
- PDF, imagem e mensagem de um orçamento de exemplo iguais antes e depois do item 2.
- `ls -la ~/.3dreport` igual antes e depois.
