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
  const [shows, setShows] = useState([]);
  const [showId, setShowId] = useState("");
  const [seats, setSeats] = useState([]);
  const [selected, setSelected] = useState([]);
  const [notice, setNotice] = useState("Choose seats to book.");
  useEffect(() => {
    api("/api/shows").then((data) => {
      setShows(data);
      setShowId(data[0]?.id || "");
    });
  }, []);
  const loadSeats = () =>
    showId &&
    api(`/api/shows/${showId}/seats`).then((data) => setSeats(data.seats));
  useEffect(loadSeats, [showId]);
  const toggle = (id) =>
    setSelected((current) =>
      current.includes(id)
        ? current.filter((seat) => seat !== id)
        : [...current, id],
    );
  async function book() {
    try {
      const hold = await api(`/api/shows/${showId}/holds`, {
        method: "POST",
        body: JSON.stringify({ seats: selected }),
      });
      const booking = await api(`/api/shows/${showId}/bookings`, {
        method: "POST",
        body: JSON.stringify({
          hold_id: hold.holdId,
          customer_name: "Demo Customer",
        }),
      });
      setNotice(
        `${booking.bookingId}: ${booking.seats.join(", ")} booked for ₹${booking.total}`,
      );
      setSelected([]);
      loadSeats();
    } catch (error) {
      setNotice(error.message);
    }
  }
  return (
    <main>
      <p className="eyebrow">Machine coding project</p>
      <h1>Movie tickets</h1>
      <section>
        <select
          value={showId}
          onChange={(event) => setShowId(event.target.value)}
        >
          {shows.map((show) => (
            <option value={show.id} key={show.id}>
              {show.movie} — ₹{show.price}
            </option>
          ))}
        </select>
        <div className="screen">SCREEN</div>
        <div className="seats">
          {seats.map((seat) => (
            <button
              disabled={seat.status !== "available"}
              className={`${seat.status} ${selected.includes(seat.id) ? "selected" : ""}`}
              onClick={() => toggle(seat.id)}
              key={seat.id}
            >
              {seat.id}
            </button>
          ))}
        </div>
        <button className="book" disabled={!selected.length} onClick={book}>
          Hold and confirm {selected.length} seat(s)
        </button>
        <p className="notice">{notice}</p>
      </section>
    </main>
  );
}
createRoot(document.getElementById("root")).render(<App />);
