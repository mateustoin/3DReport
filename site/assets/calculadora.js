(function () {
  "use strict";

  var STORAGE_KEY = "3dreport-calculadora";

  var FIELDS = [
    "peso", "horas", "minutos",
    "precoFilamento", "potencia", "precoKwh", "margem",
    "falhas", "acabamento", "valorHora", "tempoTrabalho",
    "quantidade", "taxaCanal", "imposto",
  ];

  var ADVANCED_FIELDS = [
    "falhas", "acabamento", "valorHora", "tempoTrabalho",
    "quantidade", "taxaCanal", "imposto",
  ];

  var ADVANCED_DEFAULTS = {
    falhas: "10",
    acabamento: "10",
    valorHora: "0",
    tempoTrabalho: "0",
    quantidade: "1",
    taxaCanal: "0",
    imposto: "0",
  };

  var currencyFormatter = null;
  try {
    currencyFormatter = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
  } catch (e) {
    currencyFormatter = null;
  }

  function formatMoney(value) {
    if (currencyFormatter) {
      return currencyFormatter.format(value);
    }
    return "R$ " + value.toFixed(2).replace(".", ",");
  }

  function parseDecimal(raw) {
    if (raw == null) return NaN;
    var s = String(raw).trim();
    if (s === "") return NaN;
    if (s.indexOf(",") !== -1) {
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
    out.innerHTML = '<p class="result-placeholder">Preencha o peso da peça e o tempo de impressão para ver o preço.</p>';
  }

  function renderUnavailable() {
    var out = getEl("calc-output");
    if (!out) return;
    out.innerHTML = '<p class="result-error">Não foi possível carregar a calculadora agora. Recarregue a página ou baixe o app 3DReport.</p>';
  }

  function renderError(message) {
    var out = getEl("calc-output");
    if (!out) return;
    out.innerHTML = '<p class="result-error">' + escapeHtml(message) + "</p>";
  }

  function renderResult(result, quantity) {
    var out = getEl("calc-output");
    if (!out) return;

    var breakdown = [];
    if (result.material > 0) breakdown.push(["Material", result.material]);
    if (result.energy > 0) breakdown.push(["Energia", result.energy]);
    if (result.failures > 0) breakdown.push(["Reserva para falhas", result.failures]);
    if (result.finishing > 0) breakdown.push(["Acabamento", result.finishing]);
    if (result.labor > 0) breakdown.push(["Seu trabalho", result.labor]);

    var html = "";
    html += '<p class="result-label">Valor de venda</p>';
    html += '<p class="result-total">' + formatMoney(result.salePrice) + "</p>";
    if (quantity > 1) {
      html += '<p class="result-unit">' + formatMoney(result.unitSalePrice) + " por unidade</p>";
    }
    html += '<dl class="result-lines">';
    html += "<div><dt>Custo de produção</dt><dd>" + formatMoney(result.productionCost) + "</dd></div>";
    html += "<div><dt>Lucro</dt><dd>" + formatMoney(result.profit) + "</dd></div>";
    html += "</dl>";
    html += '<p class="result-breakeven">Abaixo de ' + formatMoney(result.breakEvenSalePrice) + " você tem prejuízo.</p>";
    if (breakdown.length > 0) {
      html += '<ul class="result-breakdown">';
      breakdown.forEach(function (line) {
        html += "<li><span>" + line[0] + "</span><span>" + formatMoney(line[1]) + "</span></li>";
      });
      html += "</ul>";
    }
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
    var taxPercent = parseDecimal(values.imposto);

    var numericInputs = [
      filamentGrams, hours, minutes,
      filamentPricePerKg, printerPowerWatts, energyPricePerKwh, profitMarginPercent,
      failureRatePercent, finishingRatePercent, laborRatePerHour, laborMinutes,
      quantityRaw, channelFeePercent, taxPercent,
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
