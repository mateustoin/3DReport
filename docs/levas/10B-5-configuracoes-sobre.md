# Leva 10B, parte 5: Configurações, Sobre e os diálogos que faltam

> Especificação de trabalho. Direção visual: decisão 127
> ([design-system.md](../design-system.md)). Segue os moldes já prontos: painel e
> seções dos cadastros, diálogos do kit, textos em recursos. O Dashboard tem
> especificação própria, depois da escolha do layout.

## Objetivo

As telas e diálogos que ainda usam a aparência antiga passam pro sistema de design:
Configurações (`ui/settings/`), Sobre (`ui/about/`), primeiro uso
(`ui/onboarding/`), modelos de orçamento (`ui/templates/`) e o visualizador de STL
(`ui/viewer/`). Mesmo comportamento, mesmos dados.

## Fora de escopo

- `core/`, fórmulas e formato dos dados.
- Dashboard (parte seguinte) e o documento exportado.
- Atualizar o app por dentro: a seção de atualização do Sobre continua só avisando
  e abrindo a página da versão, como hoje. A instalação por dentro tem decisão
  própria.
- Opção nova de configuração.

## 1. Configurações

- **Estrutura:** continua com a lista de seções à esquerda (Negócio, Canais de
  venda, Documentos pro cliente, Aparência, Backup) e o conteúdo à direita. A lista
  usa o item selecionável do kit, não um componente próprio. O conteúdo não passa
  de 760 de largura, em grade de três colunas.
- **Cabeçalho:** título "Configurações", sem subtítulo.
- **Salvar:** a barra de alterações por salvar continua, com um só botão âmbar
  ("Salvar") e "Descartar" secundário. A seção com alteração pendente continua
  marcada na lista, com texto, não só com cor.
- **Campos:** todos do kit, com rótulo acima, erro por campo e ajuda em `HelpHint`.
  Nenhum parágrafo fixo de explicação.
- **Canais de venda:** lista no molde dos cadastros (linha, ações no hover ou foco,
  formulário em painel ou diálogo do kit). O editor de faixas de taxa usa a tabela
  do kit.
- **Tarifa de energia** (`EnergyTariffDialog`): `AppDialog` e campos do kit.
- **Documentos pro cliente:** campos do kit; a prévia da marca, se houver, usa os
  tokens.
- **Aparência:** tema (claro, escuro, do sistema) e moeda com o seletor do kit.
- **Backup:** ações como botões secundários; ação destrutiva (restaurar por cima)
  com confirmação do kit e texto claro do que será substituído.
- **Estado:** cada ViewModel da pasta expõe um `StateFlow` de estado e avisos em
  `UiNotice`. Os ViewModels separados por seção podem continuar separados.

## 2. Sobre

- Marca "3D" e "Report" como na barra lateral, versão, o que o app faz em poucas
  linhas (sem lista com marcador feito de caractere), links (site, código, doação)
  como ações do kit, licenças de terceiros acessíveis por um link.
- Seção de atualização com o estado em texto ("Você está na versão mais recente",
  "Versão X disponível") e a ação que resolve.

## 3. Diálogos

- **Primeiro uso** (`OnboardingDialog`): `AppDialog`, passos curtos, um botão âmbar
  por passo.
- **Modelos de orçamento** (`TemplateListDialog`): lista no molde dos cadastros
  dentro do `AppDialog`.
- **Visualizador de STL:** moldura, botões e textos do kit; a área 3D não muda.

## 4. Textos, testes e capturas

- Todos os textos dessas pastas em `strings.xml` (`settings_*`, `about_*`,
  `onboarding_*`, `templates_*`, `viewer_*`). Sem travessão, aspas sem barra, plural
  de verdade.
- Testes dos ViewModels adaptados sem perder caso.
- Capturas em claro e escuro: cada seção de Configurações, Sobre, primeiro uso,
  modelos e tarifa de energia.

## Critérios de aceite

- `scripts/check.sh build` OK.
- Nenhum literal de texto, `Color(0x...)`, `.dp` de layout, raio ou componente do
  Material 3 cru nessas pastas (o que faltar entra no kit).
- Depois desta parte e do Dashboard, um `grep` por `Text("` com literal em
  `composeApp/src/commonMain/.../ui/` só acha o que estiver listado como exceção.
- `ls -la ~/.3dreport` igual antes e depois.

## Só dá pra conferir com o app aberto

Backup e restauração de verdade (numa pasta de teste), troca de tema e de moeda
refletindo nas outras telas, verificação de atualização com e sem internet, e o
primeiro uso numa pasta de dados vazia.
