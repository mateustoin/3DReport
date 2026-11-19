# Leva 11: receber e conversar com o cliente

> Especificação de trabalho da leva. É o que cada agente recebe pra fazer a sua
> parte. Depois do merge, o que vale como registro são as decisões em
> `docs/decisions/`; este arquivo fica como histórico do que foi pedido.
> Decisões aprovadas pelo responsável do projeto em 2026-11-17.

## Objetivo

Depois de mandar o orçamento, o vendedor cobra (Pix copia e cola e QR), registra o
que entrou (sinal, parcelas, pago), entrega um recibo e avisa o cliente a cada passo
com uma mensagem pronta, sem sair do app e sem internet.

## Itens

### A. Pix copia e cola e QR Code (decisão 145)

1. **Configurações > Documentos pro cliente** ganha a seção "Pix": **Chave Pix**,
   **Nome de quem recebe** e **Cidade**. Sem chave, nada de Pix aparece em lugar
   nenhum (campo novo nasce vazio).
   - A chave é normalizada antes de entrar no código: telefone vira `+55DDDNUMERO`
     (mesma leitura de telefone do `WhatsAppLink`, só que com o `+`), CPF e CNPJ
     ficam só com os dígitos, e-mail em minúsculas, chave aleatória (EVP, 36
     caracteres com hífens) como veio. Chave que não parece nenhum desses tipos
     mostra um aviso curto embaixo do campo, mas é aceita (o banco é quem valida).
   - Nome e cidade vão pro código sem acento, em maiúsculas, cortados em 25 e 15
     caracteres (limites do BR Code). O nome nasce sugerido pelo nome da marca, se
     houver. Os dois são obrigatórios pra gerar o código: sem eles, a seção mostra
     o que falta e o Pix não aparece nos documentos.
2. **O código (core, `pix/`)**: BR Code estático, padrão EMV do Banco Central, sem
   internet. Campos: `00` = `01`; `26` com `00` = `br.gov.bcb.pix` e `01` = chave;
   `52` = `0000`; `53` = `986`; `54` = valor com ponto e duas casas (só se maior que
   zero); `58` = `BR`; `59` nome; `60` cidade; `62` com `05` = identificador (txid) =
   `3DR` + número do pedido com 4 dígitos (`3DR0042`), ou `***` sem número; `63` =
   CRC16-CCITT (polinômio 0x1021, início 0xFFFF) em 4 hex maiúsculos. Testes contra
   exemplos do manual do Pix e um teste por tipo de chave.
3. **Qual valor**: enquanto há sinal pedido e ainda não pago, o Pix é o que falta do
   sinal; depois disso, o que falta pagar do pedido (item B). Pedido já pago: sem
   Pix. Só pra pedido em real (`Currency.BRL`); outra moeda nunca mostra Pix.
   Produto do catálogo nunca mostra Pix.
4. **QR**: gerado com a **ZXing core** (Apache 2.0) só no `composeApp` (JVM), nível de
   correção M, margem de 4 módulos. Entra em `THIRD-PARTY-NOTICES.md`.
5. **PDF do orçamento**: um bloco "Pague com Pix" depois do preço: o QR (cerca de 3,5
   cm), o valor ("Sinal: R$ 50,00" ou "Valor: R$ 174,34"), a chave e o código copia
   e cola em fonte menor, quebrado em linhas. O bloco não pode ser cortado entre
   páginas. No catálogo em PDF, nunca.
6. **Mensagem do WhatsApp e copiar/colar**: termina com uma linha "Pix copia e cola
   (R$ X):" e o código sozinho na linha seguinte, pra o cliente copiar só ele.
7. **Imagem quadrada**: sem Pix (aprovado: é vista no celular do cliente, que não lê o
   QR da própria tela).
8. **Ação "Copiar código Pix"** no menu do pedido (grupo Enviar), quando há Pix pra
   este pedido.

### B. Situação do pagamento, com sinal e pagamento parcial (decisão 146)

9. Cada pedido guarda uma **lista de pagamentos** (valor, forma, data) e o **sinal
   pedido** (valor em reais). Formas: Pix, Cartão, Dinheiro, Outro.
