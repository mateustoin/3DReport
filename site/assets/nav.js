// Menu do celular (decisão 99): o botão "Menu" abre e fecha os links. Sem este script, o CSS deixa
// os links sempre visíveis, então o site continua navegável.
(function () {
  var header = document.querySelector('.site-header');
  var toggle = header && header.querySelector('.nav-toggle');
  if (!toggle) return;

  function setOpen(open) {
    header.classList.toggle('nav-open', open);
    toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
  }

  header.classList.add('nav-ready');
  toggle.addEventListener('click', function () {
    setOpen(!header.classList.contains('nav-open'));
  });
  header.querySelectorAll('.nav-links a').forEach(function (link) {
    link.addEventListener('click', function () { setOpen(false); });
  });
  document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape') setOpen(false);
  });
})();
