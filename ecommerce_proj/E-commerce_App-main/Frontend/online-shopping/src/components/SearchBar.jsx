import React, { useState, useEffect } from "react";

export default function SearchBar({ onSubmit, onChange }) {
  const [q, setQ] = useState("");
  const [suggestions, setSuggestions] = useState([]);

  useEffect(() => {
    const id = setTimeout(async () => {
      if (!q) { setSuggestions([]); return; }
      try {
        const res = await fetch(`/api/search/suggest?q=${encodeURIComponent(q)}`);
        const data = await res.json();
        setSuggestions(data);
      } catch (e) { /* ignore for demo */ }
      onChange && onChange(q);
    }, 300);
    return () => clearTimeout(id);
  }, [q]);

  return (
    <div className="search">
      <input value={q} onChange={e=>setQ(e.target.value)} placeholder="Search products…" />
      <button onClick={()=>onSubmit && onSubmit(q)}>Search</button>
      {suggestions.length>0 && (
        <ul className="suggestions">
          {suggestions.map((s,i)=><li key={i} onClick={()=>{setQ(s); onSubmit && onSubmit(s);}}>{s}</li>)}
        </ul>
      )}
    </div>
  );
}
