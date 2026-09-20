import { Fragment, type ReactNode } from "react";

type LinkResolver = (href: string) => string | null;

function inlineMarkdown(text: string, resolveLink?: LinkResolver): ReactNode[] {
  const nodes: ReactNode[] = [];
  const pattern = /(`[^`]+`|\[([^\]]+)\]\(([^)]+)\)|\*\*([^*]+)\*\*)/g;
  let cursor = 0;
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(text))) {
    if (match.index > cursor) nodes.push(text.slice(cursor, match.index));
    if (match[0].startsWith("`")) {
      nodes.push(<code key={match.index}>{match[0].slice(1, -1)}</code>);
    } else if (match[2] && match[3]) {
      const href = match[3];
      const resolvedHref = resolveLink
        ? resolveLink(href)
        : (/^https?:\/\//.test(href) || href.startsWith("#") || href.startsWith("/") ? href : null);
      if (resolvedHref) {
        const isExternal = /^https?:\/\//.test(resolvedHref);
        nodes.push(
          <a
            className="readme-link"
            href={resolvedHref}
            key={match.index}
            rel={isExternal ? "noreferrer" : undefined}
            target={isExternal ? "_blank" : undefined}
          >
            {match[2]}
          </a>,
        );
      } else {
        nodes.push(<span className="readme-link" key={match.index} title={href}>{match[2]}</span>);
      }
    } else {
      nodes.push(<strong key={match.index}>{match[4]}</strong>);
    }
    cursor = match.index + match[0].length;
  }
  if (cursor < text.length) nodes.push(text.slice(cursor));
  return nodes;
}

function isDivider(line: string) {
  return /^\s*\|?(?:\s*:?-{3,}:?\s*\|)+\s*$/.test(line);
}

export function MarkdownView({
  markdown,
  hideTitle = false,
  resolveLink,
}: {
  markdown: string;
  hideTitle?: boolean;
  resolveLink?: LinkResolver;
}) {
  const lines = markdown.replace(/\r\n/g, "\n").split("\n");
  const content: ReactNode[] = [];

  for (let index = 0; index < lines.length;) {
    const line = lines[index];
    const key = `${index}-${line}`;

    if (line.startsWith("```")) {
      const language = line.slice(3).trim();
      const body: string[] = [];
      index += 1;
      while (index < lines.length && !lines[index].startsWith("```")) {
        body.push(lines[index]);
        index += 1;
      }
      content.push(
        <pre className="readme-code" key={key} data-language={language || undefined}>
          <code>{body.join("\n")}</code>
        </pre>,
      );
      index += 1;
      continue;
    }

    if (line.startsWith("|")) {
      const rows: string[][] = [];
      while (index < lines.length && lines[index].startsWith("|")) {
        if (!isDivider(lines[index])) {
          rows.push(lines[index].slice(1, -1).split("|").map((cell) => cell.trim()));
        }
        index += 1;
      }
      if (rows.length) {
        const [head, ...body] = rows;
        content.push(
          <div className="readme-table-wrap" key={key}>
            <table>
              <thead><tr>{head.map((cell, cellIndex) => <th key={cellIndex}>{inlineMarkdown(cell, resolveLink)}</th>)}</tr></thead>
              <tbody>{body.map((row, rowIndex) => <tr key={rowIndex}>{row.map((cell, cellIndex) => <td key={cellIndex}>{inlineMarkdown(cell, resolveLink)}</td>)}</tr>)}</tbody>
            </table>
          </div>,
        );
      }
      continue;
    }

    if (/^>\s?/.test(line)) {
      const quotation: string[] = [];
      while (index < lines.length && /^>\s?/.test(lines[index])) {
        quotation.push(lines[index].replace(/^>\s?/, "").trim());
        index += 1;
      }
      content.push(
        <blockquote key={key}>
          <p>{inlineMarkdown(quotation.join(" "), resolveLink)}</p>
        </blockquote>,
      );
      continue;
    }

    const heading = /^(#{1,4})\s+(.+)$/.exec(line);
    if (heading) {
      const level = heading[1].length;
      if (hideTitle && level === 1 && content.length === 0) {
        index += 1;
        continue;
      }
      const Heading = `h${Math.min(level + 1, 4)}` as "h2" | "h3" | "h4";
      content.push(<Heading key={key}>{inlineMarkdown(heading[2], resolveLink)}</Heading>);
      index += 1;
      continue;
    }

    const bullet = /^\s*[-*]\s+(.+)$/.exec(line);
    const numbered = /^\s*\d+\.\s+(.+)$/.exec(line);
    if (bullet || numbered) {
      content.push(
        <div className="readme-list-row" key={key}>
          <span aria-hidden="true">{numbered ? `${line.trim().split(".")[0]}.` : "•"}</span>
          <p>{inlineMarkdown((bullet ?? numbered)?.[1] ?? "", resolveLink)}</p>
        </div>,
      );
      index += 1;
      continue;
    }

    if (!line.trim() || /^---+$/.test(line.trim())) {
      index += 1;
      continue;
    }

    const paragraph = [line.trim()];
    index += 1;
    while (
      index < lines.length &&
      lines[index].trim() &&
      !/^(#{1,4})\s|^```|^\||^>\s?|^\s*[-*]\s|^\s*\d+\.\s/.test(lines[index])
    ) {
      paragraph.push(lines[index].trim());
      index += 1;
    }
    content.push(<p key={key}>{inlineMarkdown(paragraph.join(" "), resolveLink)}</p>);
  }

  return <article className="readme-view">{content.map((node, index) => <Fragment key={index}>{node}</Fragment>)}</article>;
}
