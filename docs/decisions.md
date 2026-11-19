# Registro de decisões

Decisões aprovadas pelo responsável pelo projeto. Este arquivo é o **índice**: uma
linha por decisão. O texto completo de cada uma fica em
[`decisions/NNN.md`](decisions/) (o número com três dígitos, por exemplo
[decisions/121.md](decisions/121.md)).

Decisão nova: crie o arquivo `decisions/NNN.md` com o próximo número, no mesmo
formato dos outros (título, data, status e o texto), e acrescente a linha no fim
desta tabela. Decisão que substitui outra muda o status da antiga nos dois lugares.

| # | Data | Decisão | Status |
|---|---|---|---|
| [1](decisions/001.md) | 2026-09-15 | Plataforma inicial desktop JVM, Android só depois | Aprovada |
| [2](decisions/002.md) | 2026-09-15 | Módulos core (KMP puro) e composeApp (UI Compose Multiplatform) | Aprovada |
| [3](decisions/003.md) | 2026-09-15 | Licença proprietária com Edição Gratuita limitada (substituída pela 15) | Substituída pela 15 |
| [4](decisions/004.md) | 2026-09-15 | Primeira entrega: base, motor de cálculo testado, tela provisória e docs | Aprovada |
| [5](decisions/005.md) | 2026-09-15 | Energia calculada como horas x kW x preço do kWh | Aprovada |
| [6](decisions/006.md) | 2026-09-15 | Manutenção como custo fixo em R$/hora no lugar de depreciação %/h | Aprovada |
| [7](decisions/007.md) | 2026-09-15 | Taxas de marketplace e spray fora da edição gratuita (substituída pela 15) | Substituída pela 15 |
| [8](decisions/008.md) | 2026-09-15 | Falhas e acabamento como percentual do custo de material (revisada na 76) | Substituída pela 76 |
| [9](decisions/009.md) | 2026-09-15 | UI em 2 telas, Orçamento e Configurações, com abas (substituída pela 13) | Substituída pela 13 |
| [10](decisions/010.md) | 2026-09-15 | Padrão de apresentação MVVM, um ViewModel por tela | Aprovada |
| [11](decisions/011.md) | 2026-09-15 | Persistência em memória por ora (substituída pela 14) | Substituída pela 14 |
| [12](decisions/012.md) | 2026-09-15 | Impressora vira entidade própria PrinterProfile, escolhida por orçamento | Aprovada |
| [13](decisions/013.md) | 2026-09-15 | Navegação em 4 abas com cadastro de Filamentos e Impressoras | Aprovada |
| [14](decisions/014.md) | 2026-09-15 | Persistência em arquivo JSON local em ~/.3dreport via interface de repositório | Aprovada |
| [15](decisions/015.md) | 2026-09-15 | Projeto gratuito e open source, licença Apache 2.0, doação voluntária (revisa a 3) | Aprovada |
| [16](decisions/016.md) | 2026-09-15 | Convenções da base ratificadas: pacote, identificadores, Double para dinheiro | Aprovada |
| [17](decisions/017.md) | 2026-09-15 | Prioridade do roadmap: produto/UI-UX, instaladores, infra, Android por último | Aprovada |
| [18](decisions/018.md) | 2026-09-15 | Salvar orçamento com formulário na própria tela e histórico primeiro | Aprovada |
| [19](decisions/019.md) | 2026-09-15 | Exportação PDF/copiar só com nome, venda e foto, só na aba Histórico | Aprovada |
| [20](decisions/020.md) | 2026-09-15 | Marca d'água do PDF só texto, diagonal fixa, configurada em Configurações | Aprovada |
| [21](decisions/021.md) | 2026-09-15 | Marca d'água desenhada por cima da foto, com rodapé profissional | Aprovada |
| [22](decisions/022.md) | 2026-09-15 | Marca d'água e rodapé como opções independentes, PDF salva em Documentos | Aprovada |
| [23](decisions/023.md) | 2026-09-15 | Workaround para layout travado ao trocar de monitor com DPI diferente | Aprovada |
| [24](decisions/024.md) | 2026-09-15 | Exportar vários orçamentos num PDF por seleção múltipla no Histórico | Aprovada |
| [25](decisions/025.md) | 2026-09-15 | Serviços opcionais somam no total do cliente sem alterar o lucro exibido | Aprovada |
| [26](decisions/026.md) | 2026-09-15 | Taxa de marketplace ajusta o valor de venda para manter a margem real | Aprovada |
| [27](decisions/027.md) | 2026-09-15 | Versionamento SemVer com bump de MINOR por leva (revisada na 53) | Substituída pela 53 |
| [28](decisions/028.md) | 2026-09-15 | Rodapé fixo com versão, autor, GitHub, doação e botão Ajuda | Aprovada |
| [29](decisions/029.md) | 2026-09-15 | Link do modelo vira hyperlink clicável no navegador | Aprovada |
| [30](decisions/030.md) | 2026-09-15 | Confirmação antes de excluir em Filamentos, Impressoras, Serviços e Histórico | Aprovada |
| [31](decisions/031.md) | 2026-09-15 | Toda leva exige bump de versão e entrada no CHANGELOG | Aprovada |
| [32](decisions/032.md) | 2026-09-15 | Peso da peça aparece no Histórico, só uso interno | Aprovada |
| [33](decisions/033.md) | 2026-09-15 | Instaladores via GitHub Actions por tag vX.Y.Z, release em rascunho, sem assinatura | Aprovada |
| [34](decisions/034.md) | 2026-09-15 | Versão do app vira 1.0.0 porque o jpackage do macOS exige major maior ou igual a 1 | Aprovada |
| [35](decisions/035.md) | 2026-09-16 | Tema claro/escuro persistido (ThemeMode), troca imediata, paleta azul petróleo e âmbar | Aprovada |
| [36](decisions/036.md) | 2026-09-16 | Modernização de UI: paleta própria e componente EmptyState nas listas | Aprovada |
| [37](decisions/037.md) | 2026-09-16 | Cliente embutido opcional no orçamento e status do pedido (OrderStatus) | Aprovada |
| [38](decisions/038.md) | 2026-09-16 | Período por presets de atalho (Tudo, 7, 30 dias, Este mês) e aba Dashboard | Aprovada |
| [39](decisions/039.md) | 2026-09-16 | Estoque de filamento manual e binário, esgotado some da seleção do Orçamento | Aprovada |
| [40](decisions/040.md) | 2026-09-16 | Filamento com várias cores (colors), cada uma com estoque (refina a 39) | Aprovada |
| [41](decisions/041.md) | 2026-09-17 | Atualizar o instalador do Windows preserva ~/.3dreport (major upgrade MSI) | Aprovada |
| [42](decisions/042.md) | 2026-09-17 | Ícone gerado por código e atalho de instalação sempre criado | Aprovada |
| [43](decisions/043.md) | 2026-09-17 | Atalhos de teclado: Ctrl+1..7 abas, Ctrl+S salvar, Ctrl+N limpar, Esc cancela | Aprovada |
| [44](decisions/044.md) | 2026-09-17 | Catálogo/portfólio em PDF reaproveitando a seleção múltipla do Histórico | Aprovada |
| [45](decisions/045.md) | 2026-09-17 | Templates como presets que só copiam para o BrandingSettings ativo (revisada na 47) | Aprovada |
| [46](decisions/046.md) | 2026-09-17 | Quatro moedas (BRL, USD, EUR, GBP) com separadores próprios, via LocalCurrency | Aprovada |
| [47](decisions/047.md) | 2026-09-17 | Templates sem aba própria: salvar em Configurações e lista em diálogo (revisa a 45) | Aprovada |
| [48](decisions/048.md) | 2026-09-17 | Texto extra das notas de release fica no workflow, CHANGELOG só técnico | Aprovada |
| [49](decisions/049.md) | 2026-09-17 | Preparativos de repositório público: README e site em português, CI, SEO | Aprovada |
| [50](decisions/050.md) | 2026-09-17 | Repositório GitHub tornado público, cumprindo a decisão 15 | Aprovada |
| [51](decisions/051.md) | 2026-09-17 | SEO do site também para venda de impressão 3D, com seção de gestão de vendas | Aprovada |
| [52](decisions/052.md) | 2026-09-17 | Screenshots reais no site e README, og:image com orcamento.png | Aprovada |
| [53](decisions/053.md) | 2026-09-18 | PATCH para correção ou doc, MINOR para funcionalidade nova (revisa a 27) | Aprovada |
| [54](decisions/054.md) | 2026-09-19 | Presets embutidos de impressoras (potência nominal) e tipo de material do filamento | Aprovada |
| [55](decisions/055.md) | 2026-09-20 | Preencher peso e tempo a partir dos metadados do G-code do fatiador | Aprovada |
| [56](decisions/056.md) | 2026-09-20 | Miniatura do G-code vira foto só se não houver foto, com Desfazer importação (revisa a 55) | Aprovada |
| [57](decisions/057.md) | 2026-09-20 | Filtro de extensão do diálogo de G-code via wildcard em FileDialog.file no Windows | Aprovada |
| [58](decisions/058.md) | 2026-09-20 | Troca para JFileChooser com filtro de extensão (revertida na 59) | Aprovada |
| [59](decisions/059.md) | 2026-09-20 | Volta ao FileDialog nativo, sem filtro garantido no Windows (reverte a 58) | Aprovada |
| [60](decisions/060.md) | 2026-09-20 | Instrução de instalação do macOS inclui Privacidade e Segurança, Abrir mesmo assim | Aprovada |
| [61](decisions/061.md) | 2026-09-21 | Anexar STL ao histórico antes do visualizador, que é um rasterizador no Canvas | Aprovada |
| [62](decisions/062.md) | 2026-09-21 | Correções do visualizador 3D: sentido do arrasto, scroll, borda e desempenho (revisa a 61) | Aprovada |
| [63](decisions/063.md) | 2026-09-21 | Limite de 500 mil triângulos, captura do 3D como foto e formulário sempre visível | Aprovada |
| [64](decisions/064.md) | 2026-09-21 | Editar orçamento salvo mantendo id e data de criação, com lastEditedEpochMillis | Aprovada |
| [65](decisions/065.md) | 2026-09-21 | Foto WebP no PDF corrigida decodificando com Skia, sem dependência nova | Aprovada |
| [66](decisions/066.md) | 2026-09-21 | Pular Fase 2 do STL e fazer nível de dificuldade por análise geométrica (Fase 3) | Aprovada |
| [67](decisions/067.md) | 2026-09-21 | Documentação do nível de dificuldade do STL em pricing-formulas, site e manual | Aprovada |
| [68](decisions/068.md) | 2026-09-21 | Fila de impressão na aba Impressoras, por pedidos Em impressão | Aprovada |
| [69](decisions/069.md) | 2026-09-21 | Duplicar orçamento como rascunho na aba Orçamento, compartilhando foto e STL | Aprovada |
| [70](decisions/070.md) | 2026-09-21 | Kanban de pedidos como alternador Lista/Kanban dentro do Histórico, arrasto manual | Aprovada |
| [71](decisions/071.md) | 2026-09-21 | Correções do Kanban: positionInWindow, indicador de destino e miniatura (revisa a 70) | Aprovada |
| [72](decisions/072.md) | 2026-09-21 | Kanban com colunas de altura igual, miniatura compacta e EditQuoteDialog (revisa a 71) | Aprovada |
| [73](decisions/073.md) | 2026-09-21 | Refinamento visual: números em fonte monoespaçada e resultado como nota (QuoteReceipt) | Aprovada |
| [74](decisions/074.md) | 2026-09-22 | Configurações de impressão só informativas (camada, preenchimento), lidas do G-code | Aprovada |
| [75](decisions/075.md) | 2026-09-22 | Leva 0: backup e restauração em zip da pasta de dados, menu Ações no Histórico | Aprovada |
| [76](decisions/076.md) | 2026-09-22 | Leva 1: mão de obra, acabamento por hora, falha sobre custo refeito e custo fixo mensal | Item (2) substituído pela 93 |
| [77](decisions/077.md) | 2026-09-22 | Leva 2: quantidade e lote, entradas são de uma peça e o app multiplica | Aprovada |
| [78](decisions/078.md) | 2026-09-22 | Leva 3: canal de venda e forma de pagamento num campo só, catálogo em Configurações | Aprovada |
| [79](decisions/079.md) | 2026-09-22 | Leva 4: preço negociado vira o salePrice de verdade, com aviso de prejuízo e preço mínimo | Aprovada |
| [80](decisions/080.md) | 2026-09-22 | Leva 5: abrir no WhatsApp (wa.me) e imagem quadrada 1080x1080 do orçamento | Aprovada |
| [81](decisions/081.md) | 2026-09-22 | Leva 7: Orçamento em duas colunas responsivo, barra de composição do preço | Aprovada |
| [82](decisions/082.md) | 2026-09-23 | Leva 7: só o Snackbar de confirmação entra, sem unificar catálogos nem fundir abas | Aprovada |
| [83](decisions/083.md) | 2026-09-23 | Ideias externas entram por issue e o roadmap é a única fonte de verdade | Aprovada |
| [84](decisions/084.md) | 2026-09-23 | Leva 4: guardar o preço de tabela (tableSalePrice) para saber que houve negociação | Aprovada |
| [85](decisions/085.md) | 2026-09-23 | Leva 5: prazo de entrega como data fixa e tempo de impressão no orçamento do cliente | Aprovada |
| [86](decisions/086.md) | 2026-09-23 | Leva 5: logo, contato e borda no PDF, identidade separada do template | Aprovada |
| [87](decisions/087.md) | 2026-09-23 | Leva 7: ícones só ao lado de texto, em abas, seções e ações, sem ícone em campos | Aprovada |
| [88](decisions/088.md) | 2026-09-23 | Leva 8: assinatura Gerado com 3DReport nos PDFs, link só pro site (revisada na 91) | Aprovada |
| [89](decisions/089.md) | 2026-09-23 | Leva 8: arrastar o G-code gera orçamento completo, validado com arquivos reais | Aprovada |
| [90](decisions/090.md) | 2026-09-23 | Leva 8: comunicação reposicionada como o sistema de quem vende impressão 3D | Aprovada |
| [91](decisions/091.md) | 2026-09-23 | Leva 8: assinatura 3DReport obrigatória, discreta, também na imagem (revisa a 88) | Aprovada |
| [92](decisions/092.md) | 2026-09-24 | Leva 2: valor do serviço por pedido, preço do cadastro vira sugestão | Aprovada |
| [93](decisions/093.md) | 2026-09-24 | Acabamento e mão de obra independentes, só somam (substitui o item 2 da 76) | Aprovada |
| [94](decisions/094.md) | 2026-09-24 | Tempo de trabalho num campo só, do pedido inteiro (revisa a 77) | Aprovada |
| [95](decisions/095.md) | 2026-09-24 | Leva 6: Dashboard soma só vendidos, lucro por hora e rankings de peças e clientes | Aprovada |
| [96](decisions/096.md) | 2026-09-24 | Leva 6: manutenção por impressora com alertas por componente, diário e horas avulsas | Aprovada |
| [97](decisions/097.md) | 2026-09-24 | Correções da revisão da 96: desfazer ao zerar contador de manutenção | Aprovada |
| [98](decisions/098.md) | 2026-09-24 | Leva 9: calculadora web no site com Kotlin/JS reaproveitando o core | Aprovada |
| [99](decisions/099.md) | 2026-09-24 | Menu do site no celular vira botão Menu abaixo de 860 px | Aprovada |
| [100](decisions/100.md) | 2026-09-24 | Calculadora do site mostra o que só existe no app, com tabela calculadora x app | Aprovada |
| [101](decisions/101.md) | 2026-09-24 | Leva 7B: produto separado de pedido via SavedQuote.kind (revisita a 44) | Aprovada |
| [102](decisions/102.md) | 2026-09-24 | Leva 7B: catálogo vivo com ProductRepricer e aviso por mudança de custo | Aprovada |
| [103](decisions/103.md) | 2026-09-24 | Leva 7B: perfil de uso para quem está começando, Dashboard do catálogo, barra discreta | Aprovada |
| [104](decisions/104.md) | 2026-09-25 | Leva 9: formato de dados 2 sem conversor, a partir da 2.0.0 | Aprovada |
| [105](decisions/105.md) | 2026-09-25 | Leva 9: cálculo por impressão e vários filamentos por impressão (QuotedPrint) | Aprovada |
| [106](decisions/106.md) | 2026-09-25 | Pré-lançamento: dados prontos para sincronizar, número do pedido e status Cancelado | Aprovada |
| [107](decisions/107.md) | 2026-09-25 | Pré-lançamento: canal e imposto incidem sobre o total cobrado, parseDecimal tolerante | Aprovada |
| [108](decisions/108.md) | 2026-09-25 | Pré-lançamento: gravação atômica, tratador de exceção e anexos por SHA-256 | Aprovada |
| [109](decisions/109.md) | 2026-09-25 | Leva 2.1: barra lateral agrupada, Configurações em seções, edição na própria aba | Aprovada |
| [110](decisions/110.md) | 2026-09-25 | Sai o perfil de uso da 103 e o aviso de dados da 1.x (revisa a 103 e a 104) | Aprovada |
| [111](decisions/111.md) | 2026-09-25 | Leva 2.1: navegação em barra lateral agrupada, Histórico vira Pedidos, Catálogo separado | Aprovada |
| [112](decisions/112.md) | 2026-09-25 | Leva 2.1: Configurações em seções com barra fixa de alterações não salvas | Aprovada |
| [113](decisions/113.md) | 2026-09-25 | Leva 2.1: editar na própria aba do Orçamento, sem EditQuoteDialog | Aprovada |
| [114](decisions/114.md) | 2026-09-25 | Leva 9: várias impressões num pedido, cada uma em cartão recolhível | Aprovada |
| [115](decisions/115.md) | 2026-09-25 | Leva 2.1: arquivar cadastro em uso, arquivado some das escolhas do Orçamento | Aprovada |
| [116](decisions/116.md) | 2026-09-25 | Leva 2.1: verificação de atualizações opcional e upgradeUuid fixo no Windows | Aprovada |
| [117](decisions/117.md) | 2026-09-25 | Saída garantida ao fechar: ExitWatchdog força a saída se o processo travar | Aprovada |
| [118](decisions/118.md) | 2026-09-25 | A margem não incide sobre a hora de trabalho (revisa o cálculo do preço base) | Aprovada |
| [119](decisions/119.md) | 2026-09-25 | Módulos do Java listados à mão no instalador, conferidos no CI, hotfix 2.1.1 | Aprovada |
| [120](decisions/120.md) | 2026-09-26 | Site pronto para divulgar a 2.1 com prints gerados por código (renderScreenshots) | Aprovada |
| [121](decisions/121.md) | 2026-09-29 | Leva 10: taxa fixa por item e faixas de preço no canal de venda | Aprovada |
| [122](decisions/122.md) | 2026-09-29 | Leva 10: insumos como cadastro próprio, custo passa pela margem e fora da reserva de falha | Aprovada |
| [123](decisions/123.md) | 2026-09-29 | Leva 10: tempo nos serviços entra no lucro real do serviço (revisa a 25) | Aprovada |
| [124](decisions/124.md) | 2026-09-29 | Leva 10: frete grátis desconta do lucro sem mudar o preço da peça | Aprovada |
| [125](decisions/125.md) | 2026-09-29 | Leva 10: preço mínimo do pedido e acréscimo por urgência | Aprovada |
| [126](decisions/126.md) | 2026-09-29 | Leva 10: ajuda para calcular o kWh por conta de luz, bandeira ou tarifa branca | Aprovada |
| [127](decisions/127.md) | 2026-11-14 | Nova interface na direção Oficina (azul-marinho e âmbar) pra todo o app | Aprovada |
| [128](decisions/128.md) | 2026-11-14 | Ícone novo do app, três camadas com a última em âmbar | Aprovada |
| [129](decisions/129.md) | 2026-11-14 | Pedidos abre na última visualização usada (kanban ou lista) | Aprovada |
| [130](decisions/130.md) | 2026-11-14 | Fundação de arquitetura antes da nova interface | Aprovada |
| [131](decisions/131.md) | 2026-11-15 | "Negociar" e "Enviar pro cliente" no painel de resultado do Orçamento | Aprovada |
| [132](decisions/132.md) | 2026-11-15 | Pedidos em lista vira tabela, e o Catálogo abre em grade com foto | Aprovada |
| [133](decisions/133.md) | 2026-11-15 | Dashboard em blocos, sem gráfico por enquanto | Aprovada |
| [134](decisions/134.md) | 2026-11-16 | Configurações, Sobre e modelos na nova interface | Aprovada |
| [135](decisions/135.md) | 2026-11-16 | Ajustes do Dashboard depois da revisão | Aprovada |
| [136](decisions/136.md) | 2026-11-16 | A 2.2 fecha com a Leva 10B | Aprovada |
| [137](decisions/137.md) | 2026-11-16 | PDF e imagem do orçamento no visual novo, com fundo da imagem pelo brilho da peça | Aprovada |
| [138](decisions/138.md) | 2026-11-16 | Ícone "Camadas" refinado, com contorno claro nos tamanhos de 16 a 32 px (complementa a 128) | Aprovada |
| [139](decisions/139.md) | 2026-11-16 | Preparação pra tradução concluída sem mudar o formato dos dados (nome automático por `autoNamed`) | Aprovada |
| [140](decisions/140.md) | 2026-11-16 | Site na 2.2: capturas, ícone, marca e textos, sem redesenho das páginas | Aprovada |
| [141](decisions/141.md) | 2026-11-17 | Arquivo do modelo aceita STL ou 3MF; miniatura do fatiador vira prévia e foto | Aprovada |
| [142](decisions/142.md) | 2026-11-17 | Prévia de STL pesado por malha simplificada, medidas da malha completa, teto de 300 MB (revisa a 63) | Aprovada |
| [143](decisions/143.md) | 2026-11-17 | Atualizar o app por dentro: Atualizar agora, SHA256SUMS e instalação por sistema | Aprovada |
| [144](decisions/144.md) | 2026-11-17 | Impressoras da lista com valores de referência: consumo médio (oficial, medido ou estimado pela família, com a máxima ao lado), preço médio no Brasil datado de nov/2026, manutenção por faixa (0,20 aberta de entrada, 0,30 fechada/CoreXY, 0,45 multicolor/grande) e uso padrão de 12 h/dia, 25 dias, 12 meses em todo cadastro novo; aviso pra trocar pelos valores reais. Revisa a 54 | Aprovada |
| [145](decisions/145.md) | 2026-11-17 | Pix copia e cola (BR Code estático no core, txid 3DR+número) e QR pela ZXing, no PDF e na mensagem, não na imagem; valor é o sinal pendente ou o que falta; só pedido em real e não cancelado | Aprovada |
| [146](decisions/146.md) | 2026-11-17 | Pagamentos como lista (valor, forma, data, número fixo) com sinal pedido; sinal padrão em % nascendo em 0, só pra pedido novo; situação calculada; "a receber" desconta o pago. Revisita a frase sobre contas a receber | Aprovada |
| [147](decisions/147.md) | 2026-11-17 | Recibo em PDF por pagamento, número fixo `#0042-1` que não renumera, no visual do orçamento | Aprovada |
| [148](decisions/148.md) | 2026-11-17 | Mensagens prontas por andamento, editáveis em Configurações, com trecho opcional entre colchetes; oferecidas no aviso da mudança de andamento e no menu do pedido | Aprovada |
| [149](decisions/149.md) | 2026-11-17 | Orçamento parado depois de 3 dias em Orçado (configurável, 0 desliga), com "Retomar conversa" e "Marcar como cancelado" | Aprovada |
| [150](decisions/150.md) | 2026-11-17 | Código de rastreio no pedido, com site configurável (`{codigo}`), padrão o dos Correios | Aprovada |
| [151](decisions/151.md) | 2026-11-17 | Observações internas no pedido, nunca exportadas | Aprovada |

## Pendentes de aprovação

Nenhuma no momento — ver [roadmap.md](roadmap.md) para as próximas evoluções.
