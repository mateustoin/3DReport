(function () {
  "use strict";

  // Textos por idioma (decisão 157). O idioma vem do lang do <html>; a conta é a mesma nos dois
  // (vem do core, via web/): aqui só muda o texto, o formato do valor e a leitura do número digitado.
  var TEXTS = {
    pt: {
      prompt: "Preencha o peso da peça e o tempo de impressão para ver o preço.",
      unavailable: "Não foi possível carregar a calculadora agora. Recarregue a página ou baixe o app 3DReport.",
      invalidValues: null, // em português a mensagem do core já vem em português
      partialPrice: "Preço de venda parcial",
      perUnit: " por unidade",
      productionCost: "Custo de produção",
      profit: "Lucro",
      breakEven: "Preço mínimo sem prejuízo",
      material: "Material",
      energy: "Energia",
      failures: "Reserva para falhas",
      finishing: "Acabamento",
      labor: "Seu trabalho",
      appOnlyCosts: ["Manutenção da impressora", "Retorno da máquina", "Custo fixo do mês"],
      appOnlyTag: "só no app",
      note: "Esses custos só aumentam o preço. O app calcula todos.",
    },
    en: {
      prompt: "Enter the part weight and the print time to see the price.",
      unavailable: "The calculator could not load right now. Reload the page or download the 3DReport app.",
      invalidValues: "Check the values: the price cannot be worked out with these numbers.",
      partialPrice: "Partial sale price",
      perUnit: " per unit",
      productionCost: "Production cost",
      profit: "Profit",
      breakEven: "Break-even price",
      material: "Material",
      energy: "Energy",
      failures: "Failure reserve",
      finishing: "Finishing",
      labor: "Your work",
      appOnlyCosts: ["Printer maintenance", "Machine payback", "Monthly fixed cost"],
      appOnlyTag: "app only",
      note: "These costs only raise the price. The app counts all of them.",
    },
  };

  var LANG = /^en/i.test(document.documentElement.lang || "") ? "en" : "pt";
  var T = TEXTS[LANG];

  var STORAGE_KEY = LANG === "en" ? "3dreport-calculadora-en" : "3dreport-calculadora";

  var FIELDS = [
    "peso", "horas", "minutos",
    "precoFilamento", "potencia", "precoKwh", "margem",
    "falhas", "acabamento", "valorHora", "tempoTrabalho",
    "quantidade", "taxaCanal", "taxaFixaCanal", "imposto",
  ];

  var ADVANCED_FIELDS = [
    "falhas", "acabamento", "valorHora", "tempoTrabalho",
    "quantidade", "taxaCanal", "taxaFixaCanal", "imposto",
  ];

  var ADVANCED_DEFAULTS = {
    falhas: "10",
    acabamento: "10",
    valorHora: "0",
    tempoTrabalho: "0",
    quantidade: "1",
    taxaCanal: "0",
    taxaFixaCanal: "0",
    imposto: "0",
  };

  // Em português o valor sai em reais. Em inglês a calculadora não assume moeda: só o número,
  // com duas casas, porque cada pessoa digita os preços na moeda dela.
  var moneyFormatter = null;
  try {
    moneyFormatter = LANG === "en"
      ? new Intl.NumberFormat("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
      : new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
  } catch (e) {
    moneyFormatter = null;
  }

  function formatMoney(value) {
    if (moneyFormatter) {
      return moneyFormatter.format(value);
    }
    return LANG === "en" ? value.toFixed(2) : "R$ " + value.toFixed(2).replace(".", ",");
  }

  function parseDecimal(raw) {
    if (raw == null) return NaN;
    var s = String(raw).trim();
    if (s === "") return NaN;
    if (LANG === "en") {
      // Inglês: ponto decimal. Com ponto, a vírgula separa milhar ("1,250.5"). Sem ponto, uma vírgula só é milhar
      // quando vem seguida de exatamente 3 dígitos ("1,250" = 1250); senão é decimal de quem escreve com vírgula
      // ("0,25", "12,5"), espelhando o que o português faz com o ponto.
      if (s.indexOf(".") !== -1 || (s.match(/,/g) || []).length > 1) {
        s = s.replace(/,/g, "");
      } else if (/^-?[1-9]\d{0,2},\d{3}$/.test(s)) {
        s = s.replace(",", "");
      } else {
        s = s.replace(",", ".");
      }
    } else if (s.indexOf(",") !== -1) {
      s = s.replace(/\./g, "").replace(",", ".");
    }
    var n = parseFloat(s);
    return isFinite(n) ? n : NaN;
  }

  function escapeHtml(str) {
    var div = document.createElement("div");
    div.textContent = String(str);
    return div.innerHTML;
  }

  function resolveCalculateQuote() {
    if (typeof window === "undefined" || typeof window.web === "undefined") return null;
    var w = window.web;
    if (typeof w.calculateQuote === "function") {
      return w.calculateQuote;
    }
    if (
      w.com &&
      w.com.threedreport &&
      w.com.threedreport.web &&
      typeof w.com.threedreport.web.calculateQuote === "function"
    ) {
      return w.com.threedreport.web.calculateQuote;
    }
    return null;
  }

  function getEl(id) {
    return document.getElementById(id);
  }

  function readFieldValues() {
    var values = {};
    FIELDS.forEach(function (id) {
      var el = getEl(id);
      values[id] = el ? el.value : "";
    });
    return values;
  }

  function loadSavedValues() {
    try {
      var raw = window.localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      var parsed = JSON.parse(raw);
      if (parsed && typeof parsed === "object") return parsed;
      return null;
    } catch (e) {
      return null;
    }
  }

  function saveValues() {
    try {
      var values = readFieldValues();
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(values));
    } catch (e) {
      // Sem localStorage disponível (modo privado etc): a calculadora continua funcionando.
    }
  }

  function applySavedValues() {
    var saved = loadSavedValues();
    if (!saved) return;
    FIELDS.forEach(function (id) {
      if (Object.prototype.hasOwnProperty.call(saved, id) && typeof saved[id] === "string") {
        var el = getEl(id);
        if (el) el.value = saved[id];
      }
    });
  }

  function openAdvancedIfChanged() {
    var details = getEl("calc-advanced");
    if (!details) return;
    var changed = ADVANCED_FIELDS.some(function (id) {
      var el = getEl(id);
      if (!el) return false;
      return el.value.trim() !== ADVANCED_DEFAULTS[id];
    });
    if (changed) {
      details.open = true;
    }
  }

  function renderPrompt() {
    var out = getEl("calc-output");
    if (!out) return;
    out.innerHTML = '<p class="result-placeholder">' + T.prompt + "</p>";
  }

  function renderUnavailable() {
    var out = getEl("calc-output");
    if (!out) return;
    out.innerHTML = '<p class="result-error">' + T.unavailable + "</p>";
  }

  function renderError(message) {
    var out = getEl("calc-output");
    if (!out) return;
    // A mensagem do core está em português; em inglês mostramos um aviso geral.
    var text = T.invalidValues != null ? T.invalidValues : message;
    out.innerHTML = '<p class="result-error">' + escapeHtml(text) + "</p>";
  }

  function renderResult(result, quantity) {
    var out = getEl("calc-output");
    if (!out) return;

    var breakdown = [];
    if (result.material > 0) breakdown.push([T.material, result.material]);
    if (result.energy > 0) breakdown.push([T.energy, result.energy]);
    if (result.failures > 0) breakdown.push([T.failures, result.failures]);
    if (result.finishing > 0) breakdown.push([T.finishing, result.finishing]);
    if (result.labor > 0) breakdown.push([T.labor, result.labor]);

    var html = "";
    html += '<p class="result-label">' + T.partialPrice + "</p>";
    html += '<p class="result-total">' + formatMoney(result.salePrice) + "</p>";
    if (quantity > 1) {
      html += '<p class="result-unit">' + formatMoney(result.unitSalePrice) + T.perUnit + "</p>";
    }
    html += '<dl class="result-lines">';
    html += "<div><dt>" + T.productionCost + "</dt><dd>" + formatMoney(result.productionCost) + "</dd></div>";
    html += "<div><dt>" + T.profit + "</dt><dd>" + formatMoney(result.profit) + "</dd></div>";
    html += "<div><dt>" + T.breakEven + "</dt><dd>" + formatMoney(result.breakEvenSalePrice) + "</dd></div>";
    html += "</dl>";
    html += '<ul class="result-breakdown">';
    breakdown.forEach(function (line) {
      html += "<li><span>" + line[0] + "</span><span>" + formatMoney(line[1]) + "</span></li>";
    });
    // Custos que o app cobra e esta calculadora não: ficam à vista, pra ninguém achar que o preço está completo.
    T.appOnlyCosts.forEach(function (name) {
      html += '<li class="app-only"><span>' + name + '</span><span class="app-only-tag">' + T.appOnlyTag + "</span></li>";
    });
    html += "</ul>";
    html += '<p class="result-note">' + T.note + "</p>";
    out.innerHTML = html;
  }

  function computeAndRender() {
    var values = readFieldValues();

    var pesoStr = (values.peso || "").trim();
    var horasStr = (values.horas || "").trim();
    var minutosStr = (values.minutos || "").trim();

    if (pesoStr === "" || (horasStr === "" && minutosStr === "")) {
      renderPrompt();
      return;
    }

    var filamentGrams = parseDecimal(pesoStr);
    var hours = horasStr === "" ? 0 : parseDecimal(horasStr);
    var minutes = minutosStr === "" ? 0 : parseDecimal(minutosStr);
    hours = isFinite(hours) ? hours : NaN;
    minutes = isFinite(minutes) ? minutes : NaN;

    var filamentPricePerKg = parseDecimal(values.precoFilamento);
    var printerPowerWatts = parseDecimal(values.potencia);
    var energyPricePerKwh = parseDecimal(values.precoKwh);
    var profitMarginPercent = parseDecimal(values.margem);
    var failureRatePercent = parseDecimal(values.falhas);
    var finishingRatePercent = parseDecimal(values.acabamento);
    var laborRatePerHour = parseDecimal(values.valorHora);
    var laborMinutes = parseDecimal(values.tempoTrabalho);
    var quantityRaw = parseDecimal(values.quantidade);
    var channelFeePercent = parseDecimal(values.taxaCanal);
    var channelFixedFeePerItem = parseDecimal(values.taxaFixaCanal);
    var taxPercent = parseDecimal(values.imposto);

    var numericInputs = [
      filamentGrams, hours, minutes,
      filamentPricePerKg, printerPowerWatts, energyPricePerKwh, profitMarginPercent,
      failureRatePercent, finishingRatePercent, laborRatePerHour, laborMinutes,
      quantityRaw, channelFeePercent, channelFixedFeePerItem, taxPercent,
    ];
    var allFinite = numericInputs.every(function (n) {
      return isFinite(n);
    });
    if (!allFinite) {
      renderPrompt();
      return;
    }

    var printTimeMinutes = hours * 60 + minutes;
    var quantity = Math.max(1, Math.round(quantityRaw) || 1);

    var calculateQuote = resolveCalculateQuote();
    if (!calculateQuote) {
      renderUnavailable();
      return;
    }

    var result;
    try {
      result = calculateQuote(
        filamentGrams,
        printTimeMinutes,
        filamentPricePerKg,
        printerPowerWatts,
        energyPricePerKwh,
        profitMarginPercent,
        failureRatePercent,
        finishingRatePercent,
        laborRatePerHour,
        laborMinutes,
        quantity,
        channelFeePercent,
        channelFixedFeePerItem,
        taxPercent
      );
    } catch (e) {
      renderUnavailable();
      return;
    }

    if (!result) {
      renderUnavailable();
      return;
    }

    if (result.error) {
      renderError(result.error);
      return;
    }

    renderResult(result, quantity);
  }

  function init() {
    applySavedValues();
    openAdvancedIfChanged();

    var form = getEl("calc-form");
    if (form) {
      form.addEventListener("input", function () {
        saveValues();
        computeAndRender();
      });
    }

    if (!resolveCalculateQuote()) {
      renderUnavailable();
    } else {
      computeAndRender();
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
