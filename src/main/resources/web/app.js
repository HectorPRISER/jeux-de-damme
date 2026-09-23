let source = null;

function start() {
  if (source) source.close();
  const white = document.getElementById("white").value;
  const depth = document.getElementById("depth").value;
  const term = document.getElementById("term");
  term.textContent = "";

  source = new EventSource(`/api/stream?white=${white}&depth=${depth}`);
  source.onmessage = (e) => {
    term.textContent += e.data + "\n";
    term.scrollTop = term.scrollHeight;
  };
  source.onerror = () => {
    term.textContent += "\n[connexion terminée]\n";
    source.close();
  };
}

document.getElementById("start").addEventListener("click", start);
start();
