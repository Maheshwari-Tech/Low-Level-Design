"use client";

import { useEffect, useMemo, useState } from "react";
import { CodeViewer } from "./CodeViewer";
import { FileTree } from "./FileTree";
import { resolveLearningMarkdownLink } from "./learningLinks";
import { MarkdownView } from "./MarkdownView";
import { lldPublicAsset } from "../lib/public-asset.mjs";

type FeatureIndex = {
  slug: string;
  title: string;
  eyebrow: string;
  summary: string;
  focus: readonly string[];
  invariants: readonly string[];
  accent: string;
  level: string;
  durationMinutes: number;
  variationCount: number;
  javaFileCount: number;
  pythonFileCount: number;
  fileCount: number;
  entryFile: string;
  pythonEntryFile: string;
};

type FullProblem = {
  slug: string;
  readme: string;
  files: { path: string; code: string }[];
};

type ContentPayload = { catalog: FullProblem[] };

export function SolutionExplorer({
  features,
  problemSlugs,
}: {
  features: readonly FeatureIndex[];
  problemSlugs: readonly string[];
}) {
  const [selectedSlug, setSelectedSlug] = useState(features[0]?.slug ?? "");
  const [payload, setPayload] = useState<ContentPayload | null>(null);
  const [selectedPath, setSelectedPath] = useState(features[0]?.entryFile ?? "");
  const [view, setView] = useState<"source" | "readme">("readme");
  const [sourceLanguage, setSourceLanguage] = useState<"java" | "python">("java");
  const [fileQuery, setFileQuery] = useState("");

  useEffect(() => {
    let active = true;
    fetch(lldPublicAsset("/lld-content.json"))
      .then((response) => {
        if (!response.ok) throw new Error("Unable to load the source bundle.");
        return response.json() as Promise<ContentPayload>;
      })
      .then((data) => { if (active) setPayload(data); })
      .catch(() => { if (active) setPayload({ catalog: [] }); });
    return () => { active = false; };
  }, []);

  const selectedIndex = features.find((feature) => feature.slug === selectedSlug) ?? features[0];
  const selected = payload?.catalog.find((problem) => problem.slug === selectedSlug);
  const files = useMemo(() => selected?.files ?? [], [selected]);
  const languageFiles = useMemo(
    () => files.filter((file) => file.path.endsWith(sourceLanguage === "python" ? ".py" : ".java")),
    [files, sourceLanguage],
  );
  const visibleFiles = useMemo(
    () => languageFiles.filter((file) => file.path.toLowerCase().includes(fileQuery.trim().toLowerCase())),
    [fileQuery, languageFiles],
  );
  const preferredEntry = sourceLanguage === "python"
    ? selectedIndex?.pythonEntryFile
    : selectedIndex?.entryFile;
  const activeFile = languageFiles.find((file) => file.path === selectedPath) ??
    languageFiles.find((file) => file.path === preferredEntry) ?? languageFiles[0];

  function selectSolution(feature: FeatureIndex) {
    setSelectedSlug(feature.slug);
    setSelectedPath(feature.entryFile);
    setFileQuery("");
    setSourceLanguage("java");
    setView("readme");
  }

  function selectSourceLanguage(language: "java" | "python") {
    setSourceLanguage(language);
    setFileQuery("");
    setSelectedPath(language === "python" ? selectedIndex.pythonEntryFile : selectedIndex.entryFile);
  }

  if (!selectedIndex) return null;

  return (
    <div className="solution-explorer">
      <nav className="solution-rail" aria-label="Runnable LLD solutions">
        <p className="rail-label">60-minute interview solutions</p>
        {features.map((feature, index) => (
          <button
            className={`solution-nav-item ${feature.slug === selectedSlug ? "is-active" : ""}`}
            data-accent={feature.accent}
            key={feature.slug}
            onClick={() => selectSolution(feature)}
            type="button"
          >
            <span className="nav-number">{String(index + 1).padStart(2, "0")}</span>
            <span>{feature.title}</span>
            <span className="nav-arrow" aria-hidden="true">↗</span>
          </button>
        ))}
      </nav>

      <div className="solution-stage" data-accent={selectedIndex.accent}>
        <div className="solution-summary">
          <div>
            <p className="eyebrow">{selectedIndex.eyebrow}</p>
            <div className="interview-meta" aria-label="Interview format">
              <span>{selectedIndex.level}</span>
              <span>{selectedIndex.durationMinutes} minutes</span>
              <span>{selectedIndex.variationCount} interview variations</span>
              <span>Java + Python</span>
            </div>
            <h3>{selectedIndex.title}</h3>
            <p className="solution-description">{selectedIndex.summary}</p>
          </div>
          <div className="solution-count">
            <div><strong>{selectedIndex.durationMinutes}</strong><span>minutes</span></div>
            <div><strong>{selectedIndex.variationCount}</strong><span>variations</span></div>
            <div><strong>{selectedIndex.javaFileCount}</strong><span>Java files</span></div>
            <div><strong>{selectedIndex.pythonFileCount}</strong><span>Python files</span></div>
          </div>
        </div>

        <div className="focus-row" aria-label="Design focus">
          {selectedIndex.focus.map((focus) => <span key={focus}>{focus}</span>)}
        </div>

        <ol className="interview-timeline" aria-label="Suggested 60-minute interview plan">
          <li><span>00–05</span><strong>Clarify</strong><small>Scope and assumptions</small></li>
          <li><span>05–15</span><strong>Protect</strong><small>Requirements and invariants</small></li>
          <li><span>15–30</span><strong>Model</strong><small>Objects, APIs, state</small></li>
          <li><span>30–45</span><strong>Stress</strong><small>Races and failures</small></li>
          <li><span>45–55</span><strong>Defend</strong><small>SOLID and trade-offs</small></li>
          <li><span>55–60</span><strong>Prove</strong><small>Tests and summary</small></li>
        </ol>

        <div className="invariant-grid">
          {selectedIndex.invariants.map((invariant, index) => (
            <div className="invariant-card" key={invariant}>
              <span>Invariant {index + 1}</span>
              <p>{invariant}</p>
            </div>
          ))}
        </div>

        <div className="source-panel">
          <div className="source-panel-head">
            <div>
              <p className="eyebrow">Senior / Staff answer</p>
              <h4>Study the question, expected solution, then code.</h4>
            </div>
            <div className="view-switch" role="group" aria-label="Solution view">
              <button className={view === "readme" ? "is-active" : ""} onClick={() => setView("readme")} type="button">Interview guide</button>
              <button className={view === "source" ? "is-active" : ""} onClick={() => setView("source")} type="button">Source code</button>
            </div>
          </div>

          {view === "readme" ? (
            selected ? (
              <MarkdownView
                hideTitle
                markdown={selected.readme}
                resolveLink={(href) => resolveLearningMarkdownLink(href, problemSlugs)}
              />
            ) : <div className="content-loading">Loading the design brief…</div>
          ) : (
            <div className="source-workbench">
              <aside className="file-browser">
                <div className="source-language-switch" role="group" aria-label="Source language">
                  <button className={sourceLanguage === "java" ? "is-active" : ""} onClick={() => selectSourceLanguage("java")} type="button">Java <span>{selectedIndex.javaFileCount}</span></button>
                  <button className={sourceLanguage === "python" ? "is-active" : ""} onClick={() => selectSourceLanguage("python")} type="button">Python <span>{selectedIndex.pythonFileCount}</span></button>
                </div>
                <label htmlFor="file-filter">Files</label>
                <input
                  id="file-filter"
                  onChange={(event) => setFileQuery(event.target.value)}
                  placeholder="Filter files…"
                  type="search"
                  value={fileQuery}
                />
                <div className="file-list">
                  <FileTree
                    activePath={activeFile?.path}
                    files={visibleFiles}
                    onSelect={setSelectedPath}
                  />
                </div>
              </aside>
              <div className="mobile-file-select">
                <div className="source-language-switch" role="group" aria-label="Source language">
                  <button className={sourceLanguage === "java" ? "is-active" : ""} onClick={() => selectSourceLanguage("java")} type="button">Java <span>{selectedIndex.javaFileCount}</span></button>
                  <button className={sourceLanguage === "python" ? "is-active" : ""} onClick={() => selectSourceLanguage("python")} type="button">Python <span>{selectedIndex.pythonFileCount}</span></button>
                </div>
                <label htmlFor="mobile-file">Source file</label>
                <select id="mobile-file" onChange={(event) => setSelectedPath(event.target.value)} value={activeFile?.path ?? ""}>
                  {languageFiles.map((file) => <option key={file.path} value={file.path}>{file.path}</option>)}
                </select>
              </div>
              <div className="viewer-slot">
                {activeFile ? (
                  <CodeViewer code={activeFile.code} path={activeFile.path} />
                ) : (
                  <div className="content-loading">
                    {payload ? "Source bundle unavailable." : "Loading runnable source…"}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
