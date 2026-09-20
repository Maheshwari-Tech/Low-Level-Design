import type { Metadata } from "next";
import { CatalogExplorer } from "./components/CatalogExplorer";
import { KnowledgeExplorer } from "./components/KnowledgeExplorer";
import { SolutionExplorer } from "./components/SolutionExplorer";
import { SourceLibrary } from "./components/SourceLibrary";
import { contentIndex } from "./content.generated";

export const metadata: Metadata = {
  title: "Learn Low-Level Design — Questions, Patterns & Code",
  description: "A guided low-level design learning platform with Senior/Staff interview answers, runnable Java and Python, SOLID, design patterns, concurrency, UML, and revision-pinned provenance.",
};

export default function Home() {
  const totalInterviewVariations = contentIndex.featured.reduce(
    (sum, item) => sum + item.variationCount,
    0,
  );
  const totalGuides = contentIndex.lessons.length + contentIndex.principles.length + contentIndex.patterns.length;
  const totalIndexedFiles = contentIndex.repositories.reduce((sum, repository) => sum + repository.fileCount, 0);

  return (
    <main>
      <header className="site-header">
        <a className="wordmark" href="#top" aria-label="LLD Atlas home">
          <span className="wordmark-mark">L/A</span>
          <span>LLD Atlas</span>
        </a>
        <nav aria-label="Primary navigation">
          <a href="#learning">Learn</a>
          <a href="#principles">Principles</a>
          <a href="#patterns">Patterns</a>
          <a href="#solutions">Solutions</a>
          <a href="#questions">Questions</a>
          <a href="#sources">Sources</a>
        </nav>
        <a className="header-cta" href="#learning">Start learning <span aria-hidden="true">↘</span></a>
      </header>

      <section className="hero" id="top">
        <div className="hero-copy">
          <p className="hero-kicker"><span /> Senior/Staff answers in Java + Python</p>
          <h1>Design systems from <em>invariants,</em> not boxes.</h1>
          <p className="hero-lede">
            Prepare for a one-hour Senior/Staff LLD round, learn the foundations, then apply SOLID and design patterns across {contentIndex.catalog.length} interview questions and {totalInterviewVariations} source-backed variations in the eight deep-dive packs.
          </p>
          <div className="hero-actions">
            <a className="primary-button" href="#learning">Follow the learning path <span aria-hidden="true">→</span></a>
            <a className="text-button" href="#questions">Browse all questions</a>
          </div>
        </div>
        <div className="hero-blueprint" aria-label="Order workflow blueprint">
          <div className="blueprint-head">
            <span>FLOW / 01</span>
            <span className="blueprint-status"><i /> runnable</span>
          </div>
          <div className="blueprint-canvas">
            <div className="blueprint-node node-command"><small>01</small><strong>Command</strong><span>ConfirmOrder</span></div>
            <span className="blueprint-line line-one" aria-hidden="true">→</span>
            <div className="blueprint-node node-service"><small>02</small><strong>Service</strong><span>orchestrate()</span></div>
            <span className="blueprint-line line-two" aria-hidden="true">→</span>
            <div className="blueprint-node node-aggregate"><small>03</small><strong>Aggregate</strong><span>protect invariants</span></div>
            <div className="blueprint-annotation">atomic boundary</div>
            <div className="blueprint-note">Explicit states<br />Idempotent edges<br />Injected dependencies</div>
          </div>
          <div className="blueprint-foot"><span>requirements</span><span>model</span><span>failure path</span><span>code</span></div>
        </div>
      </section>

      <section className="stat-strip" aria-label="Repository statistics">
        <div><strong>{contentIndex.catalog.length}</strong><span>canonical questions</span></div>
        <div><strong>{totalInterviewVariations}</strong><span>featured interview variations</span></div>
        <div><strong>{totalGuides}</strong><span>structured learning guides</span></div>
        <div><strong>{totalIndexedFiles}</strong><span>indexed source files</span></div>
      </section>

      <section className="section learning-section" id="learning">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Guided learning path</p>
            <h2>Build the vocabulary before the system.</h2>
          </div>
          <p>Start with the one-hour interview playbook, then build object-modeling judgment, learn to test invariants, practice concurrency, and choose the UML view that communicates the design.</p>
        </div>
        <KnowledgeExplorer items={contentIndex.lessons} kind="lesson" />
      </section>

      <section className="section principles-section" id="principles">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Principles, by design pressure</p>
            <h2>Know why the boundary moves.</h2>
          </div>
          <p>Open every SOLID principle and complementary heuristic for its intent, mechanics, failure signals, trade-offs, and local Java examples.</p>
        </div>
        <KnowledgeExplorer items={contentIndex.principles} kind="principle" />
      </section>

      <section className="section patterns-section" id="patterns">
        <div className="section-heading">
          <div>
            <p className="eyebrow">The complete GoF catalog</p>
            <h2>Patterns are decisions, not decorations.</h2>
          </div>
          <p>Explore all 23 creational, structural, and behavioral patterns with crisp explanations, trade-offs, and every available source variation.</p>
        </div>
        <KnowledgeExplorer items={contentIndex.patterns} kind="pattern" />
      </section>

      <section className="section method-section">
        <div className="method-copy">
          <p className="eyebrow">A repeatable method</p>
          <h2>Start with what must remain true.</h2>
          <p>Patterns are supporting actors. The problem boundary, lifecycle, and competing commands determine the design.</p>
        </div>
        <ol className="method-list">
          <li><span>01</span><div><strong>Frame</strong><p>Actors, scope, functional requirements, and explicit exclusions.</p></div></li>
          <li><span>02</span><div><strong>Protect</strong><p>Invariants, ownership, state transitions, and time boundaries.</p></div></li>
          <li><span>03</span><div><strong>Stress</strong><p>Retries, races, invalid commands, unknown outcomes, and compensation.</p></div></li>
          <li><span>04</span><div><strong>Prove</strong><p>A focused model, narrow ports, and deterministic executable scenarios.</p></div></li>
        </ol>
      </section>

      <section className="section solutions-section" id="solutions">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Eight Senior / Staff interview packs</p>
            <h2>One hour. Question, reasoning, proof.</h2>
          </div>
          <p>Each pack merges every distinct source-backed variation into its own interview question and expected Senior/Staff solution, then supplies SOLID reasoning, failure follow-ups, rubric, and runnable Java 17 plus one-hour Python core code.</p>
        </div>
        <SolutionExplorer
          features={contentIndex.featured}
          problemSlugs={contentIndex.catalog.map((problem) => problem.slug)}
        />
      </section>

      <section className="section questions-section" id="questions">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Complete problem bank</p>
            <h2>Find your next design.</h2>
          </div>
          <p>Open any question for its complete explanation, requirements, model, trade-offs, and source files where an implementation exists.</p>
        </div>
        <CatalogExplorer problems={contentIndex.catalog} />
      </section>

      <section className="section library-section" id="library">
        <div className="library-intro">
          <p className="eyebrow">The supporting library</p>
          <h2>More than a question list.</h2>
          <p>Build fundamentals, read all 23 GoF patterns, model with UML, and study concurrency before tackling the complete problem bank.</p>
        </div>
        <div className="library-grid">
          <a className="library-link-card" href="#lesson/senior_staff_lld_interview"><span>01</span><p>Interview playbook</p><strong>A complete 60-minute Senior/Staff answer method</strong><small>Start preparing <i aria-hidden="true">↗</i></small></a>
          <a className="library-link-card" href="#principles"><span>02</span><p>Principles</p><strong>SOLID with intent, mechanics, and trade-offs</strong><small>Open all guides <i aria-hidden="true">↗</i></small></a>
          <a className="library-link-card" href="#patterns"><span>03</span><p>Patterns</p><strong>All 23 GoF patterns with local examples</strong><small>Browse the catalog <i aria-hidden="true">↗</i></small></a>
          <a className="library-link-card" href="#lesson/concurrency_fundamentals"><span>04</span><p>Concurrency</p><strong>Memory, coordination, failure modes, exercises</strong><small>Open the lab <i aria-hidden="true">↗</i></small></a>
          <a className="library-link-card" href="#lesson/uml_for_lld"><span>05</span><p>UML</p><strong>Class, sequence, activity, state, use case</strong><small>Choose a diagram <i aria-hidden="true">↗</i></small></a>
          <a className="library-link-card upstream-card" href="#sources"><span>06</span><p>Variation provenance</p><strong>{totalIndexedFiles.toLocaleString()} files behind the merged interview solutions</strong><small>Inspect exact evidence <i aria-hidden="true">↗</i></small></a>
        </div>
      </section>

      <section className="section sources-section" id="sources">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Exact provenance behind every variation</p>
            <h2>The solution is merged. The evidence stays traceable.</h2>
          </div>
          <p>The eight interview packs contain the organized questions and expected answers. Use this archive only to inspect the exact revision-pinned prompt or implementation that informed a variation.</p>
        </div>
        <SourceLibrary repositories={contentIndex.repositories} />
      </section>

      <section className="closing-section">
        <p className="eyebrow">One repository. One map.</p>
        <h2>Read the question.<br />Challenge the design.<br /><em>Run the code.</em></h2>
        <a className="primary-button light" href="#solutions">Start with Order Management <span aria-hidden="true">↑</span></a>
      </section>

      <footer>
        <a className="wordmark" href="#top"><span className="wordmark-mark">L/A</span><span>LLD Atlas</span></a>
        <p>Structured low-level design notes with runnable Java and Python reference solutions.</p>
        <p>Pinned sources retain their exact attribution and license boundaries; two clones declare no repository-wide license.</p>
      </footer>
    </main>
  );
}
