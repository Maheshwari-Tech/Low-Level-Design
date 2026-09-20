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
const query =
  "/api/availability?day=2026-09-22&participants=ava%40demo.com%2Csam%40demo.com";

function App() {
  const [slots, setSlots] = useState([]);
  const [meetings, setMeetings] = useState([]);
  const [notice, setNotice] = useState("Choose an available time.");
  const load = () => {
    api(query).then((data) => setSlots(data.slots));
    api("/api/meetings").then(setMeetings);
  };
  useEffect(load, []);
  async function schedule(slot) {
    try {
      await api("/api/meetings", {
        method: "POST",
        body: JSON.stringify({
          title: "Machine coding discussion",
          participants: ["ava@demo.com", "sam@demo.com"],
          start: slot.start,
          end: slot.end,
        }),
      });
      setNotice("Meeting scheduled.");
      load();
    } catch (error) {
      setNotice(error.message);
    }
  }
  return (
    <main>
      <p className="eyebrow">Machine coding project</p>
      <h1>Calendar meet</h1>
      <section>
        <h2>22 September · Ava and Sam</h2>
        <div className="slots">
          {slots.map((slot) => (
            <button
              disabled={!slot.available}
              onClick={() => schedule(slot)}
              key={slot.start}
            >
              {new Date(slot.start).toLocaleTimeString([], {
                hour: "2-digit",
                minute: "2-digit",
              })}
            </button>
          ))}
        </div>
        <p className="notice">{notice}</p>
        <h2>Meetings</h2>
        <ul>
          {meetings.map((meeting) => (
            <li key={meeting.id}>
              <strong>{meeting.title}</strong>
              <span>
                {new Date(meeting.start).toLocaleTimeString([], {
                  hour: "2-digit",
                  minute: "2-digit",
                })}{" "}
                · {meeting.participants.join(", ")}
              </span>
            </li>
          ))}
        </ul>
      </section>
    </main>
  );
}
createRoot(document.getElementById("root")).render(<App />);
