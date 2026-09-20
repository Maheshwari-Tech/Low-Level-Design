"use client";

import { useEffect, useMemo, useState } from "react";
import { CodeViewer } from "./CodeViewer";
import { MarkdownView } from "./MarkdownView";
import { lldPublicAsset } from "../lib/public-asset.mjs";

const PAGE_SIZE = 80;

type RepositoryIndex = {
  id: string;
  title: string;
  commit: string | null;
  branch: string | null;
  remote: string | null;
  localPath: string;
  license: string;
  role: string;
  fileCount: number;
  previewableFiles: number;
  bytes: number;
  byKind: Readonly<Record<string, number>>;
  byLanguage: Readonly<Record<string, number>>;
};

type RepositoryEntry = {
  path: string;
  topLevel: string;
  kind: string;
  language: string;
  size: number;
  sourceUrl: string | null;
  generated: boolean;
  content: string | null;
};

type RepositoryPayload = RepositoryIndex & {
  entries: RepositoryEntry[];
};

function sourceRouteFromHash() {
  if (typeof window === "undefined") return null;
  const match = /^#source\/([a-z0-9-]+)(?:\?path=([^&]+))?$/.exec(window.location.hash);
  if (!match) return null;
  try {
    return { id: match[1], path: match[2] ? decodeURIComponent(match[2]) : "" };
  } catch {
    return { id: match[1], path: "" };
  }
}

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(bytes < 10240 ? 1 : 0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function isGeneratedOrBinary(entry: RepositoryEntry) {
  return entry.generated || entry.kind === "Generated / binary";
}

function localLearningHref(href: string, currentPath: string) {
  let resolvedPath: string;
  try {
    resolvedPath = new URL(href, `https://lld.local/${currentPath}`).pathname
      .replace(/^\//, "")
      .replace(/\/$/, "");
  } catch {
    return null;
  }
  if (resolvedPath === "Problems" || resolvedPath === "Problems/README.md") return "#questions";
  const problem = /^Problems\/([^/]+)(?:\/(?:README|readme)\.md)?$/.exec(resolvedPath);
  if (problem) return `#question/${problem[1]}`;
  if (resolvedPath === "Patterns" || resolvedPath === "Patterns/README.md") return "#patterns";
  const pattern = /^Patterns\/(creational|structural|behavioral)\/([^/]+)(?:\/(?:README|readme)\.md)?$/.exec(resolvedPath);
  if (pattern) return `#pattern/${pattern[1]}_${pattern[2]}`;
  if (resolvedPath === "Principles" || resolvedPath === "Principles/README.md") return "#principles";
  if (resolvedPath === "Principles/other.md") return "#principle/complementary_principles";
  const principle = /^Principles\/([^/]+)(?:\/(?:README|readme)\.md)?$/.exec(resolvedPath);
  if (principle) return `#principle/${principle[1]}`;
  const foundation = /^Foundations\/(object_oriented_programming|class_relationships|clean_code|testing)\.md$/.exec(resolvedPath);
  if (foundation) return `#lesson/${foundation[1] === "testing" ? "testing_lld" : foundation[1]}`;
  if (resolvedPath === "Foundations" || resolvedPath === "Foundations/README.md") return "#learning";
  if (
    resolvedPath === "Concurrency/JAVA_MULTITHREADING_INTERVIEW_GUIDE.md" ||
    resolvedPath === "Concurrency/interview/README.md"
  ) {
    return "#lesson/java_concurrency_lld_interviews";
  }
  if (resolvedPath === "Concurrency" || resolvedPath === "Concurrency/README.md") return "#lesson/concurrency_fundamentals";
  if (resolvedPath === "Concurrency/Multithreading/Introduction.md") return "#lesson/multithreading_introduction";
  if (resolvedPath === "Concurrency/questions/README.md") return "#lesson/concurrency_questions";
  if (resolvedPath === "UML" || resolvedPath === "UML/README.md") return "#lesson/uml_for_lld";
  return null;
}

function resolveRepositoryMarkdownLink(href: string, sourceUrl: string | null, currentPath: string) {
  if (/^https?:\/\//.test(href)) return href;
  if (!sourceUrl) return localLearningHref(href, currentPath);
  try {
    return new URL(href, sourceUrl).toString();
  } catch {
    return null;
  }
}

export function SourceLibrary({ repositories }: { repositories: readonly RepositoryIndex[] }) {
  const [selectedRepositoryId, setSelectedRepositoryId] = useState<string | null>(null);
  const [payloads, setPayloads] = useState<Record<string, RepositoryPayload>>({});
  const [loadErrors, setLoadErrors] = useState<Record<string, string>>({});
  const [query, setQuery] = useState("");
  const [kind, setKind] = useState("All kinds");
  const [language, setLanguage] = useState("All languages");
  const [includeGenerated, setIncludeGenerated] = useState(false);
  const [page, setPage] = useState(0);
  const [selectedPath, setSelectedPath] = useState("");

  function resetBrowser() {
    setQuery("");
    setKind("All kinds");
    setLanguage("All languages");
    setIncludeGenerated(false);
    setPage(0);
    setSelectedPath("");
  }

  useEffect(() => {
    function syncWithUrl() {
      const route = sourceRouteFromHash();
      const repository = repositories.find((candidate) => candidate.id === route?.id);
      setSelectedRepositoryId(repository?.id ?? null);
      resetBrowser();
      if (repository && route?.path) {
        setQuery(route.path);
        setSelectedPath(route.path);
      }
      if (repository) {
        window.requestAnimationFrame(() => {
          document.getElementById("sources")?.scrollIntoView({ block: "start" });
        });
      }
    }

    syncWithUrl();
    window.addEventListener("hashchange", syncWithUrl);
    window.addEventListener("popstate", syncWithUrl);
    return () => {
      window.removeEventListener("hashchange", syncWithUrl);
      window.removeEventListener("popstate", syncWithUrl);
    };
  }, [repositories]);

  useEffect(() => {
    if (!selectedRepositoryId || payloads[selectedRepositoryId]) return;
    const controller = new AbortController();
    const id = selectedRepositoryId;

    fetch(lldPublicAsset(`/repository-content/${encodeURIComponent(id)}.json`), { signal: controller.signal })
      .then((response) => {
        if (!response.ok) throw new Error(`Unable to load repository index (${response.status}).`);
        return response.json() as Promise<RepositoryPayload>;
      })
      .then((payload) => {
        setPayloads((current) => ({ ...current, [id]: payload }));
        setLoadErrors((current) => {
          const next = { ...current };
          delete next[id];
          return next;
        });
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        const message = error instanceof Error ? error.message : "Unable to load repository index.";
        setLoadErrors((current) => ({ ...current, [id]: message }));
      });

    return () => controller.abort();
  }, [payloads, selectedRepositoryId]);

  const selectedRepository = repositories.find((repository) => repository.id === selectedRepositoryId);
  const selectedPayload = selectedRepositoryId ? payloads[selectedRepositoryId] : undefined;
  const kindOptions = useMemo(
    () => Object.entries(selectedPayload?.byKind ?? selectedRepository?.byKind ?? {})
      .sort((left, right) => right[1] - left[1]),
    [selectedPayload, selectedRepository],
  );
  const languageOptions = useMemo(
    () => Object.entries(selectedPayload?.byLanguage ?? selectedRepository?.byLanguage ?? {})
      .sort((left, right) => right[1] - left[1]),
    [selectedPayload, selectedRepository],
  );
  const hiddenArtifactCount = useMemo(
    () => selectedPayload?.entries.filter(isGeneratedOrBinary).length ?? 0,
    [selectedPayload],
  );
  const filteredEntries = useMemo(() => {
    if (!selectedPayload) return [];
    const needle = query.trim().toLowerCase();
    return selectedPayload.entries.filter((entry) => {
      if (!includeGenerated && isGeneratedOrBinary(entry)) return false;
      if (kind !== "All kinds" && entry.kind !== kind) return false;
      if (language !== "All languages" && entry.language !== language) return false;
      return !needle || `${entry.path} ${entry.topLevel}`.toLowerCase().includes(needle);
    });
  }, [includeGenerated, kind, language, query, selectedPayload]);
  const pageCount = Math.max(1, Math.ceil(filteredEntries.length / PAGE_SIZE));
  const safePage = Math.min(page, pageCount - 1);
  const pageEntries = filteredEntries.slice(safePage * PAGE_SIZE, (safePage + 1) * PAGE_SIZE);
  const selectedEntry = filteredEntries.find((entry) => entry.path === selectedPath) ?? pageEntries[0];

  function openRepository(repository: RepositoryIndex) {
    setSelectedRepositoryId(repository.id);
    resetBrowser();
    const nextHash = `#source/${repository.id}`;
    if (window.location.hash !== nextHash) window.history.pushState(null, "", nextHash);
  }

  function updateQuery(value: string) {
    setQuery(value);
    setPage(0);
    setSelectedPath("");
  }

  function updateKind(value: string) {
    setKind(value);
    setPage(0);
    setSelectedPath("");
  }

  function updateLanguage(value: string) {
    setLanguage(value);
    setPage(0);
    setSelectedPath("");
  }

  function toggleGenerated(checked: boolean) {
    setIncludeGenerated(checked);
    setPage(0);
    setSelectedPath("");
  }

  function navigatePage(nextPage: number) {
    setPage(nextPage);
    setSelectedPath("");
  }

  return (
    <div className="source-library-shell">
      <div className="repository-grid" aria-label="Indexed repositories">
        {repositories.map((repository, index) => (
          <article className={repository.id === selectedRepositoryId ? "repository-card is-active" : "repository-card"} key={repository.id}>
            <button onClick={() => openRepository(repository)} type="button">
              <div className="repository-card-top">
                <span>{String(index + 1).padStart(2, "0")}</span>
                <span>{repository.fileCount.toLocaleString()} files</span>
              </div>
              <h3>{repository.title}</h3>
              <p>{repository.role}</p>
              <dl>
                <div><dt>Branch</dt><dd>{repository.branch ?? "Not pinned"}</dd></div>
                <div><dt>Commit</dt><dd title={repository.commit ?? "No pinned commit"}>{repository.commit?.slice(0, 8) ?? "Not pinned"}</dd></div>
                <div><dt>License</dt><dd>{repository.license}</dd></div>
                <div><dt>Local path</dt><dd title={repository.localPath}>{repository.localPath}</dd></div>
              </dl>
              <span className="repository-card-action">Search this repository <i aria-hidden="true">↗</i></span>
            </button>
            <div className="repository-card-footer">
              <span>{repository.previewableFiles.toLocaleString()} previewable</span>
              {repository.remote ? (
                <a href={repository.remote} rel="noreferrer" target="_blank">Open repository <span aria-hidden="true">↗</span></a>
              ) : (
                <span>Local working tree</span>
              )}
            </div>
          </article>
        ))}
      </div>

      {!selectedRepository && (
        <div className="source-library-empty">
          <span aria-hidden="true">↳</span>
          <div>
            <h3>Choose a repository to search.</h3>
            <p>Each index is loaded only when opened. Generated outputs and binary artifacts remain hidden until you ask to see them.</p>
          </div>
        </div>
      )}

      {selectedRepository && !selectedPayload && (
        <div className="source-library-loading" role="status">
          {loadErrors[selectedRepository.id] ? (
            <>
              <strong>Repository index unavailable.</strong>
              <span>{loadErrors[selectedRepository.id]}</span>
            </>
          ) : (
            <>
              <i aria-hidden="true" />
              <strong>Loading {selectedRepository.title}</strong>
              <span>Fetching its revision-pinned file index…</span>
            </>
          )}
        </div>
      )}

      {selectedRepository && selectedPayload && (
        <div className="source-browser">
          <header className="source-browser-header">
            <div>
              <p className="eyebrow">Revision-pinned source</p>
              <h3>{selectedPayload.title}</h3>
              <p>{selectedPayload.role}</p>
            </div>
            <dl>
              <div><dt>Branch</dt><dd>{selectedPayload.branch ?? "Not pinned"}</dd></div>
              <div><dt>Commit</dt><dd title={selectedPayload.commit ?? "No pinned commit"}>{selectedPayload.commit?.slice(0, 8) ?? "Not pinned"}</dd></div>
              <div><dt>License</dt><dd>{selectedPayload.license}</dd></div>
              <div><dt>Local path</dt><dd title={selectedPayload.localPath}>{selectedPayload.localPath}</dd></div>
            </dl>
            {selectedPayload.remote && (
              <a className="source-external-link" href={selectedPayload.remote} rel="noreferrer" target="_blank">
                Open exact repository <span aria-hidden="true">↗</span>
              </a>
            )}
          </header>

          <div className="source-controls">
            <label className="source-query">
              <span>Search paths</span>
              <input
                onChange={(event) => updateQuery(event.target.value)}
                placeholder="Try parking, strategy, README…"
                type="search"
                value={query}
              />
            </label>
            <label>
              <span>Kind</span>
              <select onChange={(event) => updateKind(event.target.value)} value={kind}>
                <option>All kinds</option>
                {kindOptions.map(([option, count]) => <option key={option} value={option}>{option} ({count.toLocaleString()})</option>)}
              </select>
            </label>
            <label>
              <span>Language</span>
              <select onChange={(event) => updateLanguage(event.target.value)} value={language}>
                <option>All languages</option>
                {languageOptions.map(([option, count]) => <option key={option} value={option}>{option} ({count.toLocaleString()})</option>)}
              </select>
            </label>
            <label className="artifact-toggle">
              <input checked={includeGenerated} onChange={(event) => toggleGenerated(event.target.checked)} type="checkbox" />
              <span><strong>Generated / binary</strong><small>{includeGenerated ? "Shown" : `${hiddenArtifactCount.toLocaleString()} hidden`}</small></span>
            </label>
          </div>

          <div className="source-results-bar">
            <p><strong>{filteredEntries.length.toLocaleString()}</strong> matching files <span>·</span> {formatBytes(selectedPayload.bytes)} indexed</p>
            <p>Showing {filteredEntries.length ? safePage * PAGE_SIZE + 1 : 0}–{Math.min((safePage + 1) * PAGE_SIZE, filteredEntries.length)} of {filteredEntries.length.toLocaleString()}</p>
          </div>

          {filteredEntries.length ? (
            <div className="repository-workbench">
              <aside className="repository-file-panel">
                <div className="repository-file-list">
                  {pageEntries.map((entry) => (
                    <button
                      className={entry.path === selectedEntry?.path ? "is-active" : ""}
                      key={entry.path}
                      onClick={() => setSelectedPath(entry.path)}
                      title={entry.path}
                      type="button"
                    >
                      <span className="repository-file-icon" aria-hidden="true">{entry.language.slice(0, 1)}</span>
                      <span className="repository-file-name">{entry.path}</span>
                      <span className="repository-file-meta">{entry.language} · {formatBytes(entry.size)}</span>
                      {isGeneratedOrBinary(entry) && <span className="artifact-badge">Artifact</span>}
                    </button>
                  ))}
                </div>
                <div className="source-pagination" aria-label="File result pages">
                  <button disabled={safePage === 0} onClick={() => navigatePage(safePage - 1)} type="button">← Prev</button>
                  <span>Page {safePage + 1} / {pageCount}</span>
                  <button disabled={safePage >= pageCount - 1} onClick={() => navigatePage(safePage + 1)} type="button">Next →</button>
                </div>
              </aside>

              <div className="repository-preview">
                {selectedEntry && (
                  <>
                    <div className="repository-preview-meta">
                      <div>
                        <span>{selectedEntry.kind}</span>
                        <span>{selectedEntry.language}</span>
                        <span>{formatBytes(selectedEntry.size)}</span>
                        {isGeneratedOrBinary(selectedEntry) && <span>Generated / binary</span>}
                      </div>
                      {selectedEntry.sourceUrl ? (
                        <a href={selectedEntry.sourceUrl} rel="noreferrer" target="_blank">View exact source <span aria-hidden="true">↗</span></a>
                      ) : (
                        <span>Local working-tree file</span>
                      )}
                    </div>
                    {selectedEntry.content === null ? (
                      <div className="nonpreviewable-file">
                        <span aria-hidden="true">∅</span>
                        <h4>Preview unavailable</h4>
                        <p><code>{selectedEntry.path}</code> is indexed as {selectedEntry.kind.toLowerCase()} ({selectedEntry.language}, {formatBytes(selectedEntry.size)}).</p>
                        {selectedEntry.sourceUrl ? (
                          <a href={selectedEntry.sourceUrl} rel="noreferrer" target="_blank">Open the exact file on GitHub <span aria-hidden="true">↗</span></a>
                        ) : (
                          <p className="local-source-path">Local path: <code>{selectedPayload.localPath}/{selectedEntry.path}</code></p>
                        )}
                      </div>
                    ) : selectedEntry.language === "Markdown" ? (
                      <MarkdownView
                        markdown={selectedEntry.content}
                        resolveLink={(href) => resolveRepositoryMarkdownLink(href, selectedEntry.sourceUrl, selectedEntry.path)}
                      />
                    ) : (
                      <CodeViewer code={selectedEntry.content} path={selectedEntry.path} />
                    )}
                  </>
                )}
              </div>
            </div>
          ) : (
            <div className="source-no-results">
              <strong>No files match these filters.</strong>
              <p>Try a shorter path, another language, or show generated and binary artifacts.</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
