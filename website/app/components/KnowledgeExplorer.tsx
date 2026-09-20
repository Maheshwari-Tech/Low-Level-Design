"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { CodeViewer } from "./CodeViewer";
import { FileTree } from "./FileTree";
import { resolveLearningMarkdownLink } from "./learningLinks";
import { MarkdownView } from "./MarkdownView";
import { lldPublicAsset } from "../lib/public-asset.mjs";

type KnowledgeKind = "lesson" | "principle" | "pattern";

type KnowledgeItem = {
  title: string;
  slug: string;
  summary: string;
  group: string;
  javaFileCount: number;
  entryFile: string;
};

type KnowledgeDetail = KnowledgeItem & {
  readme: string;
  files: { path: string; code: string }[];
};

type ContentPayload = {
  lessons: KnowledgeDetail[];
  principles: KnowledgeDetail[];
  patterns: KnowledgeDetail[];
};

function knowledgeSlugFromHash(kind: KnowledgeKind) {
  if (typeof window === "undefined") return null;
  const match = new RegExp(`^#${kind}/([a-z0-9_-]+)$`).exec(window.location.hash);
  return match?.[1] ?? null;
}

function groupSlug(group: string) {
  return group.toLowerCase().replace(/[^a-z0-9]+/g, "-");
}

