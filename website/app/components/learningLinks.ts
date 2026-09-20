function unwrapDestination(href: string) {
  const trimmed = href.trim();
  return trimmed.startsWith("<") && trimmed.endsWith(">")
    ? trimmed.slice(1, -1)
    : trimmed;
}

function normalizedSegments(href: string) {
  return href
    .split("#", 1)[0]
    .replace(/\\/g, "/")
    .split("/")
    .filter((segment) => segment && segment !== "." && segment !== "..");
}

function sourceHref(repositoryId: string, path: readonly string[]) {
  const suffix = path.length ? `?path=${encodeURIComponent(path.join("/"))}` : "";
  return `#source/${repositoryId}${suffix}`;
}

export function resolveLearningMarkdownLink(
  rawHref: string,
  problemSlugs: readonly string[] = [],
) {
  const href = unwrapDestination(rawHref);
  if (/^https?:\/\//.test(href) || href.startsWith("#") || href.startsWith("/")) {
    return href;
  }

  const segments = normalizedSegments(href);
  const referenceIndex = segments.indexOf("References");
  if (referenceIndex >= 0) {
    const repository = segments[referenceIndex + 1];
    const pathInsideClone = segments.slice(referenceIndex + 2);
    if (repository === "awesome-low-level-design") {
      return sourceHref("awesome-low-level-design", pathInsideClone);
    }
    if (repository === "low-level-design-primer") {
      return sourceHref("low-level-design-primer", pathInsideClone);
    }
    if (repository === "kumaransg-LLD") {
      return sourceHref("kumaransg-lld", pathInsideClone);
    }
    return sourceHref("lld-atlas", segments.slice(referenceIndex));
  }

  const problemRoot = segments.indexOf("Problems");
  if (problemRoot >= 0 && !segments[problemRoot + 1]) return "#questions";
  const linkedProblem = problemSlugs.find((slug) => segments.includes(slug));
  if (linkedProblem) return `#question/${linkedProblem}`;
  if (problemRoot >= 0 && segments[problemRoot + 1]) {
    return `#question/${segments[problemRoot + 1]}`;
  }

  const patternRoot = segments.indexOf("Patterns");
  if (patternRoot >= 0) {
    const category = segments[patternRoot + 1];
    const pattern = segments[patternRoot + 2];
    if (category && pattern && ["creational", "structural", "behavioral"].includes(category)) {
      return `#pattern/${category}_${pattern}`;
    }
    return "#patterns";
  }

  const principleRoot = segments.indexOf("Principles");
  if (principleRoot >= 0) {
    const principle = segments[principleRoot + 1]?.replace(/\.md$/i, "");
    if (!principle || /^readme$/i.test(principle)) return "#principles";
    return principle === "other"
      ? "#principle/complementary_principles"
      : `#principle/${principle}`;
  }

  const lessonByFile: Readonly<Record<string, string>> = {
    "senior_staff_lld_interview.md": "senior_staff_lld_interview",
    "object_oriented_programming.md": "object_oriented_programming",
    "class_relationships.md": "class_relationships",
    "clean_code.md": "clean_code",
    "testing.md": "testing_lld",
  };
  const foundationRoot = segments.indexOf("Foundations");
  if (foundationRoot >= 0) {
    const lesson = lessonByFile[segments[foundationRoot + 1] ?? ""];
    return lesson ? `#lesson/${lesson}` : "#learning";
  }
  const concurrencyLeaf = segments.at(-1);
  if (
    concurrencyLeaf === "JAVA_MULTITHREADING_INTERVIEW_GUIDE.md" ||
    (concurrencyLeaf?.toLowerCase() === "readme.md" && segments.at(-2) === "interview")
  ) {
    return "#lesson/java_concurrency_lld_interviews";
  }
  if (concurrencyLeaf === "Introduction.md" && segments.at(-2) === "Multithreading") {
    return "#lesson/multithreading_introduction";
  }
  if (concurrencyLeaf?.toLowerCase() === "readme.md" && segments.at(-2) === "questions") {
    return "#lesson/concurrency_questions";
  }
  if (segments.includes("Concurrency")) return "#lesson/concurrency_fundamentals";
  if (segments.includes("UML")) return "#lesson/uml_for_lld";

  return null;
}
