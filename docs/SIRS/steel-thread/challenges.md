# Identified Challenges

## Constants
* Needed to add constants to AIR model definitions.  This is required so we can give rates in terms of state variables/constants defined in other models, such as "total population".
* **Todo**: the UI/UX needs to support constant definition.  As currently defined, constants can be defined in a model as either an expression or "extern" to indicate they will be defined externally.
  * Do we need to have "extern"?  We should settle on a common method for overriding definitions in a model.  This should be part of our language definition.

## Naming conventions
* We need a way to procedurally name ids within a model, as composition leads to collisions.
  * **Proposal**: Since the AIR is hierarchical, use a hierarchical system:

```json
{"model": {
 "id": "A",
 "statevariables": [{"id": "X", "label": "A", "type": "float", "initial_value": "0"}],
 "events": [],
 "contants": [],
 "raterewards": [],
 "impulserewards": [],
 "composedrewards": []
}}
{"model": {
 "id": "B",
 "statevariables": [{"id": "Y", "label": "A", "type": "float", "initial_value": "0"}],
 "events": [],
 "contants": [],
 "raterewards": [],
 "impulserewards": [],
 "composedrewards": []
}}
```

Call `A`'s state variable `X`: `A.X`, and `B`'s state variable `Y`: `B.Y`.

Given a noun:

```json
{ "noun": {
  "id": "Noun",
  "model": "A.air",
  "icon": "A.svg",
  "inputvariables": ["X"],
  "outputvariables": ["X"],
  "parameters": [{"id": "X", "val": "0"}]
}}
```

Force the user to set the "id" of the noun to a unique identifier when they create an instance of "Noun".  If the user places two such nouns with identifiers "Q" and "R", then we can refer to their state variables as `Q.A.X` and `R.A.X`.

* **Question**: Since each noun has a single model associated with it, should we allow `Q.X` to be used instead of `Q.A.X`?
* My current thought on this is "no" for the purpose of simplicity of the initial prototype.
