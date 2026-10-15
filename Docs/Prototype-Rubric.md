# Project 2 – Deliverable 3 – Prototype + Tests

A running slice of the system, plus the tests that prove it behaves. This is new for P2: you ship something that works before you ship the final.

> **Note:** You were given `LinkedChain.java`, so this deliverable is not about building the data structure. It is about how well your application uses it. Each item below is one rubric point.

## Rubric Points

1. **Working application slice** — your app classes compile and run on the MVP path, with the LinkedChain doing the storage.
2. **The LinkedChain is reused** — your application stores and retrieves at least two different element types (the types you named in your scope) through the same generic LinkedChain class, with no copy-pasted, type-specific versions.
3. **Used through its public API** — your app classes work with the LinkedChain only by calling its methods (`add`, `remove`, `contains`, `count`, `size`, …), and application logic lives in your app classes, not in the LinkedChain.
4. **JUnit tests that run** — at least one normal case and one bad-input case per public method of your app classes, following your spec's test plan, and they pass.
5. **Bad input survives** — the prototype does not crash on the cases you named in your scope, and each one is caught where your specification said validation lives.
6. **Reflection** — in your `README.md`, under a "Reflection" heading, describe what changed between your specification and what you actually built. Your reflection should address:
   - **What changed from your specification?** Classes, method signatures, where validation lives, test cases — what did the spec say, what did you build instead, and why?
   - **What changed in the LinkedChain?** Anything you added to or modified in the provided class and why your application needed it, or that you used it unchanged.
   - **What happened to your unknowns?** Of the things you listed in your scope as not knowing how to do, which did you figure out, and which are still open?
   - **What doesn't work yet?** MVP features, bad-input cases, or tests that are missing or failing in this prototype.
7. **Next Steps** — in your `README.md`, under a "Next Steps" heading, lay out what you will do next to extend this prototype toward the final. Your plan should address:
   - **What is left to finish the MVP?** The remaining features and fixes, in the order you will do them.
   - **Which stretch goals will you attempt?** The ones from your scope you are committing to, and the ones you are dropping.
   - **What new tests will you need?** The methods and bad-input cases the added work must cover.
   - **Who does what?** Which team member owns each remaining item.
8. **Attribution** — in your `README.md`, a table with one row per class and test file listing who worked on it and how (wrote, revised, reviewed, tested, GenAI-assisted, …), plus a note on the way in which you used GenAI, if at all.

**Meets** — the application runs on the MVP path; one generic LinkedChain is reused for two-plus element types; app classes use it only through its public API; the JUnit tests run and pass with a normal and a bad-input case per public app method; the bad-input cases from your scope are handled without crashing; the README has a reflection on what changed in the implementation and a plan for what comes next; and every class and test file is attributed, with a note on how (or that) GenAI was used.

## Submit

Your source and tests together, with a `README.md` containing:

- how to compile and run the program and the tests,
- your "Reflection" section,
- your "Next Steps" section, and
- your attribution table and GenAI note.

The tests must actually execute — a test file that doesn't run counts as not present.

> 🤖 **Working with GenAI Tip!** GenAI is good at generating edge-case tests for the code that uses your LinkedChain — an empty collection, removing something that isn't there, input your app should reject. Ask for candidates, then read each one and decide whether it tests something real before you keep it. Tests you don't understand can't protect you in the grading session.

Due at the start of your lab session. Graded EMRN — aim for at least Meets. Bring your work to the grading session ready to defend it; treat it as an oral exam.

Full project context, learning objectives, and the EMRN table are on the *Project 2: Building for Reuse – Overview* page.

## Grading Table

Every criterion uses the same lower tiers: **Revisable** (7.5 pts) — missing one or more of the Meets criteria; **Not There Yet** (5 pts) — missing significant components; **Missing** (0 pts).

| Criterion | Meets (8.5 pts) | Exceeds (11 pts) — all of Meets plus |
|---|---|---|
| **Working Application Slice** /11 | ☐ App classes compile<br>☐ Runs on the MVP path<br>☐ The LinkedChain does the storage | ☐ At least one stretch goal from the scope already works |
| **LinkedChain Is Reused** /11 | ☐ At least two element types are stored and retrieved<br>☐ Both go through the same generic LinkedChain class, with no type-specific copies | ☐ More than two element types go through the same class |
| **Used Through Its Public API** /11 | ☐ App classes work with the LinkedChain only by calling its methods<br>☐ Application logic lives in the app classes, not in the LinkedChain | ☐ The team can state the Big-O of their app methods and whether it matches their spec estimates |
| **JUnit Tests** /11 | ☐ The tests execute<br>☐ One normal case and one bad-input case per public app method<br>☐ The tests pass | At least one of:<br>☐ Tests cover an empty collection<br>☐ Tests cover removing something that isn't there |
| **Bad Input Survives** /11 | ☐ No crash on the bad-input cases named in the scope<br>☐ Each case is caught where the spec said validation lives | ☐ Each case gets the response the spec named for it (re-prompt, exception, or default) |
| **Reflection** /11 | ☐ A "Reflection" section in `README.md`<br>☐ Describes what changed from the specification and why<br>☐ Touches on the other questions (changes to the LinkedChain, unknowns, what doesn't work yet) | At least one of:<br>☐ All four questions are answered with specifics (named classes, methods, tests, or unknowns)<br>☐ Says where each change originated (group discussion, GenAI, or own reflection) |
| **Next Steps** /11 | ☐ A "Next Steps" section in `README.md`<br>☐ Lists what is left to finish the MVP<br>☐ Touches on the other questions (stretch goals, new tests, who does what) | At least one of:<br>☐ Remaining work is ordered and every item has an owner<br>☐ Each stretch goal from the scope is marked as committed or dropped, and the new tests are named |