export function KnowledgeExplorer({
  items,
  kind,
}: {
  items: readonly KnowledgeItem[];
  kind: KnowledgeKind;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [payload, setPayload] = useState<ContentPayload | null>(null);
  const [selectedSlug, setSelectedSlug] = useState<string | null>(null);
  const [selectedPath, setSelectedPath] = useState("");
  const [view, setView] = useState<"explanation" | "source">("explanation");
  const [fileQuery, setFileQuery] = useState("");
  const payloadKey = kind === "lesson" ? "lessons" : kind === "principle" ? "principles" : "patterns";
  const sectionHash = kind === "lesson" ? "#learning" : kind === "principle" ? "#principles" : "#patterns";
  const kindLabel = kind === "lesson" ? "lesson" : kind === "principle" ? "principle" : "design pattern";

  useEffect(() => {
    let active = true;
    fetch(lldPublicAsset("/lld-content.json"))
      .then((response) => {
        if (!response.ok) throw new Error("Unable to load the knowledge library.");
        return response.json() as Promise<ContentPayload>;
      })
      .then((data) => { if (active) setPayload(data); })
      .catch(() => {
        if (active) setPayload({ lessons: [], principles: [], patterns: [] });
      });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    function syncWithUrl() {
      const slug = knowledgeSlugFromHash(kind);
      const item = items.find((candidate) => candidate.slug === slug);
      if (item) {
        setSelectedSlug(item.slug);
        setSelectedPath(item.entryFile);
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
  }, [items, kind]);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (selectedSlug && dialog && !dialog.open) dialog.showModal();
    if (!selectedSlug && dialog?.open) dialog.close();
  }, [selectedSlug]);

  const groups = useMemo(
    () => Array.from(new Set(items.map((item) => item.group))),
    [items],
  );
  const details = payload?.[payloadKey] ?? [];
  const selectedItem = items.find((item) => item.slug === selectedSlug);
  const selectedDetail = details.find((item) => item.slug === selectedSlug);
  const files = useMemo(() => selectedDetail?.files ?? [], [selectedDetail]);
  const visibleFiles = useMemo(() => {
    const needle = fileQuery.trim().toLowerCase();
    return needle ? files.filter((file) => file.path.toLowerCase().includes(needle)) : files;
  }, [fileQuery, files]);
  const activeFile = files.find((file) => file.path === selectedPath) ??
    files.find((file) => file.path === selectedItem?.entryFile) ??
    files[0];
  const selectedIndex = selectedItem
    ? items.findIndex((item) => item.slug === selectedItem.slug)
    : -1;

  function openItem(item: KnowledgeItem) {
    setSelectedSlug(item.slug);
    setSelectedPath(item.entryFile);
    setFileQuery("");
    setView("explanation");
    window.history.pushState(null, "", `#${kind}/${item.slug}`);
  }

  function finishClose() {
    setSelectedSlug(null);
    setFileQuery("");
    setView("explanation");
    if (knowledgeSlugFromHash(kind)) {
      window.history.replaceState(null, "", sectionHash);
    }
  }

  function closeItem() {
    if (dialogRef.current?.open) dialogRef.current.close();
    else finishClose();
  }

  function navigateItem(offset: number) {
    if (selectedIndex < 0) return;
    const nextIndex = (selectedIndex + offset + items.length) % items.length;
    const next = items[nextIndex];
    setSelectedSlug(next.slug);
    setSelectedPath(next.entryFile);
    setFileQuery("");
    setView("explanation");
    window.history.replaceState(null, "", `#${kind}/${next.slug}`);
  }

  function resolveKnowledgeLink(href: string) {
    const workspaceLink = resolveLearningMarkdownLink(href);
    if (workspaceLink) return workspaceLink;
    const normalized = href
      .split("#")[0]
      .replace(/\\/g, "/")
      .replace(/\/?(?:README|readme)\.md$/, "")
      .replace(/\.md$/, "")
      .replace(/[^a-zA-Z0-9]+/g, "_")
      .replace(/^_|_$/g, "")
      .toLowerCase();
    const linkedItem = items.find((item) =>
      item.slug === normalized || item.slug.endsWith(`_${normalized}`),
    );
    return linkedItem ? `#${kind}/${linkedItem.slug}` : null;
  }

  return (
    <div className={`knowledge-shell knowledge-${kind}`}>
      <div className="knowledge-summary-bar">
        <p><strong>{items.length}</strong> complete guides</p>
        <div aria-label={`${kindLabel} groups`}>
          {groups.map((group) => (
            <a href={`#${kind}-${groupSlug(group)}`} key={group}>
              {group}
              <span>{items.filter((item) => item.group === group).length}</span>
            </a>
          ))}
        </div>
      </div>

      <div className="knowledge-groups">
        {groups.map((group) => {
          const groupItems = items.filter((item) => item.group === group);
          return (
            <section
              className="knowledge-group"
              data-group={groupSlug(group)}
              id={`${kind}-${groupSlug(group)}`}
              key={group}
            >
              <header className="knowledge-group-header">
                <span>{String(groups.indexOf(group) + 1).padStart(2, "0")}</span>
                <div>
                  <h3>{group}</h3>
                  <p>{groupItems.length} {groupItems.length === 1 ? "guide" : "guides"}</p>
                </div>
              </header>
              <div className="knowledge-card-grid">
                {groupItems.map((item) => {
                  const index = items.findIndex((candidate) => candidate.slug === item.slug);
                  const symbol = kind === "principle"
                    ? (item.group === "SOLID" ? "SOLID"[index] : "+")
                    : String(index + 1).padStart(2, "0");
                  return (
                    <button
                      aria-haspopup="dialog"
                      className="knowledge-card"
                      key={item.slug}
                      onClick={() => openItem(item)}
                      type="button"
                    >
                      <div className="knowledge-card-top">
                        <span className="knowledge-symbol">{symbol}</span>
                        <span className={item.javaFileCount ? "status-coded" : "status-prompt"}>
                          {item.javaFileCount ? `${item.javaFileCount} Java` : "Guide"}
                        </span>
                      </div>
                      <h4>{item.title}</h4>
                      <p>{item.summary}</p>
                      <span className="knowledge-card-action">
                        Open guide <i aria-hidden="true">↗</i>
                      </span>
                    </button>
                  );
                })}
              </div>
            </section>
          );
        })}
      </div>

      <dialog
        aria-labelledby={`${kind}-dialog-title`}
        className="question-dialog knowledge-dialog"
        data-kind={kind}
        onClick={(event) => { if (event.target === event.currentTarget) closeItem(); }}
        onClose={finishClose}
        ref={dialogRef}
      >
        {selectedItem && (
          <div className="question-dialog-shell">
            <header className="question-dialog-header knowledge-dialog-header">
              <div className="question-dialog-index">
                <span>{String(selectedIndex + 1).padStart(2, "0")} / {String(items.length).padStart(2, "0")}</span>
                <span>{selectedItem.group} {kindLabel}</span>
              </div>
              <button aria-label={`Close ${kindLabel}`} className="question-close" onClick={closeItem} type="button">×</button>
              <h2 id={`${kind}-dialog-title`}>{selectedItem.title}</h2>
              <p>{selectedItem.summary}</p>
              <div className="question-dialog-meta">
                <span>Intent &amp; mechanics</span>
                <span>{selectedItem.javaFileCount ? `${selectedItem.javaFileCount} source files` : "Concept guide"}</span>
                <span>Trade-offs included</span>
              </div>
            </header>

            <div className="question-dialog-toolbar">
              <div className="view-switch" role="group" aria-label={`${kindLabel} content`}>
                <button className={view === "explanation" ? "is-active" : ""} onClick={() => setView("explanation")} type="button">Explanation</button>
                {selectedItem.javaFileCount > 0 && (
                  <button className={view === "source" ? "is-active" : ""} onClick={() => setView("source")} type="button">Source code</button>
                )}
              </div>
              <div className="question-navigation" aria-label={`${kindLabel} navigation`}>
                <button onClick={() => navigateItem(-1)} type="button"><span aria-hidden="true">←</span> Previous</button>
                <button onClick={() => navigateItem(1)} type="button">Next <span aria-hidden="true">→</span></button>
              </div>
            </div>

            <div className="question-dialog-content">
              {view === "explanation" ? (
                selectedDetail ? (
                  <MarkdownView hideTitle markdown={selectedDetail.readme} resolveLink={resolveKnowledgeLink} />
                ) : (
                  <div className="question-loading"><span />Loading the complete guide…</div>
                )
              ) : (
                <div className="question-source-workbench">
                  <aside className="question-file-browser">
                    <label htmlFor={`${kind}-file-filter`}>Example files</label>
                    <input
                      id={`${kind}-file-filter`}
                      onChange={(event) => setFileQuery(event.target.value)}
                      placeholder="Filter files…"
                      type="search"
                      value={fileQuery}
                    />
                    <FileTree
                      activePath={activeFile?.path}
                      files={visibleFiles}
                      onSelect={setSelectedPath}
                    />
                  </aside>
                  <div className="question-mobile-file-select">
                    <label htmlFor={`${kind}-mobile-file`}>Example file</label>
                    <select
                      id={`${kind}-mobile-file`}
                      onChange={(event) => setSelectedPath(event.target.value)}
                      value={activeFile?.path ?? ""}
                    >
                      {files.map((file) => <option key={file.path} value={file.path}>{file.path}</option>)}
                    </select>
                  </div>
                  <div className="question-viewer-slot">
                    {activeFile
                      ? <CodeViewer code={activeFile.code} path={activeFile.path} />
                      : <div className="question-loading">Loading source code…</div>}
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
