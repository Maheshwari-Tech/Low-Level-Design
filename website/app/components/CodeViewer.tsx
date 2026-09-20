"use client";

import { useMemo, useState } from "react";

const languageKeywords = new Set([
  "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
  "class", "const", "continue", "default", "do", "double", "else", "enum",
  "extends", "final", "finally", "float", "for", "if", "implements", "import",
  "instanceof", "int", "interface", "long", "native", "new", "package", "private",
  "protected", "public", "record", "return", "sealed", "short", "static", "strictfp",
  "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try",
  "var", "void", "volatile", "while", "yield", "true", "false", "null",
  "and", "as", "async", "await", "def", "elif", "except", "finally", "from", "global",
  "in", "is", "lambda", "None", "nonlocal", "not", "or", "pass", "raise", "True", "False",
  "with", "function", "let", "of", "typeof", "undefined", "export", "interface", "readonly",
  "type", "declare", "keyof", "namespace", "using", "virtual", "override", "operator", "template",
  "typename", "auto", "delete", "friend", "inline", "sizeof", "union", "constexpr", "func",
  "defer", "go", "chan", "map", "range", "select", "struct", "fallthrough", "impl", "match",
  "mut", "pub", "ref", "self", "Self", "trait", "where", "dyn", "crate", "mod", "use",
]);

const tokenPattern = /(\/\/.*$|#.*$|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])'|@[A-Za-z_$][\w$]*|\b\d+(?:\.\d+)?\b|\b[A-Za-z_$][\w$]*\b)/g;

function languageFromPath(filePath: string) {
  const extension = filePath.split(".").at(-1)?.toLowerCase();
  return ({
    c: "C", cc: "C++", cpp: "C++", cs: "C#", cql: "CQL", css: "CSS", go: "Go",
    h: "C/C++", hpp: "C++", html: "HTML", java: "Java", js: "JavaScript", json: "JSON",
    jsx: "JavaScript", md: "Markdown", py: "Python", rs: "Rust", sql: "SQL", ts: "TypeScript",
    tsx: "TypeScript", xml: "XML", yaml: "YAML", yml: "YAML",
  } as Record<string, string>)[extension ?? ""] ?? "Text";
}

function highlightedLine(line: string) {
  const fragments: React.ReactNode[] = [];
  let cursor = 0;
  let match: RegExpExecArray | null;
  tokenPattern.lastIndex = 0;

  while ((match = tokenPattern.exec(line))) {
    if (match.index > cursor) fragments.push(line.slice(cursor, match.index));
    const token = match[0];
    let className = "code-identifier";
    if (token.startsWith("//") || token.startsWith("#")) className = "code-comment";
    else if (token.startsWith('"') || token.startsWith("'")) className = "code-string";
    else if (token.startsWith("@")) className = "code-annotation";
    else if (/^\d/.test(token)) className = "code-number";
    else if (languageKeywords.has(token)) className = "code-keyword";
    else if (/^[A-Z]/.test(token)) className = "code-type";
    fragments.push(
      <span className={className} key={`${match.index}-${token}`}>
        {token}
      </span>,
    );
    cursor = match.index + token.length;
  }
  if (cursor < line.length) fragments.push(line.slice(cursor));
  return fragments.length ? fragments : " ";
}

export function CodeViewer({ code, path }: { code: string; path: string }) {
  const [copied, setCopied] = useState(false);
  const lines = useMemo(() => code.replace(/\r\n/g, "\n").split("\n"), [code]);

  async function copyCode() {
    await navigator.clipboard.writeText(code);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1600);
  }

  return (
    <div className="code-frame">
      <div className="code-toolbar">
        <div className="traffic-lights" aria-hidden="true">
          <span />
          <span />
          <span />
        </div>
        <span className="code-path" title={path}><b>{languageFromPath(path)}</b>{path}</span>
        <button className="copy-button" type="button" onClick={copyCode}>
          {copied ? "Copied" : "Copy"}
        </button>
      </div>
      <div className="code-scroll" tabIndex={0} aria-label={`Source code for ${path}`}>
        <pre className="code-block">
          <code>
            {lines.map((line, index) => (
              <span className="code-line" key={`${index}-${line}`}>
                <span className="line-number" aria-hidden="true">{index + 1}</span>
                <span className="line-source">{highlightedLine(line)}</span>
              </span>
            ))}
          </code>
        </pre>
      </div>
    </div>
  );
}
