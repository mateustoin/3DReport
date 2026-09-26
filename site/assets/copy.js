// Botões "Copiar" da página de imprensa: copiam o texto do bloco indicado em data-copy.
(function () {
  document.querySelectorAll('.copy-btn[data-copy]').forEach(function (button) {
    var source = document.getElementById(button.dataset.copy);
    if (!source || !navigator.clipboard) return;
    button.addEventListener('click', function () {
      var text = Array.prototype.map.call(source.querySelectorAll('p'), function (p) { return p.innerText; }).join('\n\n') || source.innerText;
      navigator.clipboard.writeText(text).then(function () {
        button.textContent = 'Copiado';
        setTimeout(function () { button.textContent = 'Copiar'; }, 2000);
      });
    });
  });
})();
