# Changelog

Todas as mudanças notáveis deste projeto são documentadas aqui. O formato é
baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/), e o
projeto segue [SemVer](https://semver.org/lang/pt-BR/) (decisão 27 em
[docs/decisions.md](docs/decisions.md)).

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
