import React, { useEffect, useState } from "react";
import SearchBar from "../components/SearchBar";

export default function SearchPage() {
  const [filters,setFilters] = useState({category:"", brand:"", minPrice:"", maxPrice:""});
  const [q,setQ] = useState("");
  const [page,setPage] = useState(0);
  const [resp,setResp] = useState(null);

  async function load() {
    const params = new URLSearchParams();
    if (q) params.set("q", q);
    Object.entries(filters).forEach(([k,v]) => v && params.set(k, v));
    params.set("page", page);
    params.set("size", 12);
    const res = await fetch(`/api/search?${params.toString()}`);
    const data = await res.json();
    setResp(data);
  }

  useEffect(()=>{ load(); /* eslint-disable-next-line */}, [q, filters, page]);

  return (
    <div style={{display:"grid", gridTemplateColumns:"240px 1fr", gap: "16px"}}>
      <div>
        <SearchBar onSubmit={setQ} onChange={setQ} />
        <h4>Filters</h4>
        <input placeholder="Brand" value={filters.brand} onChange={e=>setFilters(f=>({...f,brand:e.target.value}))} />
        <input placeholder="Min Price" value={filters.minPrice} onChange={e=>setFilters(f=>({...f,minPrice:e.target.value}))} />
        <input placeholder="Max Price" value={filters.maxPrice} onChange={e=>setFilters(f=>({...f,maxPrice:e.target.value}))} />
        {resp?.aggregations?.by_category && (
          <>
            <h5>Categories</h5>
            <ul>
              {resp.aggregations.by_category.buckets.map(b => (
                <li key={b.key} onClick={()=>setFilters(f=>({...f,category:b.key}))}>
                  {b.key} ({b.doc_count})
                </li>
              ))}
            </ul>
          </>
        )}
      </div>
      <div>
        <div style={{display:"grid", gridTemplateColumns:"repeat(4, 1fr)", gap:"12px"}}>
          {resp?.content?.map(p => (
            <div key={p.id} style={{border:"1px solid #eee", borderRadius:8, padding:12}}>
              <h5 style={{margin:"0 0 8px"}}>{p.title}</h5>
              <div>₹{p.price}</div>
              <div style={{fontSize:12, opacity:0.8}}>{p.brand} · {p.category}</div>
            </div>
          ))}
        </div>
        <div style={{display:"flex", alignItems:"center", gap:12, marginTop:16}}>
          <button disabled={page===0} onClick={()=>setPage(p=>p-1)}>Prev</button>
          <span>Page {page+1}</span>
          <button onClick={()=>setPage(p=>p+1)}>Next</button>
        </div>
      </div>
    </div>
  );
}
