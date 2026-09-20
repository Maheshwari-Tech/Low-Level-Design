"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { CodeViewer } from "./CodeViewer";
import { FileTree } from "./FileTree";
import { resolveLearningMarkdownLink } from "./learningLinks";
import { MarkdownView } from "./MarkdownView";
import { lldPublicAsset } from "../lib/public-asset.mjs";

type Problem = {
  title: string;
  slug: string;
  summary: string;
  category: string;
  javaFileCount: number;
  pythonFileCount: number;
  entryFile: string;
  pythonEntryFile: string;
};

type ProblemDetail = Problem & {
  readme: string;
  files: { path: string; code: string }[];
};

type ContentPayload = { catalog: ProblemDetail[] };

function problemSlugFromHash() {
  if (typeof window === "undefined") return null;
  const match = /^#question\/([a-z0-9_]+)$/.exec(window.location.hash);
  return match?.[1] ?? null;
}

export function CatalogExplorer({ problems }: { problems: readonly Problem[] }) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("All");
  const [payload, setPayload] = useState<ContentPayload | null>(null);
  const [selectedSlug, setSelectedSlug] = useState<string | null>(null);
  const [view, setView] = useState<"explanation" | "source">("explanation");
  const [sourceLanguage, setSourceLanguage] = useState<"all" | "java" | "python">("all");
  const [selectedPath, setSelectedPath] = useState("");
  const [fileQuery, setFileQuery] = useState("");

  useEffect(() => {
    let active = true;
    fetch(lldPublicAsset("/lld-content.json"))
      .then((response) => {
        if (!response.ok) throw new Error("Unable to load the question library.");
        return response.json() as Promise<ContentPayload>;
      })
      .then((data) => { if (active) setPayload(data); })
      .catch(() => { if (active) setPayload({ catalog: [] }); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    function syncWithUrl() {
      const slug = problemSlugFromHash();
      const problem = problems.find((candidate) => candidate.slug === slug);
      if (problem) {
        setSelectedSlug(problem.slug);
        setSelectedPath(problem.entryFile);
        setSourceLanguage("all");
        setView("explanation");
      } else {
        setSelectedSlug(null);
      }
    }

    syncWithUrl();
    window.addEventListener("hashchange", syncWithUrl);
    window.addEventListener("popstate", syncWithUrl);
    return () => {
      window.removeEventListener("hashchange", syncWithUrl);
      window.removeEventListener("popstate", syncWithUrl);
    };
  }, [problems]);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (selectedSlug && dialog && !dialog.open) dialog.showModal();
    if (!selectedSlug && dialog?.open) dialog.close();
  }, [selectedSlug]);

  const categories = useMemo(
    () => ["All", ...Array.from(new Set(problems.map((problem) => problem.category)))],
    [problems],
  );
  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return problems.filter((problem) => {
      const categoryMatches = category === "All" || problem.category === category;
      const textMatches = !needle ||
        `${problem.title} ${problem.summary} ${problem.category}`.toLowerCase().includes(needle);
      return categoryMatches && textMatches;
    });
  }, [category, problems, query]);

  const selectedProblem = problems.find((problem) => problem.slug === selectedSlug);
  const selectedDetail = payload?.catalog.find((problem) => problem.slug === selectedSlug);
  const files = useMemo(() => selectedDetail?.files ?? [], [selectedDetail]);
  const languageFiles = useMemo(() => files.filter((file) => {
    if (sourceLanguage === "all") return true;
    return file.path.endsWith(sourceLanguage === "python" ? ".py" : ".java");
  }), [files, sourceLanguage]);
  const visibleFiles = useMemo(() => {
    const needle = fileQuery.trim().toLowerCase();
    return needle ? languageFiles.filter((file) => file.path.toLowerCase().includes(needle)) : languageFiles;
  }, [fileQuery, languageFiles]);
  const preferredEntry = sourceLanguage === "python"
    ? selectedProblem?.pythonEntryFile
    : selectedProblem?.entryFile;
  const activeFile = languageFiles.find((file) => file.path === selectedPath) ??
    languageFiles.find((file) => file.path === preferredEntry) ??
    languageFiles[0];
  const selectedIndex = selectedProblem ? problems.findIndex((problem) => problem.slug === selectedProblem.slug) : -1;

  function sourceLabel(problem: Problem) {
    const labels = [];
    if (problem.javaFileCount) labels.push(`${problem.javaFileCount} Java`);
    if (problem.pythonFileCount) labels.push(`${problem.pythonFileCount} Python`);
    return labels.join(" · ") || "Design prompt";
  }

  function selectSourceLanguage(language: "all" | "java" | "python") {
    setSourceLanguage(language);
    setFileQuery("");
    const nextPath = language === "python"
      ? selectedProblem?.pythonEntryFile
      : selectedProblem?.entryFile;
    setSelectedPath(nextPath ?? "");
  }

  function openQuestion(problem: Problem) {
    setSelectedSlug(problem.slug);
    setSelectedPath(problem.entryFile);
    setFileQuery("");
    setSourceLanguage("all");
    setView("explanation");
    window.history.pushState(null, "", `#question/${problem.slug}`);
  }

  function finishClose() {
    setSelectedSlug(null);
    setView("explanation");
    setFileQuery("");
    setSourceLanguage("all");
    if (problemSlugFromHash()) window.history.replaceState(null, "", "#questions");
  }

  function closeQuestion() {
    if (dialogRef.current?.open) dialogRef.current.close();
    else finishClose();
  }

  function navigateQuestion(offset: number) {
    if (selectedIndex < 0) return;
    const nextIndex = (selectedIndex + offset + problems.length) % problems.length;
    const next = problems[nextIndex];
    setSelectedSlug(next.slug);
    setSelectedPath(next.entryFile);
    setFileQuery("");
    setSourceLanguage("all");
    setView("explanation");
    window.history.replaceState(null, "", `#question/${next.slug}`);
  }

  function resolveQuestionLink(href: string) {
    return resolveLearningMarkdownLink(href, problems.map((problem) => problem.slug));
  }

  return (
    <div className="catalog-shell">
      <div className="catalog-tools">
        <label className="catalog-search">
          <span aria-hidden="true">⌕</span>
          <span className="sr-only">Search the question bank</span>
          <input
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search systems, domains, or concepts…"
            type="search"
            value={query}
          />
        </label>
        <p><strong>{filtered.length}</strong> of {problems.length} questions</p>
      </div>

      <div className="category-filters" aria-label="Filter questions by category">
        {categories.map((item) => (
          <button
            className={item === category ? "is-active" : ""}
            key={item}
            onClick={() => setCategory(item)}
            type="button"
          >
            {item}
            <span>{item === "All" ? problems.length : problems.filter((problem) => problem.category === item).length}</span>
          </button>
        ))}
      </div>

      <div className="catalog-intro-bar">
        <span aria-hidden="true">↳</span>
        <p>Every card opens a complete design explanation. Implemented questions also include their full source tree.</p>
      </div>

      <div className="catalog-grid">
        {filtered.map((problem) => {
          const index = problems.findIndex((candidate) => candidate.slug === problem.slug);
          return (
            <button
              aria-haspopup="dialog"
              className="problem-card"
              key={problem.slug}
              onClick={() => openQuestion(problem)}
              type="button"
            >
              <div className="problem-card-top">
                <span>{String(index + 1).padStart(2, "0")}</span>
                <span className={problem.javaFileCount || problem.pythonFileCount ? "status-coded" : "status-prompt"}>
                  {sourceLabel(problem)}
                </span>
              </div>
              <span className="problem-category">{problem.category}</span>
              <h3>{problem.title}</h3>
              <p>{problem.summary}</p>
              <span className="problem-card-action">Read full explanation <i aria-hidden="true">↗</i></span>
            </button>
          );
        })}
      </div>
      {filtered.length === 0 && <p className="empty-result">No questions match “{query}”. Try a domain such as commerce, booking, or concurrency.</p>}

      <dialog
        aria-labelledby="question-dialog-title"
        className="question-dialog"
        onClick={(event) => { if (event.target === event.currentTarget) closeQuestion(); }}
        onClose={finishClose}
        ref={dialogRef}
      >
        {selectedProblem && (
          <div className="question-dialog-shell">
            <header className="question-dialog-header">
              <div className="question-dialog-index">
                <span>{String(selectedIndex + 1).padStart(2, "0")} / {String(problems.length).padStart(2, "0")}</span>
                <span>{selectedProblem.category}</span>
              </div>
              <button aria-label="Close question" className="question-close" onClick={closeQuestion} type="button">×</button>
              <h2 id="question-dialog-title">{selectedProblem.title}</h2>
              <p>{selectedProblem.summary}</p>
              <div className="question-dialog-meta">
                <span>Full design brief</span>
                <span>{selectedProblem.javaFileCount + selectedProblem.pythonFileCount ? `${selectedProblem.javaFileCount + selectedProblem.pythonFileCount} source files` : "Specification exercise"}</span>
                <span>Canonical question</span>
              </div>
            </header>

            <div className="question-dialog-toolbar">
              <div className="view-switch" role="group" aria-label="Question content">
                <button className={view === "explanation" ? "is-active" : ""} onClick={() => setView("explanation")} type="button">Explanation</button>
                {selectedProblem.javaFileCount + selectedProblem.pythonFileCount > 0 && (
                  <button className={view === "source" ? "is-active" : ""} onClick={() => setView("source")} type="button">Source code</button>
                )}
              </div>
              <div className="question-navigation" aria-label="Question navigation">
                <button onClick={() => navigateQuestion(-1)} type="button"><span aria-hidden="true">←</span> Previous</button>
                <button onClick={() => navigateQuestion(1)} type="button">Next <span aria-hidden="true">→</span></button>
              </div>
            </div>

            <div className="question-dialog-content">
              {view === "explanation" ? (
                selectedDetail ? (
                  <MarkdownView hideTitle markdown={selectedDetail.readme} resolveLink={resolveQuestionLink} />
                ) : (
                  <div className="question-loading"><span />Loading the complete design explanation…</div>
                )
              ) : (
                <div className="question-source-workbench">
                  <aside className="question-file-browser">
                    <div className="source-language-switch question-language-switch" role="group" aria-label="Source language">
                      <button className={sourceLanguage === "all" ? "is-active" : ""} onClick={() => selectSourceLanguage("all")} type="button">All</button>
                      <button className={sourceLanguage === "java" ? "is-active" : ""} onClick={() => selectSourceLanguage("java")} type="button">Java</button>
                      <button className={sourceLanguage === "python" ? "is-active" : ""} onClick={() => selectSourceLanguage("python")} type="button">Python</button>
                    </div>
                    <label htmlFor="question-file-filter">Source files</label>
                    <input
                      id="question-file-filter"
                      onChange={(event) => setFileQuery(event.target.value)}
                      placeholder="Filter files…"
                      type="search"
                      value={fileQuery}
                    />
                    <div className="question-file-list">
                      <FileTree
                        activePath={activeFile?.path}
                        files={visibleFiles}
                        onSelect={setSelectedPath}
                      />
                    </div>
                  </aside>
                  <div className="question-mobile-file-select">
                    <div className="source-language-switch question-language-switch" role="group" aria-label="Source language">
                      <button className={sourceLanguage === "all" ? "is-active" : ""} onClick={() => selectSourceLanguage("all")} type="button">All</button>
                      <button className={sourceLanguage === "java" ? "is-active" : ""} onClick={() => selectSourceLanguage("java")} type="button">Java</button>
                      <button className={sourceLanguage === "python" ? "is-active" : ""} onClick={() => selectSourceLanguage("python")} type="button">Python</button>
                    </div>
                    <label htmlFor="question-mobile-file">Source file</label>
                    <select id="question-mobile-file" onChange={(event) => setSelectedPath(event.target.value)} value={activeFile?.path ?? ""}>
                      {languageFiles.map((file) => <option key={file.path} value={file.path}>{file.path}</option>)}
                    </select>
                  </div>
                  <div className="question-viewer-slot">
                    {activeFile ? <CodeViewer code={activeFile.code} path={activeFile.path} /> : <div className="question-loading">Loading source code…</div>}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </dialog>
    </div>
  );
}
