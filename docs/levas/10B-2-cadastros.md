# Leva 10B, parte 2: cadastros na nova interface

> Especificação de trabalho. A direção visual é a da decisão 127
> ([design-system.md](../design-system.md)). Esta parte define o padrão que as
> outras telas de lista vão copiar, por isso começa pelos dois cadastros mais
> simples e para pra aprovação antes de replicar.

## Objetivo

Insumos e Serviços reescritos uma vez só: visual novo, padrão de estado novo e
textos em recursos traduzíveis. Junto, nasce a infraestrutura de textos e os
componentes de lista e de formulário do kit. Depois da aprovação das capturas,
Filamentos e Impressoras seguem o mesmo molde.

## Fora de escopo

- Filamentos e Impressoras (etapa seguinte, depois da aprovação).
- Qualquer mudança no que os cadastros guardam, em `core/` ou no formato dos dados.
- Traduzir pra outro idioma: só português, mas já em recurso.
- Orçamento, Pedidos, Catálogo, Dashboard, Configurações e Sobre.

## 1. Textos em recursos (infraestrutura)

- Arquivo de textos do Compose Multiplatform em
  `composeApp/src/commonMain/composeResources/values/strings.xml`, lido com
  `stringResource` nas telas. Plural de verdade onde houver contagem
  (`pluralStringResource`), nunca "(s)".
- **Nome das chaves:** `<tela>_<o que é>`, em inglês e minúsculas, como
  `consumables_title`, `consumables_empty`, `services_field_price`. Texto usado em
  mais de uma tela usa o prefixo `common_` (`common_save`, `common_cancel`,
  `common_edit`, `common_archive`, `common_delete`).
- **Mensagem montada fora da tela.** ViewModel não chama `stringResource`. Ele
  expõe o texto como dado: um tipo pequeno em `ui/text/` que carrega o
  `StringResource` e os argumentos, e uma função `@Composable` que o transforma em
  `String`. Erros de formulário e avisos usam esse tipo. Mensagem de exceção
  (`exception.message`) não vai mais pra tela: o ViewModel valida os campos e
  escolhe a chave.
- Os textos de hoje são o ponto de partida, revistos pelo sistema de design:
  curtos, sem travessão, do ponto de vista de quem usa. Frase que só explicava a
  tela sai.

## 2. Padrão de estado

Cada tela tem **um** `StateFlow<XUiState>` (itens, arquivados, formulário aberto
ou não, pergunta de exclusão pendente) e um fluxo de avisos de uma vez só. A tela
observa e envia eventos. Os ViewModels continuam em Kotlin puro, recebendo o
repositório por construtor, e continuam criados em `AppViewModels`.

## 3. Layout das telas de cadastro

- **Cabeçalho de tela** do kit: título à esquerda, ação principal em âmbar à
  direita ("Novo insumo"). Sem subtítulo.
- **Lista em linhas**, não em cartões: uma linha por item com o nome em
  `bodyStrong`, os dados secundários em `caption` e o valor alinhado à direita com
  algarismos tabulares. As ações (editar, arquivar, excluir) aparecem ao passar o
  mouse e ao focar a linha pelo teclado, como botões de ícone com descrição e
  dica. Clicar na linha abre a edição. Divisor de 1 px entre linhas.
- **Formulário num painel à direita**, com a mesma largura do painel de resultado
  do Orçamento (380), fundo `card` e borda `line` à esquerda. Abre ao criar ou
  editar e fecha ao salvar, cancelar ou com Esc. A lista continua visível ao lado.
  Abaixo de 960 de largura útil, o painel ocupa a área toda. Rodapé do painel:
  Salvar (âmbar) e Cancelar.
- **Arquivados** numa seção recolhida no fim da lista, com a contagem.
- **Estado vazio:** uma frase e o botão que cria o primeiro item.
- **Largura:** a lista não passa de 760 quando o painel está fechado.
- Excluir e arquivar seguem as regras de hoje (`DeleteOrArchiveDialog`: item usado
  em pedido só pode ser arquivado), com o diálogo no estilo novo.

## 4. Kit

Entram em `ui/components/`, pra todas as telas de lista reaproveitarem: cabeçalho
de tela, linha de lista com ações, painel lateral de formulário com rodapé, seção
recolhível de arquivados, estado vazio com ação, e o diálogo padrão. O que já
existe e serve (`AppButton`, `AppField`, `EmptyState`, `ConfirmDialog`,
`Archive.kt`) é adaptado, não duplicado. O que ficar sem uso depois é removido.

## 5. Capturas

O `ScreenshotRenderer` passa a gerar, em claro e escuro: Insumos com a lista, e
Serviços com o painel de edição aberto. Use `-Pscreenshots.dir` pra gravar fora do
repositório; os prints públicos de `site/assets/screenshots/` não mudam nesta etapa.

## Critérios de aceite

- `scripts/check.sh build` passa.
- Nenhum texto literal de interface em `ui/consumables/` e `ui/services/`
  (`grep` por `Text("` e por aspas com letras acentuadas não acha nada).
- Nenhum `Color(0x`, `RoundedCornerShape(` nem `.dp` de layout nessas duas pastas.
- Testes dos dois ViewModels atualizados pro estado único, cobrindo o que cobriam.
- Esc fecha o painel; Tab percorre os campos na ordem; Ctrl+número continua
  trocando de tela.
- Capturas conferidas pelo `revisor-design` e aprovadas pelo responsável do projeto.
