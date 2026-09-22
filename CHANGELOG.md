# Changelog

Todas as mudanças notáveis deste projeto são documentadas aqui. O formato é
baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/), e o
projeto segue [SemVer](https://semver.org/lang/pt-BR/) (decisão 53 em
[docs/decisions.md](docs/decisions.md), que revisa a decisão 27).

## [1.22.0] - 2026-09-22

### Adicionado
- Configurações de impressão (altura de camada, % de preenchimento, padrão
  de preenchimento e uso de suporte) agora podem ser guardadas junto com
  cada orçamento, pra recuperar depois no Histórico e replicar o mesmo
  padrão numa impressão futura da mesma peça, sem precisar lembrar de
  cabeça ou reabrir o fatiador. São preenchidas automaticamente ao
  importar um G-code do PrusaSlicer/Bambu Studio/OrcaSlicer (quando o
  arquivo gravar esses dados), ou manualmente pelo botão "Adicionar
  configurações de impressão" no Orçamento. No Histórico (lista ou
  Kanban), um botão "Configurações de impressão" no card permite
  consultar e editar direto ali, sem precisar reabrir a edição completa
  do orçamento. Uso só interno: não entra no cálculo do preço nem em
  nenhum export (PDF ou copiar/colar).

## [1.21.0] - 2026-09-21

### Adicionado
- Refinamento visual (decisão 73): números (preço, peso, tempo) passam a
  usar fonte monoespaçada em todo o app — Orçamento, Dashboard, Kanban e
  Histórico — dando aos valores uma leitura de "instrumento de medição",
  consistente em qualquer tela. O resultado do cálculo no Orçamento virou
  uma "nota": o valor cobrado do cliente em destaque no topo, com custo/
  lucro/serviços como itens abaixo, em vez de uma lista de linhas do
  mesmo peso visual. Cartões de estatística do Dashboard trocaram os 4
  blocos brancos com sombra por uma leitura em régua (número grande +
  rótulo, separados por uma linha fina). Status de pedido (Orçado →
  Entregue) ganhou uma cor de progresso fria→quente (reaproveitando as
  cores já existentes do app), visível como um ponto colorido no Kanban
  e no seletor de status do Histórico.

## [1.20.0] - 2026-09-21

### Adicionado
- Editar um orçamento salvo (Histórico → "Editar") agora abre um diálogo
  com o formulário completo, sem sair da tela de Histórico e sem trocar
  de aba — um botão "Cancelar edição" sempre visível no topo permite
  desistir sem precisar salvar antes. Antes, editar levava pra aba
  Orçamento e a única forma de desistir era rolar até o fim do
  formulário achar o botão de cancelar (ou salvar mesmo sem querer).

### Corrigido
- Quadro Kanban: soltar um card num espaço "vazio" de uma coluna (abaixo
  do último card, ou em qualquer parte de uma coluna sem nenhum item)
  agora também muda o status — antes só funcionava exatamente em cima de
  um card existente ou do título, porque cada coluna só tinha a altura
  do próprio conteúdo. Colunas agora esticam até a altura da mais alta.
- Cards do Kanban ficaram grandes demais depois da miniatura de foto
  (v1.19.1) — foto e texto agora ficam lado a lado, card bem mais
  compacto, mantendo a foto.

## [1.19.1] - 2026-09-21

### Corrigido
- Quadro Kanban: soltar um card em qualquer parte de uma coluna agora
  muda o status (antes só funcionava perto do título da coluna, porque a
  área "válida" da coluna era recortada pela rolagem da tela por trás).
  Também passou a mostrar um destaque na coluna sob o ponteiro durante o
  arrasto, indicando onde o card vai parar se for solto ali, e a exibir a
  miniatura da foto do orçamento (quando houver) no topo do card.

## [1.19.0] - 2026-09-21

### Adicionado
- Quadro Kanban no Histórico: alterne entre "Lista" e "Kanban" pra ver os
  orçamentos organizados em colunas por status (Orçado, Aprovado, Em
  impressão, Pronto, Entregue). Arraste um card pra outra coluna pra
  mudar o status, ou use o menu "⋮" do card (Editar/Duplicar/Excluir).

## [1.18.0] - 2026-09-21

### Adicionado
- Botão "Duplicar" no Histórico: reabre um orçamento salvo como rascunho
  na aba Orçamento (revise nome/cliente e outros dados antes de salvar),
  criando um orçamento novo em vez de sobrescrever o original. Se a foto
  ou o STL não mudarem, o arquivo em disco é reaproveitado, não duplicado.

### Corrigido
- Editar um orçamento sem trocar a foto/STL não regrava mais o arquivo à
  toa (mesma otimização usada pra duplicar).