10. **Sinal padrão**: Configurações ganha "Sinal padrão (%)", nascendo em **0** (sem
    sinal). Ao salvar um pedido novo, o sinal vira valor = total do pedido × %,
    arredondado a centavos, e fica congelado como o resto do pedido. Vale também
    pra produto do catálogo transformado em pedido. **Reabrir o cálculo e salvar
    nunca cria nem muda o sinal** (revisto em 2026-11-17: pedido antigo nunca ganha
    selo "Não pago" sozinho, e quem zerou o sinal de propósito não perde o zero);
    se o total novo ficar menor que o sinal, o sinal é limitado ao total. O sinal
    é editável no pedido (item 13). Sinal nunca maior que o total.
11. **Situação, calculada** (nada disso é gravado): pago = soma dos pagamentos; falta
    = max(0, total do pedido − pago).
    - **Não pago**: nenhum pagamento.
    - **Sinal pago**: há sinal, pago ≥ sinal e falta > 0.
    - **Pago em parte**: pago > 0, falta > 0, e não é o caso acima.
    - **Pago**: falta = 0 com pelo menos um pagamento.
    Pagar a mais é aceito (gorjeta, arredondamento): a falta fica em zero.
12. **Com sinal, o PDF e a mensagem** dizem "Sinal de R$ X pra começar" logo depois
    do total (texto pro cliente; o restante é implícito).
13. **Tela "Pagamentos"** (diálogo, item "Pagamentos" no menu do pedido, grupo
    Organizar, só em pedido): mostra total, pago, falta e a situação; o sinal
    (editável); a lista de pagamentos (data, forma, valor, com excluir e "Recibo");
    e "Registrar pagamento", que abre o formulário com o valor já preenchido (o que
    falta do sinal, ou o que falta do pedido), a forma (última usada) e a data (hoje,
    editável). Ao registrar, o aviso embaixo oferece "Gerar recibo".
14. **Onde a situação aparece**: selo no cartão do kanban e coluna "Pagamento" na
    tabela de Pedidos, só quando há pelo menos um pagamento ou sinal (pedido antigo,
    sem nada disso, fica como hoje). Selos: "Sinal pago", "Pago em parte", "Pago",
    e "Não pago" só quando há sinal pedido.
15. **"A receber"** no resumo de Pedidos continua contando de Aprovado a Pronto, mas
    soma o que **falta** de cada pedido, e não o total (pedido sem pagamento soma o
    total, como hoje).
16. O Dashboard não muda: venda continua contada pelo andamento (decisão 95).
17. Revisita a frase "não é objetivo do app virar um sistema de contas a receber":
    continua não sendo. Sem vencimento, cobrança automática nem conciliação.

### C. Recibo em PDF (decisão 147)

18. Gerado a partir de um pagamento (lista do item 13, ou o aviso depois de
    registrar), pelo mesmo exportador do orçamento e no mesmo visual (cabeçalho com
    logo e contatos, assinatura "Gerado com 3DReport").
19. Conteúdo: "Recibo" com o número `#0042-1` (número do pedido e um número do
    pagamento **gravado ao registrar**, sempre o maior já usado no pedido mais um:
    excluir um pagamento não renumera os outros, e um recibo entregue nunca muda de
    número; pagamento sem número gravado, que não existe antes desta leva, usa a
    posição na lista); "Recebi de {cliente} o valor de R$ X ({forma}) em {data},
    referente ao pedido #0042, {nome da peça}." (sem cliente: "Recebi o valor..."),
    e um quadro com total do pedido, pago até agora e falta pagar. Nome da marca
    embaixo. Sem custo, lucro nem nada interno (decisão 19). O nome do cliente entra
    porque o recibo é pra ele.
20. Nome do arquivo sugerido: `Recibo #0042-1 - Nome da peça.pdf`.

### D. Mensagens prontas por andamento (decisão 148)

21. **Modelos**, um por momento, com texto padrão nos recursos traduzíveis:
    - **Aprovado**: "Oi, {cliente}! Seu pedido {pedido} ({peca}) foi aprovado e fica
      pronto até {prazo}." (sem prazo, a frase termina em "foi aprovado.")
    - **Imprimindo**: "{peca} já está imprimindo."
    - **Pronto**: "{peca} ficou pronto!"
    - **Saiu pra entrega**: "{peca} saiu pra entrega. Rastreio: {rastreio}" (sem
      código, sem a parte do rastreio).
    - **Lembrete de pagamento**: "Lembrete: falta o pagamento de {falta} do pedido
      {pedido}." seguido do Pix, se houver (item 6).
    - **Retomar orçamento**: "Oi, {cliente}! Conseguiu ver o orçamento de {peca}? Se
      quiser, ajusto alguma coisa."
    O texto final de cada modelo é do `construtor-ui`, curto, sem travessão, do
    ponto de vista de quem vende. Marcadores: `{cliente}`, `{pedido}`, `{peca}`,
    `{prazo}`, `{valor}`, `{falta}`, `{rastreio}`, `{loja}`. **Trecho opcional entre
    colchetes** (aprovado em 2026-11-17): `foi aprovado[ e fica pronto até {prazo}].`
    Se algum marcador dentro do trecho fica sem valor, o trecho inteiro some; se
    todos têm valor, só os colchetes somem. Os textos padrão usam isso (prazo no
    Aprovado, rastreio no Saiu pra entrega, cliente na saudação), e a lista de
    marcadores em Configurações explica os colchetes numa frase. Marcador sem valor
    fora de colchetes some junto com o espaço sobrando. O preenchimento é uma função
    pura testada.
    Pedido **Cancelado** não oferece mensagem nem Pix.
