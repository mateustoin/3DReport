# Avisos de terceiros

O 3DReport inclui material de terceiros, listado abaixo com a respectiva licença.

## Material Symbols

- **O que é:** ícones da interface (abas, títulos de seção e ações), copiados
  como vetor para `composeApp/src/commonMain/kotlin/com/threedreport/app/ui/icons/AppIcons.kt`
  (decisão 87 em [docs/decisions.md](docs/decisions.md)).
- **Origem:** [google/material-design-icons](https://github.com/google/material-design-icons),
  pasta `symbols/web`, estilo Outlined, peso 400, grade 24.
- **Direitos autorais:** Copyright Google LLC.
- **Licença:** [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0),
  a mesma do 3DReport (ver [LICENSE](LICENSE)).

Os ícones de carretel de filamento e de impressora 3D, no mesmo arquivo, foram
desenhados para o próprio 3DReport e seguem a licença do projeto.

## Inter

- **O que é:** fonte do texto da interface, embutida no app (pesos 400, 500 e 600,
  arquivos estáticos em `composeApp/src/commonMain/composeResources/font/`).
- **Origem:** [rsms/inter](https://github.com/rsms/inter), versão 4.1
  (`extras/ttf` do pacote de lançamento).
- **Direitos autorais:** Copyright (c) 2016 The Inter Project Authors.
- **Licença:** [SIL Open Font License 1.1](https://openfontlicense.org/)
  (texto em [licenses/OFL-Inter.txt](licenses/OFL-Inter.txt)).

## Space Grotesk

- **O que é:** fonte da marca, dos títulos e dos números em destaque, embutida no app
  (arquivo variável de peso, usado em 500, 600 e 700, em
  `composeApp/src/commonMain/composeResources/font/`). O PDF e a imagem do orçamento
  usam uma versão derivada, `space_grotesk_bold_tnum.ttf`: o mesmo desenho fixado no
  peso 700 e com os algarismos de largura igual no lugar dos comuns, porque o PDF não
  escolhe peso nem variante de algarismo num arquivo variável. Fora isso, nada foi
  alterado. A licença não reserva o nome da fonte.
- **Origem:** [floriankarsten/space-grotesk](https://github.com/floriankarsten/space-grotesk),
  versão 2.0.0.
- **Direitos autorais:** Copyright 2020 The Space Grotesk Project Authors.
- **Licença:** [SIL Open Font License 1.1](https://openfontlicense.org/)
  (texto em [licenses/OFL-SpaceGrotesk.txt](licenses/OFL-SpaceGrotesk.txt)).

## ZXing

- **O que é:** biblioteca que gera o QR Code do Pix copia e cola no PDF do orçamento
  (decisão 145). Só o módulo `core`, usado em
  `composeApp/src/jvmMain/kotlin/com/threedreport/app/platform/PixQrCode.kt`; o QR é
  montado no computador, sem internet.
- **Origem:** [zxing/zxing](https://github.com/zxing/zxing), artefato
  `com.google.zxing:core`, versão 3.5.4.
- **Direitos autorais:** Copyright ZXing authors.
- **Licença:** [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0),
  a mesma do 3DReport (ver [LICENSE](LICENSE)).