## [1.17.0] - 2026-09-21

### Adicionado
- Fila de impressão na aba Impressoras: cada impressora mostra quanto
  tempo está ocupada agora, somando os orçamentos com status "Em
  impressão" que a usaram — ajuda a prometer prazo com mais segurança
  pro cliente.

## [1.16.0] - 2026-09-21

### Adicionado
- Nível de dificuldade de impressão sugerido (Fácil/Médio/Difícil),
  calculado a partir do STL anexado — combina forma (área vs. volume),
  % de overhang, contagem de triângulos e partes soltas no arquivo.
  Exibido ao vivo na tela de Orçamento, junto com área, volume, overhang
  e um aviso se a malha tiver furos/geometria não-manifold. Uso só
  interno, não entra no PDF nem no copiar/colar.

## [1.15.1] - 2026-09-21

### Corrigido
- Fotos `.webp` sumiam do PDF exportado (orçamento individual e catálogo),
  sem nenhum erro aparecer — a exportação usava um decodificador que não
  lê WebP sem plugin extra. Corrigido reaproveitando o mesmo decodificador
  (Skia) já usado pela miniatura no app, sem precisar de dependência nova.
  Conserta retroativamente fotos WebP já salvas antes desta versão.

## [1.15.0] - 2026-09-21

### Adicionado
- Editar um orçamento já salvo: botão "Editar" no Histórico reabre tudo
  na aba Orçamento (filamento, impressora, comprimento, tempo, serviços,
  marketplace, nome, foto, STL, link, cliente) pra corrigir sem refazer
  do zero. Salvar de novo atualiza o mesmo item — a data de criação
  original não muda, só aparece uma marca discreta "Editado em" ao lado
  dela no Histórico.

## [1.14.0] - 2026-09-21

### Adicionado
- Botão "Capturar como foto do orçamento": usa o ângulo/zoom atual do
  visualizador 3D como foto do orçamento, sem precisar de câmera externa.
- O formulário de salvar (nome, foto, STL, link, cliente) agora aparece
  desde o início na aba Orçamento — antes só surgia depois de um cálculo
  válido, o que impedia anexar STL/foto antes de preencher os outros
  campos.

### Corrigido
- Um STL muito pesado (centenas de milhares de triângulos) travava o app
  ao tentar pré-visualizar. Agora o app checa a complexidade antes de
  tentar renderizar e, se for grande demais, avisa e não tenta desenhar
  — o arquivo continua sendo salvo normalmente pra recuperar depois.

## [1.13.1] - 2026-09-21

### Corrigido
- Sentido do arrasto do visualizador 3D estava invertido (arrastar pra
  direita girava a peça pra esquerda).
- Dar zoom com a roda do mouse no visualizador também rolava a página
  inteira por trás.
- Visualizador 3D sem nenhuma borda/fundo — agora envolvido num card, pra
  ficar clara a área interativa.
- Desempenho em malhas STL densas: normal/centroide de cada triângulo
  agora são calculados uma vez (não a cada frame) e os `Path` de desenho
  são reaproveitados entre frames em vez de recriados a cada redesenho.

## [1.13.0] - 2026-09-21

### Adicionado
- Visualizador 3D do modelo STL anexado ao orçamento: gire com o mouse e
  dê zoom com a roda pra conferir a peça antes de fechar a venda.
  Renderizado no próprio app (sem depender de nenhum programa externo).

## [1.12.0] - 2026-09-21

### Adicionado
- Anexar um arquivo STL (opcional) ao orçamento, na tela de Orçamento —
  guardado no histórico (`~/.3dreport/models/`), com "Baixar STL" na aba
  Histórico, pra recuperar o modelo depois e reaproveitar numa venda
  futura da mesma peça. Uso só interno, nunca entra no PDF nem no
  copiar/colar.

## [1.11.3] - 2026-09-20

### Alterado
- Revertido o diálogo de escolher arquivo (G-code e foto) de volta pro
  nativo do sistema operacional (`java.awt.FileDialog`), como era antes da
  v1.11.2 — a troca pra `JFileChooser` filtrava por tipo de arquivo, mas o
  visual Swing destoava do resto do app. Aceita a limitação de não filtrar
  por extensão no Windows: é só uma conveniência de busca, não impede
  escolher o arquivo manualmente.

## [1.11.2] - 2026-09-20

