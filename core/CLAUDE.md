# core

Domínio e cálculo. Kotlin Multiplatform puro (JVM e JS), só com
`kotlinx-serialization-core`. O `web/` e o `composeApp/` dependem dele, nunca o contrário.

- **Toda regra de preço mora aqui**, não em ViewModel. Se uma tela precisa de uma
  conta nova, ela vira função ou caso de uso em `pricing/` com teste em `commonTest`.
- **Fórmula muda com três coisas juntas:** o código, o teste e a seção de
  `docs/pricing-formulas.md`. O exemplo de referência do documento (R$ 17,02) tem que
  continuar passando.
- **Os modelos são o formato de arquivo.** As classes `@Serializable` de `model/` são
  gravadas direto em JSON pelo `composeApp`. Renomear ou remover um campo quebra dados
  de quem já usa: campo novo entra com valor padrão neutro, e mudança incompatível
  exige migração em `composeApp/.../data/DataFormat.kt` e decisão aprovada.
- **Sem texto de interface.** O `core` não escreve frase pra quem usa o app. Erro de
  validação é identificado por tipo ou código, e a tela escolhe o texto.
- Percentual é fração (0,15, não 15). Dinheiro é `Double`, arredondado só ao exibir.
- Testes: `scripts/check.sh :core:allTests` (roda em JVM e JS).
