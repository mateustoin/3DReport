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

### B. Aprovados em 2026-11-16 (escolhas na tabela de decisões)

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

## Escolhas de aparência (aprovadas em 2026-11-16)

Esboços em https://claude.ai/artifact/UCQwjvAfNf9H6BrpmD1g6W (página privada do
responsável do projeto).

- **PDF, opção A.** Cabeçalho com a marca do vendedor à esquerda e o contato à
  direita, separados do corpo por uma linha. Linha de pedido e emissão, e o nome da
  peça em Space Grotesk. Foto na largura útil da página, sobre um fundo cinza claro,
  com a marca d'água por cima quando ligada. Abaixo, um bloco cinza claro com o valor
  em número grande à esquerda e, à direita, "Entrega até" e "Válido até". Rodapé com
  a marca e "Gerado com 3DReport". Texto em Inter. Sem cor de marca do app: o
  documento é do vendedor. Todas as opções de hoje continuam valendo (marca d'água,
  rodapé, borda, tempo de impressão, nome do cliente, validade), e o PDF com vários
  itens e a página de detalhes seguem o mesmo estilo.
- **Imagem, opção A.** Fundo `navy`, peça no alto, faixa inferior mais escura
  (`#081526`) com o nome e o preço à esquerda (só o preço em `amber`, Space Grotesk)
  e, à direita, a entrega, a marca com o contato e "Gerado com 3DReport".
- **Peça escura.** Campo novo `BrandingSettings.imageBackground` (`AUTO`, `DARK`,
  `LIGHT`), que nasce `AUTO`. Em `AUTO`, o app mede o brilho médio dos pixels não
  transparentes da foto: peça escura sai no fundo claro (a opção B dos esboços:
  fundo `#F6F8FB`, peça num cartão branco, preço em texto escuro, entrega em âmbar de
  texto), as demais no azul-marinho. Foto sem transparência (enviada pela pessoa)
  conta pelo brilho da imagem inteira. O limiar é uma constante testada, com teste
  pra peça preta, branca e cinza. Seletor "Fundo da imagem" em Documentos pro
  cliente, com `HelpHint`. Dado gravado sem o campo abre como `AUTO` (teste de
  compatibilidade). O modelo de orçamento não guarda esse campo.
- **Ícone, refinamento 1.** `docs/design/icone-camadas.svg` passa a ser o desenho
  refinado (camadas de 16 de altura, bico com corpo terminando sobre a ponta da
  camada âmbar). Os arquivos de 16 a 32 px levam um contorno claro fino no quadro.
  Gera `composeApp/packaging/icons/` (png, ico, icns), o ícone da janela e os do
  site (favicon e imagens de compartilhamento).

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
| | PDF: opção A (foto na largura da página e bloco de preço com entrega e validade) | aprovada em 2026-11-16 |
| | Imagem: opção A (azul-marinho, só o preço em âmbar) | aprovada em 2026-11-16 |
| | Imagem com peça escura: fundo automático pelo brilho da peça, com seletor em Documentos pro cliente | aprovada em 2026-11-16 |
| | Ícone: refinamento 1 (camadas mais grossas, bico depositando), com contorno claro nos tamanhos de 16 a 32 px | aprovada em 2026-11-16 |
| | Nome automático com campo `autoNamed` e migração | aprovada em 2026-11-16 |
| | Formato de data e número num ponto só | aprovada em 2026-11-16 |

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