### Corrigido
- A correção da v1.11.1 não resolvia de fato: o diálogo de "Preencher a
  partir do G-code" continuava mostrando todos os arquivos no Windows (o
  texto do filtro ia parar na caixa de nome do arquivo, não num filtro de
  tipo). Trocado o diálogo nativo (`java.awt.FileDialog`) por
  `javax.swing.JFileChooser`, que tem um filtro de tipo de arquivo que
  funciona de fato em qualquer sistema operacional — aplicado tanto no
  diálogo de G-code quanto no de escolher foto.

## [1.11.1] - 2026-09-20

### Corrigido
- O diálogo de "Preencher a partir do G-code" não filtrava por extensão no
  Windows (mostrava todos os arquivos) — o filtro programático não é
  respeitado pelo diálogo nativo do Windows; corrigido definindo o padrão
  de busca (`*.gcode;*.gco;*.g`) diretamente, que o Windows respeita.
  **Correção insuficiente — ver v1.11.2.**

## [1.11.0] - 2026-09-20

### Adicionado
- A importação de G-code agora também extrai a miniatura do modelo, quando
  o fatiador embute uma (PrusaSlicer, SuperSlicer, OrcaSlicer, Bambu
  Studio), e usa como foto do orçamento — só se nenhuma foto já tiver sido
  escolhida.
- Botão "Desfazer importação do G-code": limpa comprimento/tempo
  preenchidos automaticamente e remove a foto, se ela também tiver vindo
  do G-code.

## [1.10.0] - 2026-09-20

### Adicionado
- Botão "Preencher a partir do G-code" na tela de Orçamento: importa
  comprimento de filamento e tempo de impressão direto dos comentários de
  metadados do arquivo `.gcode` exportado pelo fatiador (PrusaSlicer, Bambu
  Studio/OrcaSlicer e Cura), em vez de digitar os dois campos na mão. Os
  campos continuam editáveis manualmente depois de importados.

## [1.9.0] - 2026-09-19

### Adicionado
- Preset de impressoras: botão "Escolher da lista" em Impressoras abre um
  catálogo pré-cadastrado com 39 modelos das principais marcas (Bambu Lab,
  Creality, Prusa, Elegoo, Anycubic, Flashforge, Snapmaker) — escolher um
  preenche nome e consumo (potência máxima do manual/ficha técnica oficial)
  no formulário de nova impressora, com busca por marca ou modelo. Escolher
  da lista nunca é obrigatório — cadastro manual continua funcionando igual.
- Tipo de material no cadastro de filamento (PLA, PETG, ABS, ASA, TPU,
  Nylon, PC, HIPS, PVA, PLA-CF, PETG-CF, Madeira ou personalizado) — escolher
  um tipo com densidade confiável já preenche o campo de densidade
  automaticamente (continua editável); tipos compostos (CF, madeira) não têm
  densidade sugerida, porque varia demais por fabricante.
- Chips de marcas conhecidas de filamento (internacionais e brasileiras) no
  cadastro, pra preencher o campo Marca sem digitar do zero.

## [1.8.0] - 2026-09-17

### Alterado
- Templates de orçamento: deixou de ter formulário próprio de criar/editar
  — Configurações → "Marca d'água do PDF" passa a ser o único lugar de
  edição, com um novo botão "Salvar como template" (só pede um nome).
- A aba "Templates" foi removida do menu principal — a lista de templates
  salvos ("Carregar"/"Excluir") agora abre como um diálogo, pelo botão
  "Ver templates salvos" ao lado de "Salvar como template".

### Adicionado
- Indicador de template ativo: badge "Ativo" ao lado do nome do template
  cujos campos batem com a marca d'água/rodapé em uso no momento.

## [1.7.0] - 2026-09-17

### Adicionado
- Múltiplas moedas: nova seção "Moeda" em Configurações (BRL, USD, EUR ou
  GBP), aplicada em toda a interface, no PDF exportado e no copiar/colar
  — cada moeda com seu próprio símbolo e convenção de separador decimal/
  milhar, não é só trocar "R$" por "$".

### Corrigido
- Valores monetários agora usam separador de milhar (ex.: "R$ 1.234,50"),
  que não existia antes.

## [1.6.0] - 2026-09-17

### Adicionado
- Catálogo/portfólio exportável: no Histórico, com 1+ orçamentos
  selecionados, um botão "Exportar catálogo (PDF)" gera um PDF em grade
  (foto + nome + preço, várias peças por página) — pra mandar pro cliente
  ou postar em grupo de venda, diferente do export "um orçamento por
  página" que já existia.
