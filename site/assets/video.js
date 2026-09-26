// Vídeo de apresentação (decisão 120): o player do YouTube só entra depois do clique, pelo domínio
// sem cookies. Até lá a página não faz nenhuma requisição ao YouTube.
(function () {
  document.querySelectorAll('.video[data-video-id]').forEach(function (box) {
    var button = box.querySelector('.video-play');
    if (!button) return;
    button.addEventListener('click', function () {
      var iframe = document.createElement('iframe');
      iframe.src = 'https://www.youtube-nocookie.com/embed/' + encodeURIComponent(box.dataset.videoId) + '?autoplay=1&rel=0';
      iframe.title = 'Vídeo de apresentação do 3DReport';
      iframe.allow = 'autoplay; encrypted-media; picture-in-picture; fullscreen';
      iframe.allowFullscreen = true;
      box.replaceChildren(iframe);
    });
  });
})();
