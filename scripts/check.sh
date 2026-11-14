#!/usr/bin/env bash
# Roda uma tarefa do Gradle e mostra só o resultado: "OK" ou as falhas.
# O log completo vai pra um arquivo temporário, pra não poluir o terminal
# (nem o contexto de um agente). Uso: scripts/check.sh build
#                                     scripts/check.sh :core:allTests
#                                     scripts/check.sh :composeApp:jvmTest --tests "pacote.Classe"
set -u
cd "$(dirname "$0")/.."

if [ "$#" -eq 0 ]; then
  echo "uso: scripts/check.sh <tarefa do Gradle> [argumentos]" >&2
  exit 2
fi

log="$(mktemp -t 3dreport-check.XXXXXX)"
marker="$(mktemp -t 3dreport-check-inicio.XXXXXX)"
trap 'rm -f "$marker"' EXIT
if ./gradlew --console=plain -q "$@" >"$log" 2>&1; then
  echo "OK: ./gradlew $*"
  rm -f "$log"
  exit 0
fi

echo "FALHOU: ./gradlew $*"
echo
# Erros de compilação, testes que falharam e a causa apontada pelo Gradle.
grep -E "^e: |^w: .*deprecat| FAILED$|^\* What went wrong:|^> " -A4 "$log" | cut -c1-400 | head -n 80
echo
# Mensagem de cada teste que falhou, direto dos relatórios.
# Só os relatórios desta execução: os mais novos que o log (relatório antigo engana).
find core/build/test-results composeApp/build/test-results web/build/test-results \
  -name "*.xml" -newer "$marker" -print0 2>/dev/null \
  | xargs -0 -r grep -hoE "<testcase name=\"[^\"]*\" classname=\"[^\"]*\"|<failure message=\"[^\"]{0,300}" \
  | grep -B1 "<failure" | grep -v "^--$" | head -n 40
echo
echo "Log completo: $log"
exit 1