- Templates de orçamento: nova aba "Templates" com presets nomeados de
  marca d'água/rodapé do PDF — "Usar este" aplica o preset na configuração
  ativa (Configurações → Marca d'água do PDF), sem precisar reescrever o
  texto toda vez que quiser trocar o visual.

## [1.5.0] - 2026-09-17

### Adicionado
- Atalhos de teclado: `Ctrl`/`Cmd+1` a `7` pula entre as abas; `Ctrl`/`Cmd+S`
  salva o orçamento atual e `Ctrl`/`Cmd+N` limpa a tela pra começar um novo
  (aba Orçamento); `Esc` cancela o formulário aberto em Filamentos/
  Impressoras/Serviços. Listados no diálogo de Ajuda.

## [1.4.0] - 2026-09-17

### Adicionado
- Ícone próprio do app (antes saía com o ícone padrão do Java/jpackage).
- Instalador Windows (e Linux) passa a criar atalho na área de trabalho
  e no menu Iniciar ao instalar.

## [1.3.0] - 2026-09-16

### Adicionado
- Um filamento (mesma marca/preço/densidade) agora pode ter **várias
  cores cadastradas**, cada uma com seu próprio controle de estoque
  ("Em estoque"/"Acabou") — evita duplicar o cadastro só porque muda a
  cor do rolo. Alternar o estoque de uma cor é um clique direto no chip
  da cor na lista de Filamentos, sem abrir o formulário.
- Campo de **cor personalizada** (hex, com prévia) no cadastro de
  filamento, além da paleta de cores prontas.
- Tela de Orçamento ganha um dropdown "Cor" quando o filamento escolhido
  tem mais de uma cor em estoque — não afeta o cálculo, só fica
  registrado no orçamento salvo e no Histórico (uso interno).

## [1.2.0] - 2026-09-16

### Adicionado
- Controle de estoque de filamento: marca e cor (visual + nome escrito) no
  cadastro, e um estado "Em estoque"/"Acabou" alternado manualmente por
  filamento (sem tentar calcular automaticamente pelo consumo). Filamento
  esgotado continua no catálogo (acinzentado) mas some da seleção na tela
  de Orçamento.

## [1.1.0] - 2026-09-16

### Adicionado
- Tema claro/escuro, com opção de seguir o tema do sistema (padrão) ou
  escolher manualmente em Configurações → Aparência. Paleta de cores
  customizada (azul petróleo + laranja âmbar) no lugar do roxo padrão do
  Material3.
- Cliente vinculado ao orçamento (nome + contato, opcional) — uso só
  interno, nunca aparece no PDF ou no copiar/colar.
- Status do pedido no Histórico (Orçado → Aprovado → Em impressão → Pronto
  → Entregue), editável direto na lista.
- Busca e filtro no Histórico: por nome/cliente, por status e por um atalho
  de período (7 dias, 30 dias, este mês).
- Nova aba Dashboard: total vendido, lucro acumulado e filamento mais
  usado, recortados pelo mesmo atalho de período do Histórico.
- Estado vazio nas listas de Filamentos, Impressoras e Serviços (antes não
  mostravam nada quando não havia nenhum item cadastrado).

## [1.0.0] - 2026-09-15

### Adicionado
- Peso estimado da peça (a partir da densidade do filamento e do comprimento
  usado) exibido no Histórico — informação só para o criador, não entra no
  PDF nem no texto de copiar/colar.
- Confirmação antes de excluir um filamento, impressora, serviço ou
  orçamento salvo: um diálogo nomeia o item antes de remover.

## [0.2.0] - 2026-09-15

### Adicionado
- Taxa de marketplace (ex.: Shopee): configurável em Configurações,
  aplicável por orçamento — o valor de venda é ajustado pra manter o lucro
  real, em vez de simplesmente somar a taxa ao preço do cliente.
- Link do modelo (aba Orçamento e Histórico) agora é um hyperlink clicável,
  abrindo direto no navegador padrão do sistema.
- Rodapé fixo com a versão do app, nome do autor, link do GitHub e link de
  doação (Buy Me a Coffee), além de um botão de Ajuda com um resumo do app.
- Versionamento do app (SemVer), exibido no rodapé e usado no instalador.

## [0.1.0] - 2026-09-15

### Adicionado
- Motor de cálculo de orçamento: produção, venda e lucro, com margem
  configurável.
- Cadastro de filamentos, impressoras (perfis com consumo/manutenção/
  investimento) e serviços opcionais (pintura, lixamento, embalagem etc.).
- Tela de Orçamento: escolhe filamento + impressora, calcula o resultado, e
  permite salvar (nome, foto e link do modelo, todos opcionais).
- Histórico de orçamentos salvos, com exportação em PDF (individual ou
  vários juntos num só arquivo) e cópia de texto simplificado.
- Marca d'água personalizada (diagonal e/ou rodapé) no PDF exportado.
- Persistência local em `~/.3dreport/` (arquivos JSON + fotos).
