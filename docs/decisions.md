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
| 15 | 2026-09-15 | Projeto vira **gratuito e de código aberto**: sem edição paga, sem assinatura, sem verificação de licença no software. Licença de código: **Apache 2.0** (substitui a licença proprietária da decisão 3). Sustentação financeira via doação voluntária (Buy Me a Coffee, link no README/`.github/FUNDING.yml`). O repositório GitHub, hoje privado, será aberto para o público pelo responsável do projeto quando ele decidir (não pelo Claude) | Aprovada |

## Pendentes de aprovação

- Convenções adotadas na base e passíveis de revisão: pacote `com.threedreport`,
  identificadores em inglês com KDoc em português, valores monetários em `Double`.
- Usuário do Buy Me a Coffee (hoje `SEU_USUARIO` como placeholder em
  `.github/FUNDING.yml` e `README.md`).
