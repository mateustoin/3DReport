# Leva 10C: arquivo do modelo (3MF e STL pesado) e atualização por dentro

> Especificação de trabalho da leva. É o que cada agente recebe pra fazer a sua
> parte. Depois do merge, o que vale como registro são as decisões em
> `docs/decisions/`; este arquivo fica como histórico do que foi pedido.
> Abre a versão seguinte à 2.2 (decisão 136). Tudo vai em `[Não publicado]`.

## Objetivo

Quem vende a mesma peça de novo guarda o projeto do fatiador (`.3mf`) junto com o
pedido, vê a prévia 3D de qualquer STL, por mais pesado que seja, e atualiza o app
com um clique, sem ir até o site.

## Itens

### A. Arquivo do modelo: STL ou 3MF (decisão 141)

1. O campo "Anexar arquivo STL" do Orçamento vira **"Arquivo do modelo"** e aceita
   `.stl` ou `.3mf`. O seletor de arquivo e o arrastar e soltar aceitam os dois.
   Um arquivo só por orçamento: escolher outro troca o anterior.
2. O arquivo é guardado como veio, na mesma pasta de hoje (`models/`), e baixado de
   volta em Pedidos e no Catálogo ("Baixar arquivo do modelo", com o nome do
   orçamento, como hoje, e a extensão certa, `.stl` ou `.3mf`; o nome original do
   arquivo não é guardado, pra não criar campo novo no disco). Uso só interno, como
   hoje: nunca entra no PDF, na imagem nem na mensagem.
