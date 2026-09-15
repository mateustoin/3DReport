# Registro de decisões

Decisões aprovadas pelo responsável pelo projeto. Novas decisões são adicionadas
ao final, com data.

| # | Data | Decisão | Status |
|---|---|---|---|
| 1 | 2026-09-15 | Plataforma inicial: **desktop (JVM)**. Android planejado para depois, sem configuração agora | Aprovada |
| 2 | 2026-09-15 | Módulos: **`core`** (KMP puro, domínio + cálculo) e **`composeApp`** (UI Compose Multiplatform) | Aprovada |
| 3 | 2026-09-15 | Licença **proprietária** (todos os direitos reservados), com Edição Gratuita executável limitada ao orçamento básico | Substituída pela 15 |
| 4 | 2026-09-15 | Primeira entrega: base do projeto + motor de cálculo testado + tela provisória + documentação | Aprovada |
| 5 | 2026-09-15 | Energia: `horas · kW · preço_kWh` (planilha usa kWh = 1,23) | Aprovada |
| 6 | 2026-09-15 | Manutenção: substituir "depreciação %/h" por **custo fixo em R$/hora** | Aprovada |
| 7 | 2026-09-15 | Taxas de marketplace (Shopee) e custo de spray: **fora da edição gratuita** | Substituída pela 15 |
| 8 | 2026-09-15 | Falhas e acabamento: percentuais **sobre o custo de material** | Aprovada |
| 9 | 2026-09-15 | UI/UX da tela de orçamento: **2 telas** (Orçamento + Configurações) com navegação por abas; Orçamento só pede o que muda por peça (filamento, comprimento, tempo), Configurações reúne os parâmetros da operação (energia, manutenção, falhas, acabamento, máquina, margem) | Substituída pela 13 |
| 10 | 2026-09-15 | Padrão de apresentação: **MVVM** (ViewModel + StateFlow), um ViewModel por tela | Aprovada |
| 11 | 2026-09-15 | Persistência de filamentos e configurações: **em memória por enquanto** (repositórios com valores padrão pré-carregados); armazenamento real fica para decisão futura | Substituída pela 14 |
| 12 | 2026-09-15 | Impressora vira entidade própria, **`PrinterProfile`** (consumo, manutenção, investimento da máquina), salva em catálogo e escolhida por orçamento — uma pessoa pode ter várias impressoras. `PricingSettings` fica restrito aos parâmetros gerais do negócio, iguais para qualquer impressora (energia, falhas, acabamento, administrativo, margem) | Aprovada |
| 13 | 2026-09-15 | Navegação ampliada para **4 abas**: Orçamento, Filamentos, Impressoras, Configurações. Orçamento passa a ter 2 dropdowns (Filamento e Impressora); Filamentos e Impressoras têm tela própria de cadastro (listar/adicionar/editar/excluir) | Aprovada |
| 14 | 2026-09-15 | Persistência real de filamentos, impressoras e configurações: **arquivo JSON local** (`kotlinx.serialization`, em `~/.3dreport/`), por trás da mesma interface de repositório (`expect`/`actual`) | Aprovada |
| 15 | 2026-09-15 | Projeto vira **gratuito e de código aberto**: sem edição paga, sem assinatura, sem verificação de licença no software. Licença de código: **Apache 2.0** (substitui a licença proprietária da decisão 3). Sustentação financeira via doação voluntária (Buy Me a Coffee: `mateustoin`, link no README/`.github/FUNDING.yml`). O repositório GitHub, hoje privado, será aberto para o público pelo responsável do projeto quando ele decidir (não pelo Claude) | Aprovada |
| 16 | 2026-09-15 | Convenções da base **ratificadas como estão** (revisão pedida após o projeto virar open source): pacote `com.threedreport`; identificadores em inglês com KDoc/comentários em português (reavaliar se aparecerem colaboradores que não leem português); valores monetários em `Double`, arredondados só na exibição (reavaliar só se algum bug de arredondamento aparecer na prática) | Aprovada |
| 17 | 2026-09-15 | Priorização do roadmap: **funcionalidades de produto/UI-UX primeiro**, depois instaladores desktop, depois infraestrutura/qualidade (adiada — repositório ainda privado), **Android por último** (só quando o projeto estiver consolidado e houver demanda, sem previsão). Detalhes de "Salvar um orçamento" (nome opcional com padrão automático; foto opcional, no PDF e no histórico, não no copiar/colar; link do modelo opcional, uso só interno, nunca exportado) | Aprovada |
| 18 | 2026-09-15 | Salvar orçamento: formulário **direto na tela de Orçamento** (sem diálogo separado, mesmo padrão dos formulários de Filamentos/Impressoras). Primeira leva de implementação cobre **salvar + histórico** (listar, ver, baixar foto, excluir); exportar em PDF/copiar-colar fica pra uma implementação seguinte | Aprovada |
| 19 | 2026-09-15 | Exportação (PDF/copiar-colar), por ser **documento pro cliente**: mostra só nome + **valor de venda** + foto (PDF) — produção e lucro (uso interno) **nunca aparecem**, junto com o link do modelo (já não aparecia). Exportação fica disponível **só na aba Histórico**, sobre um orçamento já salvo (não direto na tela de Orçamento). Marca d'água personalizada e exportar vários orçamentos num PDF só ficam no roadmap, não entraram nesta leva | Aprovada |
| 20 | 2026-09-15 | Marca d'água no PDF: **só texto** por ora (upload de imagem/logo fica pro roadmap se fizer falta depois), configurada em Configurações. Visual fixo — diagonal, cinza claro translúcido, centralizada — **sem** controles de posição/opacidade pelo usuário nesta primeira versão | Aprovada |
| 21 | 2026-09-15 | Correção: marca d'água ficava **encoberta pela foto** (desenhada antes dela). Passa a ser desenhada **por cima de todo o conteúdo**, inclusive a foto — técnica padrão de marca d'água em documentos. Adicionado também um **rodapé profissional** (linha fina + nome da marca centralizado, no fim da página) quando há marca d'água configurada, pra reforçar a identificação mesmo se a diagonal passar despercebida | Aprovada |
| 22 | 2026-09-15 | Marca d'água diagonal e rodapé viram **opções independentes** (checkbox pra cada uma em Configurações); com um nome preenchido, salvar exige ao menos uma marcada. "Exportar PDF" passa a abrir o diálogo de salvar direto na pasta Documents/Documentos do usuário, em vez do padrão do sistema | Aprovada |
| 23 | 2026-09-15 | Bug de terceiros contornado: arrastar a janela entre monitores com DPI/escala diferentes deixava o conteúdo com o layout antigo até um redimensionamento manual — bug conhecido do Compose Desktop/Skiko (JetBrains/compose-multiplatform#3685 e relacionadas), sem correção oficial. Contornado detectando a troca de monitor e forçando um redimensionamento programático (`Main.kt`) | Aprovada |
| 24 | 2026-09-15 | Exportar vários orçamentos num PDF só: seleção múltipla direto na lista da aba Histórico (checkbox por linha), sem tela separada. Um orçamento por página, na ordem da lista; marca d'água/rodapé (se configurados) em todas as páginas | Aprovada |
| 25 | 2026-09-15 | Serviços opcionais no orçamento (aba "Serviços", catálogo nome+preço; checkboxes na tela de Orçamento). O valor dos serviços selecionados soma só no **Total (venda + serviços)** — o **Lucro exibido não muda**, porque o app não modela o custo de um serviço (só o preço cobrado do cliente), então tratar tudo como lucro puro seria enganoso | Aprovada |

## Pendentes de aprovação

Nenhuma no momento — ver [roadmap.md](roadmap.md) para as próximas evoluções.
