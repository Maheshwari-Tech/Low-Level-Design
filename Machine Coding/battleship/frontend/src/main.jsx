import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data.detail);
  return data;
}

function App() {
  const [game, setGame] = useState(null);
  const [notice, setNotice] = useState("Place the fleet, then fire.");
  const load = () => api("/api/game").then(setGame);
  useEffect(load, []);

  async function place(name, index) {
    try {
      await api("/api/game/ships", {
        method: "POST",
        body: JSON.stringify({
          name,
          row: index,
          column: 0,
          direction: "horizontal",
        }),
      });
      setNotice(`${name} placed.`);
      load();
    } catch (error) {
      setNotice(error.message);
    }
  }

  async function fire(row, column) {
    try {
      const outcome = await api("/api/game/shots", {
        method: "POST",
        body: JSON.stringify({ row, column }),
      });
      setNotice(
        outcome.won
          ? "You won!"
          : `${outcome.result.toUpperCase()}${outcome.sunk ? ` — ${outcome.ship} sunk` : ""}`,
      );
      load();
    } catch (error) {
      setNotice(error.message);
    }
  }

  if (!game) return <main>Loading…</main>;
  const shots = new Map(
    game.shots.map((shot) => [`${shot.row}-${shot.column}`, shot.result]),
  );
  return (
    <main>
      <p className="eyebrow">Machine coding project</p>
      <h1>Battleship</h1>
      <section>
        <div className="actions">
          {Object.keys(game.fleet).map((ship, index) => (
            <button
              key={ship}
              disabled={game.ships.some((item) => item.name === ship)}
              onClick={() => place(ship, index)}
            >
              {ship} · {game.fleet[ship]}
            </button>
          ))}
          <button
            className="reset"
            onClick={async () => {
              await api("/api/game/reset", { method: "POST" });
              load();
              setNotice("New game.");
            }}
          >
            Reset
          </button>
        </div>
        <div className="board" style={{ "--board-size": game.boardSize }}>
          {Array.from({ length: game.boardSize ** 2 }, (_, index) => {
            const row = Math.floor(index / game.boardSize),
              column = index % game.boardSize,
              result = shots.get(`${row}-${column}`);
            return (
              <button
                className={`cell ${result || ""}`}
                disabled={!game.ready || Boolean(result)}
                onClick={() => fire(row, column)}
                key={index}
              >
                {result === "hit" ? "×" : result === "miss" ? "•" : ""}
              </button>
            );
          })}
        </div>
        <p className="notice">{notice}</p>
      </section>
    </main>
  );
}

createRoot(document.getElementById("root")).render(<App />);
