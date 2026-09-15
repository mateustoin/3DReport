# Registro de decisões

Decisões aprovadas pelo responsável pelo projeto. Novas decisões são adicionadas
ao final, com data.

| # | Data | Decisão | Status |
|---|---|---|---|
| 1 | 2026-09-15 | Plataforma inicial: **desktop (JVM)**. Android planejado para depois, sem configuração agora | Aprovada |
| 2 | 2026-09-15 | Módulos: **`core`** (KMP puro, domínio + cálculo) e **`composeApp`** (UI Compose Multiplatform) | Aprovada |
| 3 | 2026-09-15 | Licença **proprietária** (todos os direitos reservados), com Edição Gratuita executável limitada ao orçamento básico | Aprovada |
| 4 | 2026-09-15 | Primeira entrega: base do projeto + motor de cálculo testado + tela provisória + documentação | Aprovada |
| 5 | 2026-09-15 | Energia: `horas · kW · preço_kWh` (planilha usa kWh = 1,23) | Aprovada |
| 6 | 2026-09-15 | Manutenção: substituir "depreciação %/h" por **custo fixo em R$/hora** | Aprovada |
| 7 | 2026-09-15 | Taxas de marketplace (Shopee) e custo de spray: **fora da edição gratuita** | Aprovada |
| 8 | 2026-09-15 | Falhas e acabamento: percentuais **sobre o custo de material** | Aprovada |

## Pendentes de aprovação

- UI/UX da tela de orçamento (layout, fluxo, campos).
- Padrão de apresentação (proposto: MVVM com ViewModel + StateFlow).
- Persistência de filamentos e configurações (onde e como salvar).
- Filamentos pré-cadastrados (ex.: PLA/ABS/PETG com densidades padrão).
- Mecanismo de separação entre edição gratuita e paga.
- Convenções adotadas na base e passíveis de revisão: pacote `com.threedreport`,
  identificadores em inglês com KDoc em português, valores monetários em `Double`.
