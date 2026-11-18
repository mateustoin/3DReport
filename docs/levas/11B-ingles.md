# Leva 11B: inglês no app, no README e no site

> Especificação de trabalho da leva. É o que cada agente recebe pra fazer a sua
> parte. Depois do merge, o que vale como registro são as decisões em
> `docs/decisions/`; este arquivo fica como histórico do que foi pedido.
> Decisões aprovadas pelo responsável do projeto em 2026-11-18. A branch sai da
> Leva 11 (`feat/leva-11-receber-e-conversar`, PR #13), porque os textos dela
> também precisam de tradução.

## Objetivo

Quem não fala português usa o app inteiro em inglês, recebe PDF, mensagem e imagem
em inglês, e chega ao projeto por um site e um README em inglês.

## Itens

### A. Escolha do idioma (decisão 152)

1. Configurações > Aparência ganha **Idioma**: "Automático" (segue o sistema),
   "Português" e "English", no mesmo seletor e do mesmo jeito que o tema (decisão
   35). Os nomes das opções de idioma aparecem sempre no próprio idioma ("Português",
   "English"); "Automático" é traduzido.
2. **Automático**: sistema em português (qualquer região) usa português; qualquer
   outro idioma do sistema usa inglês (inglês é o idioma de reserva pra quem não fala
   português).
3. **Vale na hora**, sem reiniciar: a tela inteira, os avisos e os textos gerados
   depois (PDF, mensagem, imagem, recibo) passam a sair no idioma novo. Se o
   `engenheiro-core` concluir que a troca na hora não é segura com o Compose
   Multiplatform 1.12 (tela meio traduzida, texto guardado em estado que não muda),
   ele para e devolve a dúvida: a alternativa aprovada como reserva é "vale ao
   reiniciar", com o botão "Reiniciar agora".
4. Campo novo nasce em "Automático". Quem usa o sistema em português não vê nada
   mudar.

### B. Datas e números no formato do idioma (decisão 153)

5. `AppLocale` deixa de ser fixo e passa a ser o idioma em uso. Em português, tudo
   como hoje (`dd/MM/yyyy`, vírgula decimal).
6. Em inglês, a data segue a **região do sistema**: `en-US` (e sistema sem região)
   escreve `MM/dd/yyyy` e `MM/dd`; qualquer outra região (`en-GB`, `en-AU`, `en-IN`,
   e também sistema em outro idioma usando o app em inglês) escreve `dd/MM/yyyy` e
   `dd/MM`. Hora sempre 24 h. O decimal que não é dinheiro usa ponto em inglês
   ("12.5 h").
7. Dinheiro continua seguindo a moeda de cada orçamento (decisão 107), sem mudar:
   o formato do valor é da moeda, não do idioma.
8. O PDF, o recibo, a imagem e a mensagem usam o mesmo formato de data do app.

### C. Tradução do app (decisão 154)

9. Todos os recursos de `composeResources/values/` (cerca de 1.240 textos e 27
   plurais) ganham a versão em inglês em `values-en/`, pelo **glossário aprovado**
   (seção abaixo), que vira `docs/glossario.md`.
10. Os textos em inglês seguem as mesmas regras da escrita do projeto: frases curtas,
    do ponto de vista de quem vende, sem travessão, sem aspas escapadas com barra.
    Os marcadores (`%1$s`, `%2$d`) e o número de marcadores de cada texto são os
    mesmos do português.
11. **Teste de paridade** (falha o build): toda chave de `values/` existe em
    `values-en/` e vice-versa; mesmos marcadores em cada texto; mesmas quantidades
    em cada plural; nenhum texto em inglês com travessão.
12. Os modelos de mensagem da Leva 11 (decisão 148) ganham texto padrão em inglês,
    com os mesmos marcadores (`{cliente}` etc. continuam os mesmos marcadores; a
    lista mostrada em Configurações é traduzida, mas o marcador gravado não muda).
    Modelo editado pela pessoa não é traduzido.
13. Nome automático de orçamento (decisão 139) sai no idioma do app, como já previsto.

### D. Exemplos de outro país (decisão 155)

14. Em inglês, textos de ajuda e exemplos citam Etsy, eBay, Amazon e cartão no lugar
    de Shopee, Mercado Livre e maquininha; MEI e DAS saem (o imposto já é genérico,
    decisão 46).
15. A seção **Pix** de Configurações > Documentos pro cliente aparece só quando a
    moeda padrão é o real, em qualquer idioma. O Pix nos documentos já depende do
    pedido ser em real (decisão 145) e não muda.
16. Nenhum dado gravado muda: canais, serviços e cadastros que a pessoa já tem ficam
    como estão.

### E. README em inglês e notas das versões (decisão 156)

17. `README.en.md` com o mesmo conteúdo do `README.md`, adaptado pelo item 14, e um
    link "English" / "Português" no topo de cada um.
18. O `CHANGELOG.md` ganha, a partir da 2.2, um bloco `### In English` no fim de cada
    versão, com 3 a 6 linhas. O bloco `[Não publicado]` atual já ganha o seu,
    cobrindo a 2.2 inteira (Levas 10, 10B, 10C, 11 e 11B).
19. O workflow de release copia o bloco em inglês junto pra nota da release.
20. Os documentos internos (`docs/`, decisões, roadmap) continuam em português.

### F. Site em inglês (decisão 157)

21. Pasta `site/en/` com as 5 páginas (`index`, `manual`, `calculadora`,
    `imprensa`, `404`) e os textos da calculadora, **adaptadas** (item 14): mesma
    estrutura e layout (decisão 140), exemplos de Etsy, eBay e Amazon, sem o que só
    vale pro Brasil ("feito pro vendedor brasileiro" vira "any currency, any
    country").
22. A raiz continua em português e **não redireciona sozinha**. Um seletor "PT | EN"
    no menu de cada página leva à página equivalente no outro idioma.
23. `hreflang` (pt-BR, en, x-default = pt-BR) em todas as páginas, `sitemap.xml` com
    as páginas em inglês, `lang="en"` e dados estruturados (JSON-LD) em inglês nas
    páginas de `en/`.
24. A calculadora em inglês usa o mesmo `core` (via `web/`) e só troca textos; os
    textos dela saem do `calculadora.js` pra um objeto por idioma.
25. As capturas do app no site em inglês: geradas com o app em inglês pelo
    `ScreenshotRenderer` (`-Pscreenshots.lang=en`) em `site/assets/screenshots/en/`.

## Glossário (aprovado)

| Português | English |
|---|---|
| Orçamento | Quote |
| Pedido | Order |
| Catálogo | Catalog |
| Produto | Product |
| Sinal | Deposit |
| Insumos | Supplies |
| Serviços | Services |
| Canal de venda | Sales channel |
| Reserva pra falhas | Failure reserve |
| Orçado / Aprovado / Imprimindo / Pronto / Entregue / Cancelado | Quoted / Approved / Printing / Ready / Delivered / Cancelled |
| Gerado com 3DReport | Made with 3DReport |
| Parado há N dias | Waiting N days |

Termos que não estão aqui seguem o uso de vendedores de impressão 3D (Etsy, Reddit,
Printables). Termo novo que se repete entra no glossário.

## Fora de escopo

- Espanhol (fica registrado como próximo idioma).
- Traduzir `docs/`, decisões e roadmap.
- Traduzir texto que a pessoa digitou (nomes, modelos de mensagem editados).
- Mudar formato de dinheiro por idioma.
- Redesenho do site.

## Decisões

| Nº | Decisão | Situação |
|---|---|---|
| 152 | Idioma em Configurações > Aparência (Automático, Português, English), valendo na hora; automático usa inglês pra qualquer sistema fora do português | aprovada em 2026-11-18 |
| 153 | Datas em inglês pela região do sistema (en-US mês/dia; outras regiões dia/mês), 24 h, ponto decimal; dinheiro segue a moeda | aprovada em 2026-11-18 |
| 154 | Tradução do app pelo glossário aprovado, com teste de paridade de chaves, marcadores e plurais | aprovada em 2026-11-18 |
| 155 | Em inglês, exemplos de Etsy, eBay e Amazon; seção Pix só com moeda padrão real | aprovada em 2026-11-18 |
| 156 | README.en.md e bloco "In English" no CHANGELOG a partir da 2.2, levado pra nota da release | aprovada em 2026-11-18 |
| 157 | Site em inglês em `/en/`, adaptado, sem redirecionar sozinho, com seletor PT/EN, hreflang e sitemap | aprovada em 2026-11-18 |

## Modelo e cálculo (core e dados)

- Preferência nova de idioma (`AUTO`, `PT_BR`, `EN`, gravada em inglês e fixa como
  as outras enums), padrão `AUTO`, no mesmo lugar onde mora o tema. Formato de dados
  continua 2.
- `AppLocale` vira o estado do idioma em uso (idioma resolvido + região do sistema),
  com os padrões de data do item 6. Testes pra cada combinação do item 2 e do item 6.
- Os textos dos documentos (`ClientDocumentTexts`) carregam no idioma em uso.
- Nenhum cálculo muda.

## Telas

- Configurações > Aparência: seletor de idioma (mesmo componente do tema).
- Todas as telas: só textos. Conferir nas capturas em inglês se algum texto mais
  longo quebra layout (botões, cabeçalhos de tabela, colunas do kanban, menu).

## Local ou conectado

Tudo local. O site em inglês é estático no GitHub Pages, como hoje: sem custo nem
dado novo.

## Critérios de aceite

- `scripts/check.sh build` passa, com o teste de paridade.
- Trocar o idioma muda a tela na hora; PDF, mensagem, imagem e recibo saem no idioma.
- Sistema em português e idioma Automático: nada muda em relação à 2.2.
- Capturas em inglês sem texto cortado nem vazando.
- Site: as 5 páginas em `en/`, seletor nos dois idiomas, `hreflang` e `sitemap`.
- **Teste manual do responsável**: usar o app em inglês por um fluxo completo
  (orçamento, salvar, PDF, mensagem) e ler o site em inglês.