3. **Com STL:** tudo como hoje (prévia 3D, análise, captura como foto).
4. **Com 3MF:** sem prévia 3D. O app lê a miniatura que o fatiador grava dentro do
   arquivo (Bambu Studio, OrcaSlicer e PrusaSlicer; o caminho da imagem é lido do
   próprio pacote, com os caminhos conhecidos como reserva) e:
   - mostra a miniatura no lugar da prévia 3D;
   - se o orçamento não tem foto, ou a foto atual veio de uma miniatura automática
     (G-code ou 3MF), a miniatura do 3MF vira a foto, com a mesma regra da miniatura
     do G-code de hoje (`photoFromGCode` passa a cobrir as duas origens);
   - um botão "Usar como foto do orçamento" deixa trocar a foto à mão.
   3MF sem miniatura ou que não abre como pacote: o arquivo é guardado igual e
   aparece um aviso curto ("Esse 3MF não tem miniatura" / "Não consegui ler esse
   3MF"). Nunca bloqueia salvar.
5. Dado antigo continua abrindo: o campo gravado continua sendo `stlFileName` no
   JSON (formato 2, nada muda no disco), com teste de compatibilidade de um
   orçamento da 2.2 com STL.

### B. Prévia de STL pesado (decisão 142)

6. Acima de 500 mil triângulos, em vez de recusar, o app monta uma **malha
   simplificada só pra desenhar**: agrupa vértices próximos numa grade (vertex
   clustering), escolhendo o tamanho da grade pra ficar perto de um alvo (na casa de
   200 mil triângulos; o número exato é constante testada). Triângulos que viram
   degenerados somem.
7. A leitura percorre os bytes sem montar um objeto por triângulo do arquivo
   inteiro (hoje `StlTriangle` tem vários `Vec3` por triângulo, o que estoura a
   memória num arquivo de milhões). Roda fora da thread da tela e informa o
   progresso (0 a 1), que aparece como barra no lugar de "Preparando a prévia 3D…".
   Trocar ou remover o arquivo no meio cancela o trabalho.
8. **As medidas e o nível de dificuldade continuam saindo da malha completa.** Pra
   STL de até 500 mil triângulos o resultado da análise é idêntico ao de hoje
   (teste). Acima disso, a análise percorre os triângulos do arquivo sem guardar os
   objetos.
9. O arquivo guardado continua o original. A captura do 3D como foto passa a valer
   pra qualquer STL. A prévia de STL simplificado mostra uma nota pequena
   ("Prévia simplificada pra desenhar mais rápido. As medidas usam o arquivo
   completo.").
10. Continua existindo um teto pra não travar o computador: um arquivo maior que um
    limite de tamanho (constante, na casa de centenas de MB, medida pelo engenheiro
    com um STL grande sintético) fica sem prévia, com o aviso de hoje ajustado.

### C. Atualizar o app por dentro (decisão 143)

11. **Onde aparece:** quando a verificação (automática, se ligada, ou "Verificar
    agora" no Sobre) acha versão nova, o aviso e o Sobre ganham **"Atualizar agora"**
    ao lado de "Ver novidades". Sem a opção de verificação ligada, nada sai do
    computador sozinho (decisão 116 continua valendo).
12. **Qual arquivo:** pela API de releases, o asset do sistema em uso: `.msi` no
    Windows, `.deb` no Linux, `.dmg` com sufixo `-apple-silicon` ou `-intel` no
    macOS conforme a arquitetura. Sem asset pro sistema (ou rodando fora de um
    instalador, como pelo Gradle), o botão não aparece e fica só "Ver novidades".
13. **Baixar:** janela com o nome da versão, barra de progresso e "Cancelar". O
    arquivo vai pra uma pasta temporária do sistema, não pra `~/.3dreport`. Falha de
    rede mostra um erro curto com "Tentar de novo" e "Ver novidades".
14. **Conferir:** o app baixa também o `SHA256SUMS.txt` da mesma release e só segue se
    a soma SHA-256 do instalador bater. Sem o arquivo de somas (releases antigas) ou
    soma diferente: não instala, apaga o arquivo e oferece "Ver novidades".
15. **Instalar, por sistema:**
    - **Windows:** abre `msiexec /i <arquivo>` desacoplado do app e fecha o app. O
      Windows pede a confirmação de administrador. O `upgradeUuid` fixo (decisão 116)
      faz o instalador trocar a versão.
    - **Linux:** pede a senha pela janela do sistema (`pkexec`) e instala o `.deb`
      (`apt-get install` do arquivo local, ou `dpkg -i` se não houver `apt-get`).
      Deu certo: reabre o app na versão nova e fecha a atual. Senha cancelada ou
      falha: aviso curto, nada muda. Sem `pkexec`: abre a pasta do arquivo baixado e
      explica o comando de instalar.
    - **macOS:** abre o `.dmg` e fecha o app, com aviso antes ("Arraste o 3DReport pra
      Aplicativos e substitua"). Não testado em Mac nesta leva: fica dito no PR e no
      changelog que depende de teste.
16. **Antes de fechar o app**, tudo que está pendente de gravação é gravado (o
    mesmo caminho do fechamento normal da janela). Se há orçamento com mudança não
    salva, pergunta antes, como o fechamento normal já faz.
17. **CI da release:** o workflow passa a gerar e publicar `SHA256SUMS.txt` com a soma
    de cada instalador (formato do `sha256sum`). As notas da release citam o arquivo.
18. Só funciona a partir da versão seguinte à que trouxer o mecanismo: quem está na
    2.2 atualiza como hoje. Dito no changelog.

### D. Ajustes técnicos guardados no fechamento da 2.2 (sem mudança pra quem usa)

19. O "3D" do `BrandMark` passa a usar o token `onNavy`, como o banner do site.
20. A regra que sugere entrega urgente com prazo de 0 a 2 dias sai de
    `QuoteSaleSection` e vai pro `QuoteViewModel` (ou pro `core`, se for regra de
    negócio pura), com teste.
21. `MaintenanceDialog` recebe o dia injetado em vez de chamar `todayEpochDay()`.
22. `FieldHelp` sai do kit e do `docs/design-system.md`.

## Fora de escopo

- Ver o 3MF em 3D (ler a malha de dentro do 3MF). Fica registrado no roadmap.
- Guardar STL e 3MF ao mesmo tempo no mesmo orçamento.
- Ler o `.gcode.3mf` do Bambu Studio (Leva 17), embora vá reaproveitar a leitura de
  pacote feita aqui.
- Atualização silenciosa em segundo plano (exige assinatura de código, decisão 15).
- Assinar os instaladores.
- Fórmulas de preço.

## Decisões

| Nº | Decisão | Situação |
|---|---|---|
| 141 | Arquivo do modelo: um campo que aceita STL ou 3MF; com 3MF, a miniatura do fatiador vira prévia e foto automática; 3MF em 3D fica pra depois | aprovada em 2026-11-17 |
| 142 | Prévia de STL pesado com malha simplificada só pra desenhar; medidas da malha completa | aprovada em 2026-11-17 (roadmap) |
| 143 | Atualizar por dentro: botão "Atualizar agora", soma SHA-256 publicada pela release, instalação por sistema (no Linux, senha pelo `pkexec` e reabre o app) | aprovada em 2026-11-17 |

## Modelo e cálculo (core e dados)

- `SavedQuote.stlFileName` continua no disco com esse nome. Na interface e no código
  novo pode ganhar um nome melhor (`modelFileName` com `@SerialName("stlFileName")`),
  desde que o JSON gravado não mude. O tipo do arquivo sai da extensão guardada.
- **Leitura do 3MF:** o 3MF é um ZIP. A leitura das entradas fica no `jvmMain` do
  `composeApp` (`java.util.zip`), atrás de uma função de plataforma; o `core` não
  ganha leitor de ZIP nesta leva (o site não precisa de 3MF). O que for regra pura
  (achar o caminho da miniatura a partir dos `.rels` e da lista de entradas) fica
  testável sem arquivo real.
- **STL:** novas funções no `core/stl` pra percorrer os triângulos de um STL
  (binário e ASCII) sem montar `StlMesh`, pra simplificar por grade com progresso e
  pra analisar em fluxo. `StlAnalyzer` dá o mesmo resultado de hoje pra malhas
  pequenas (teste comparando os dois caminhos). Malha simplificada é um `StlMesh`
  comum, então o `Stl3DViewer` não muda.
- **Atualização:** `ReleaseSource` passa a devolver também os assets (nome, URL de
  download, tamanho) da release. Escolha do asset por sistema e arquitetura e
  conferência da soma (parse do `SHA256SUMS.txt`) são funções puras com teste. Baixar
  e instalar ficam em `jvmMain`, atrás de uma interface injetada (testes usam um
  falso).
- Nenhum campo novo no disco. Nenhum preço muda. Formato de dados continua 2.

## Telas

- **Orçamento, bloco do arquivo do modelo:** rótulo "Arquivo do modelo (opcional)",
  ajuda "STL ou 3MF. Guardado pra você baixar de novo em Pedidos ou no Catálogo".
  Com STL: como hoje, com barra de progresso no carregamento e a nota de prévia
  simplificada quando for o caso. Com 3MF: miniatura (mesmo tamanho da prévia 3D)
  com "Usar como foto do orçamento", ou o aviso de sem miniatura.
- **Pedidos e Catálogo:** "Baixar STL" vira "Baixar arquivo do modelo".
- **Aviso de versão nova e Sobre:** botão "Atualizar agora" (primário) ao lado de
  "Ver novidades". Janela de atualização (diálogo do kit) com os estados: baixando
  (barra e "Cancelar"), conferindo, pronto pra instalar (texto do que vai acontecer
  no sistema em uso e "Instalar e fechar o app"), erro (mensagem e "Ver novidades").
- Todos os textos em `strings.xml`. Componentes do kit; nada de cor ou medida solta.

## Local ou conectado

- Itens A, B e D: só local.
- Item C: **conectado**, só quando a pessoa clica em "Atualizar agora" (ou já ligou a
  verificação). Baixa do GitHub o instalador e o arquivo de somas; nada do usuário
  vai no pedido além do `User-Agent` de hoje. Sem servidor próprio, sem custo, sem
  dado pessoal (LGPD não muda).

## Critérios de aceite

- `scripts/check.sh build` OK; `ls -la ~/.3dreport` igual antes e depois.
- Orçamento da 2.2 com STL abre, baixa e mostra a prévia como antes (teste).
- 3MF do Bambu Studio, OrcaSlicer e PrusaSlicer: a miniatura é lida (teste com
  pacotes mínimos montados no teste, sem arquivo de terceiros no repositório).
- STL sintético de alguns milhões de triângulos: prévia aparece sem estourar a
  memória padrão do app, com progresso, e as medidas batem com a malha completa.
- Escolha do asset, parse do `SHA256SUMS.txt` e recusa com soma errada cobertos por
  teste.
- Workflow da release gera `SHA256SUMS.txt` (conferido lendo o YAML; a release real só
  com a tag, que não é criada nesta leva).
- **Teste manual do responsável:** anexar um 3MF real, ver a miniatura virar foto,
  baixar de volta; abrir um STL pesado real; ver o botão "Atualizar agora" aparecer
  (o fluxo completo só dá pra testar quando sair a versão seguinte).
