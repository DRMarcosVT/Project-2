A specification turns your scope into a blueprint detailed enough that a teammate, or a coding assistant, could build from. It should answer each of the following:

Class design — your generic LinkedChain plus the classes that use it. One job per class — the LinkedChain owns storage, not application logic.
System diagram — required. A single picture showing the main pieces of your system and how they connect: which parts talk to which, and what moves between them. It does not have to be UML or any formal notation — boxes and arrows are fine, and a photo of a legible whiteboard or hand sketch counts. The test is whether a reader can see the shape of your system at a glance without reading the rest of the spec. Show where the LinkedChain sits and what flows into and out of it.
The generic contract — declare your type parameter and any bound. What does LinkedChain<T> promise about T, and which operations are type-safe?
Data & state — what backs the LinkedChain (your node class, head/tail references, how size is tracked?), and for each class, the fields it holds and their types.
Method signatures — the public API of your LinkedChain (add, remove, contains, count, size, …) along with any modifications you made to it and of your app classes: name, parameters and types, return type, and a one-line behavior. This is the contract a teammate or GenAI builds against. Include a short, estimated Big-O time complexity for each method. It is important to consider these constraints before implementing methods.
Where validation lives — take each bad-input case from your scope: which class/method catches it, and what happens (re-prompt, exception, default)?
Test plan — for each public method, one normal case and one bad-input case: input and expected result, in plain English.
Revised scope — what changed since your scope doc and why, and where each change originated (group discussion, GenAI, your own reflection)?
Work Attribution (REQUIRED) — every section of this document must be attributed. Include a table with one row per section listing who worked on it and how (drafted, revised, reviewed, diagrammed, GenAI-assisted, …). Additionally, note the way in which you used GenAI, if at all. Unattributed sections are treated as incomplete and group members not contributing substantively to at least one section will receive a 50% deduction on their group's deliverable.
Section	Team member(s)	Contribution (how)
Class design	A. Student	Drafted; revised after group review
System diagram	B. Student	Sketched on whiteboard, photographed
Method signatures	A. & C. Student	Drafted with GenAI; Big-O verified by hand
…	…	…
Meets — the LinkedChain and every class have one clear responsibility; a system diagram shows where the LinkedChain sits and what flows through it; the generic contract is stated; data and state are defined; public methods are specified by signature and behavior; every named bad-input case maps to a response; some initial thought is put into what the Big-O time complexity for each operation will be; the test plan covers normal and bad input; changes from your scope are explained; every section is attributed in a contribution table; and the GenAI reflection states specifically how (or that) GenAI was used.

Submit
A 5–10 page document covering each item above, with the system diagram embedded or attached. The attribution table and GenAI reflection are required but do not count toward the page range. Lengths will differ by project. Aim for completeness and consistency rather than a fixed page count per section.


Rubric (requirements for full marks):

Class desing
-lists the generic linkedchain and the app classes that use it
-each class has one clear responsibility
-the linkedchain owns storage, not application logic
-explains why responsibilities were split among members this way, or names an alternative that was considered and rejected

System diagram
-a single legible diagram is embedded or attached
-shows the main pieces and how they connect
-shows where the linkedchain sits and what flows into and out of it
-arrows are labeled with the data or class that move between pieces
-names in the diagram match the classes and methods in the rest of the spec

Generic contract
-declares the type parameter and any bound
states what linkedchain<T> promises about T
-identifies which operations are type safe
-justifies the bound (or lack thereof) using the element types the app will store
-the method signatures in the spec enforce the contract (noo raw types or casts)

Method singatures and Big-O
-linkedchain public API gives name, parameters and types, return type, one line behaviour
-app class public methods are specified the same way
-modifications to the provided linkedchain are noted
-each method has an estimated big-o
-each big-o etimate has a brief justification
-behaviours state what happens in edge cases (empty chain, item not found)

Where validation lives
-every bad input case from the scope is listed
-each case names the class/method that catches it
-each case names the response (re-prompt, exception, or default)
-explains why each check lives in that class rather than elsewhere
-names the specific exception type, re-prompt, message, or default value

Test Plan
-one normal case and one bad input case per public method
-each case gives the input and expected result in plain english
-plan covers an empty collection
-plan covers removing something that isnt there

Revised Scope
-a revised scope section is present, describes that changed since the scope doc, explains why each change was made
-says where each change originated (group discussion, genAI, or won reflection)
