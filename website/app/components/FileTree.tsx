"use client";

import { useMemo, type CSSProperties } from "react";

type SourceFile = { path: string };

type DirectoryNode = {
  name: string;
  path: string;
  directories: DirectoryNode[];
  files: SourceFile[];
  fileCount: number;
};

type MutableDirectory = {
  name: string;
  path: string;
  directories: Map<string, MutableDirectory>;
  files: SourceFile[];
};

function buildTree(files: readonly SourceFile[]): DirectoryNode {
  const root: MutableDirectory = {
    name: "",
    path: "",
    directories: new Map(),
    files: [],
  };

  for (const file of files) {
    const segments = file.path.split("/").filter(Boolean);
    let directory = root;
    for (const segment of segments.slice(0, -1)) {
      const nextPath = directory.path ? `${directory.path}/${segment}` : segment;
      let child = directory.directories.get(segment);
      if (!child) {
        child = { name: segment, path: nextPath, directories: new Map(), files: [] };
        directory.directories.set(segment, child);
      }
      directory = child;
    }
    directory.files.push(file);
  }

  function freeze(node: MutableDirectory): DirectoryNode {
    const directories = [...node.directories.values()]
      .sort((left, right) => left.name.localeCompare(right.name))
      .map(freeze);
    const sortedFiles = [...node.files].sort((left, right) => left.path.localeCompare(right.path));
    return {
      name: node.name,
      path: node.path,
      directories,
      files: sortedFiles,
      fileCount: sortedFiles.length + directories.reduce((sum, child) => sum + child.fileCount, 0),
    };
  }

  return freeze(root);
}

function depthStyle(depth: number) {
  return { "--tree-depth": depth } as CSSProperties;
}

function FileButton({
  file,
  depth,
  activePath,
  onSelect,
}: {
  file: SourceFile;
  depth: number;
  activePath: string | undefined;
  onSelect: (path: string) => void;
}) {
  const name = file.path.split("/").at(-1) ?? file.path;
  const extension = name.split(".").at(-1)?.toLowerCase();
  const icon = extension === "py" ? "Py" : extension === "java" ? "J" : "·";
  return (
    <button
      aria-current={file.path === activePath ? "true" : undefined}
      aria-selected={file.path === activePath}
      className={`source-tree-file ${file.path === activePath ? "is-active" : ""}`}
      onClick={() => onSelect(file.path)}
      role="treeitem"
      style={depthStyle(depth)}
      title={file.path}
      type="button"
    >
      <span aria-hidden="true">{icon}</span>
      <span>{name}</span>
    </button>
  );
}

function Folder({
  node,
  depth,
  activePath,
  onSelect,
}: {
  node: DirectoryNode;
  depth: number;
  activePath: string | undefined;
  onSelect: (path: string) => void;
}) {
  return (
    <details className="source-tree-folder" open key={node.path}>
      <summary aria-selected={false} role="treeitem" style={depthStyle(depth)} title={node.path}>
        <span className="source-tree-chevron" aria-hidden="true">›</span>
        <span className="source-tree-folder-icon" aria-hidden="true">DIR</span>
        <span className="source-tree-folder-name">{node.name}</span>
        <span className="source-tree-count">{node.fileCount}</span>
      </summary>
      <div role="group">
        {node.directories.map((child) => (
          <Folder
            activePath={activePath}
            depth={depth + 1}
            key={child.path}
            node={child}
            onSelect={onSelect}
          />
        ))}
        {node.files.map((file) => (
          <FileButton
            activePath={activePath}
            depth={depth + 1}
            file={file}
            key={file.path}
            onSelect={onSelect}
          />
        ))}
      </div>
    </details>
  );
}

export function FileTree({
  files,
  activePath,
  onSelect,
}: {
  files: readonly SourceFile[];
  activePath?: string;
  onSelect: (path: string) => void;
}) {
  const tree = useMemo(() => buildTree(files), [files]);
  if (!tree.fileCount) return <p className="source-tree-empty">No matching source files.</p>;

  return (
    <div className="source-file-tree" role="tree" aria-label="Source folder tree">
      {tree.directories.map((directory) => (
        <Folder
          activePath={activePath}
          depth={0}
          key={directory.path}
          node={directory}
          onSelect={onSelect}
        />
      ))}
      {tree.files.map((file) => (
        <FileButton
          activePath={activePath}
          depth={0}
          file={file}
          key={file.path}
          onSelect={onSelect}
        />
      ))}
    </div>
  );
}