22. **Editáveis** em Configurações > Documentos pro cliente, seção "Mensagens
    prontas": um campo de várias linhas por modelo, a lista de marcadores e
    "Voltar ao texto padrão". Só o texto que a pessoa mudou é gravado; o resto vem
    dos recursos (assim o padrão segue o idioma do app).
23. **Janela "Mensagem pro cliente"**: o modelo escolhido já preenchido e editável,
    um seletor pra trocar de modelo, e os botões "Abrir no WhatsApp" (mesmo `wa.me`
    e mesma leitura de telefone da decisão 80) e "Copiar". Editar aqui não muda o
    modelo salvo.
24. **Como se chega nela**:
    - Ao mudar o andamento pra Aprovado, Imprimindo, Pronto ou Entregue (arrastar no
      kanban ou "Mover para"), o aviso que aparece embaixo ganha "Avisar cliente",
      que abre a janela no modelo do andamento novo (Entregue abre "Saiu pra
      entrega"). Mudar pra Orçado ou Cancelado não oferece mensagem. Nada abre
      sozinho.
    - O menu do pedido ganha "Mensagem pro cliente" (grupo Enviar), abrindo no
      modelo do andamento atual (Orçado abre "Retomar orçamento"; com falta a
      pagar e andamento Pronto ou Entregue, abre "Lembrete de pagamento").

### E. Lembrete de orçamento parado (decisão 149)

25. Pedido em **Orçado** há **3 dias** ou mais (configurável em Configurações >
    Pedidos, "Destacar orçamento parado depois de N dias", 0 desliga) ganha o selo
    "Parado há N dias" no cartão e na tabela, no tom de atenção (não de erro).
26. Os dias contam a partir do mais recente entre: a última entrada em Orçado
    (`statusHistory`), a criação, a última edição e o último "Retomar conversa".
27. O menu de um pedido parado ganha no topo "Retomar conversa" (abre a janela do
    item 23 no modelo "Retomar orçamento"; ao abrir no WhatsApp ou copiar, grava o
    momento do contato e o selo some até passar o prazo de novo) e "Marcar como
    cancelado".
28. Convive com o selo de prazo vencido de orçamento que já existe.

### F. Código de rastreio no pedido (decisão 150)

29. "Editar detalhes" de pedido ganha **Código de rastreio** (texto livre, opcional).
30. Configurações > Pedidos ganha **Site de rastreio**: um endereço com `{codigo}`
    no lugar do código. Nasce com o site oficial dos Correios,
    `https://rastreamento.correios.com.br/app/index.php?objetos={codigo}` (aprovado
    em 2026-11-17: abre com o código no campo; a pessoa clica em Consultar e passa
    pela verificação de segurança do site). Nada gravado = padrão do app; apagar o
    campo = sem link.
31. Pedido com código: o menu ganha "Ver rastreio" (abre o endereço, se houver) e
    "Copiar código de rastreio". O código entra na mensagem "Saiu pra entrega".
    Nunca entra no PDF nem na imagem.

### G. Observações internas no pedido (decisão 151)

32. "Editar detalhes" de pedido ganha **Observações** (várias linhas, opcional). Uso
    só interno (decisão 19): nunca entra em PDF, imagem, mensagem nem recibo.
33. Pedido com observação mostra um ícone pequeno no cartão do kanban e na tabela,
    com o texto no tooltip.

### H. Pendência da Leva 10B

34. As capturas do site e do README já são da interface nova (regeradas na 2.2.0 e na
    2.2.1). O item "Capturas de todas as telas" da Leva 10B é marcado como feito e
    a decisão 140 deixa de dizer "ainda aberto". Só documentação.

### I. Ajustes da revisão de design (aprovados em 2026-11-18)

35. **Selo de pagamento só de Aprovado em diante.** Em Orçado nenhum selo de pagamento
    aparece (o sinal só é cobrado depois do sim). "Não pago" no tom de atenção a
    partir de Aprovado; "Pago", "Sinal pago" e "Pago em parte" no neutro. Sem tom
    positivo novo no kit.
36. **Configurações > Pedidos usa a barra "Alterações não salvas"** com Salvar, como
    as outras seções.
37. **A linha do sinal só aparece enquanto falta o sinal** (`pendingDeposit > 0`).
    Depois de pago, some do PDF e da mensagem, e o Pix cobra o restante ("Valor:").
    No PDF, a linha vai pra dentro do quadro do total, sob o valor.
38. **Na janela "Mensagem pro cliente", o código Pix fica fora do texto editável:**
    embaixo do campo, só leitura, com a marcação "Incluir o Pix" (aparece só quando
    há Pix a cobrar; nasce ligada no Lembrete de pagamento e no Aprovado, e
    desligada nos outros modelos). Ao copiar ou abrir
    no WhatsApp, o código vai no fim, intacto, sozinho na última linha.

## Fora de escopo

- Saber se o Pix foi pago (Pix dinâmico, confirmação automática): Leva 18.
- Vencimento, juros, cobrança automática, conciliação, contas a receber.
- Pix na imagem quadrada.
- Pagamentos no Dashboard (recebido no mês, inadimplência).
- Mandar mensagem sem abrir o WhatsApp (API oficial).
- Pagamento em produto do catálogo.
- Inglês (Leva 11B); os textos novos nascem nos recursos, como manda a 10B.

## Decisões

| Nº | Decisão | Situação |
|---|---|---|
| 145 | Pix copia e cola (BR Code estático no core) e QR pela ZXing, no PDF e na mensagem, não na imagem; valor é o sinal pendente ou o que falta | aprovada em 2026-11-17 |
| 146 | Pagamentos como lista (valor, forma, data) com sinal pedido; sinal padrão em % nascendo em 0; situação calculada; "a receber" desconta o pago | aprovada em 2026-11-17 |
| 147 | Recibo em PDF por pagamento, numerado `#0042-1`, no visual do orçamento | aprovada em 2026-11-17 |
| 148 | Mensagens prontas por andamento, editáveis em Configurações; oferecidas no aviso da mudança de andamento e no menu do pedido | aprovada em 2026-11-17 |
| 149 | Orçamento parado destacado depois de 3 dias em Orçado (configurável, 0 desliga), com "Retomar conversa" e "Marcar como cancelado" | aprovada em 2026-11-17 |
| 150 | Código de rastreio no pedido, com site de rastreio configurável (`{codigo}`) | aprovada em 2026-11-17 |
| 151 | Observações internas no pedido, nunca exportadas | aprovada em 2026-11-17 |

## Modelo e cálculo (core e dados)

Tudo campo novo com valor padrão: **o formato de dados continua 2**, sem migração.
Teste de compatibilidade: um `quotes` gravado pela 2.2.1 (sem as chaves novas) abre
com os padrões abaixo e grava de volta sem perder nada. Nomes gravados no JSON em
inglês e fixos (`@SerialName`), como em `OrderStatus`.

**`SavedQuote`** (core):

| Campo | Tipo | Padrão |
|---|---|---|
| `payments` | `List<Payment>` | vazio |
| `depositAmount` | `Double` | `0.0` (≥ 0) |
| `trackingCode` | `String?` | `null` |
| `notes` | `String?` | `null` |
| `lastFollowUpEpochMillis` | `Long?` | `null` |

Derivados (sem gravar): `paidTotal`, `remaining` (sobre `totalWithServices`),
`paymentStatus` (`NOT_PAID`, `DEPOSIT_PAID`, `PARTIALLY_PAID`, `PAID`),
`pendingDeposit`, `pixAmount` (item 3), `hasPaymentTracking` (há pagamento ou sinal),
`staleDays(todayEpochDay, zone)` ou equivalente pro item 26. Testes pra cada regra.

**`Payment`** (core): `id: String`, `amount: Double` (> 0), `method: PaymentMethod`
(`pix`, `card`, `cash`, `other`), `epochDay: Long`, `number: Int` (fixo, do recibo,
item 19; padrão `0` = usar a posição).

**Pix** (core, pacote novo `pix/`): `PixKey` (normalização e tipo), `PixPayload`
(montagem do BR Code e CRC16). Sem dependência de UI nem de JVM.

**Configurações** (onde cada um mora é escolha do `engenheiro-core`, seguindo
`docs/architecture.md`; identidade do vendedor não entra em `QuoteTemplate`):

| Campo | Padrão |
|---|---|
| Chave Pix, nome de quem recebe, cidade | vazios |
| Sinal padrão (%) | `0` |
| Modelos de mensagem editados | vazio (usa o texto dos recursos) |
| Dias pra orçamento parado | `3` |
| Site de rastreio | constante dos Correios conferida (ou vazio) |

**Repositório**: registrar e excluir pagamento, mudar o sinal, gravar o contato do
"Retomar conversa", e `QuoteDetails` com `trackingCode` e `notes`. Reabrir o cálculo
e salvar (edição pelo Orçamento) **mantém** pagamentos, sinal, rastreio, observações
e o último contato. Duplicar não copia nenhum deles (o sinal do duplicado sai do %
padrão). Backup e restauração levam tudo junto (vem de graça por estar no
`SavedQuote`; conferir com teste).

**Nenhum preço muda**: nada disso entra no cálculo do preço, do lucro nem do
Dashboard.

## Telas

- **Configurações > Documentos pro cliente**: seções novas "Pix" (3 campos, aviso de
  chave, o que falta) e "Mensagens prontas" (um campo por modelo, marcadores, voltar
  ao padrão). Usa `FormSection`, `AppTextField`.
- **Configurações** (onde fica o que é de Pedidos hoje, ou seção nova "Pedidos"):
  sinal padrão (%), dias pra orçamento parado, site de rastreio.
- **Pedidos > menu do pedido**: Enviar ganha "Mensagem pro cliente" e "Copiar código
  Pix"; Organizar ganha "Pagamentos"; "Ver rastreio" e "Copiar código de rastreio"
  com código; "Retomar conversa" e "Marcar como cancelado" no topo quando parado.
- **Diálogo Pagamentos** (item 13) e formulário "Registrar pagamento".
- **Diálogo Mensagem pro cliente** (item 23).
- **Editar detalhes** de pedido: Código de rastreio e Observações.
- **Kanban e tabela**: selo de pagamento, selo "Parado há N dias", ícone de
  observação; coluna "Pagamento" na tabela.
- **Aviso de mudança de andamento**: ação "Avisar cliente".
- **PDF do orçamento**: bloco Pix e linha do sinal. **PDF do recibo**: novo.
- **Mensagem/copiar**: linha do sinal e o Pix no fim.

Todos os textos novos em `strings.xml`. Componentes do kit (`docs/design-system.md`);
selos com o `Badge` que já existe. Capturas novas pelo `ScreenshotRenderer` com
`-Pscreenshots.leva11=true` (sem mudar o conjunto do site): diálogo de pagamentos,
mensagem pro cliente, kanban com selos, configurações do Pix, PDF com Pix e recibo.

## Local ou conectado

Tudo local. O Pix é estático e gerado no computador; o QR também. O WhatsApp é só um
link `wa.me` aberto no navegador, como hoje. O site de rastreio é um link aberto no
navegador, por escolha da pessoa. Nada novo sai do computador sozinho: sem servidor,
sem custo, sem dado pessoal novo fora da máquina (LGPD: o nome do cliente no recibo é
um documento que o próprio vendedor entrega).

## Critérios de aceite

- `scripts/check.sh build` passa; testes novos no core pra BR Code (CRC, campos,
  tipos de chave, valor), situação do pagamento, sinal, dias parado e preenchimento
  dos modelos; teste de compatibilidade com pedido da 2.2.1.
- Pedido antigo abre igual: sem selo de pagamento, "a receber" igual ao de antes.
- Sem chave Pix configurada, nenhum documento muda.
- Com sinal padrão 0, nenhum documento muda.
- **Teste manual do responsável**: ler o QR e colar o copia e cola em pelo menos dois
  apps de banco (valor e nome certos, com sinal e sem sinal); registrar sinal e
  saldo e ver a situação e o "a receber" mudarem; gerar o recibo; mudar um pedido
  pra Pronto e mandar "Avisar cliente" pelo WhatsApp; ver um orçamento antigo
  aparecer como parado e retomar a conversa; abrir o rastreio pelo menu.
