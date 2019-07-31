---
layout: post
title: AMIDOL Ontology
toc: true
---

The AMIDOL Ontology system supports operations which modify the VDSOL palettes used to generate formulations, supporting extracted models with grounding, and automatically generated models with grounding.

## Definitions

* VDSOL - A visual domain specific ontological language
* Palette - a set of VDSOL objects
* VDSOL Object - A VDSOL Object has a type consisting of a label (such as noun or verb), an input cardinality, an output cardinality, a measure cardinality, and an IR definition.
* Formulation - a set of VDSOL objects from the same palette, with relational arcs, and defined measures.
* Annotated Ontology - An ontology (like SNOMED) with parent/child relationships, and annotated VDSOL template objects.
* VDSOL Template Object - A VDSOL Template Object is a partial definition of a VDSOL Object, consisting of all elements except an IR definition.  It defines a category to which all VDSOL objects that are compositionally compatible belong to.  VDSOL template objects also contain a rule for generating unique visual identifiers (such as coloration rules for a VDSOL icon, or name generation for VDSOL identifiers).

## User Story 1

* Extant VDSOL Palette consisting of:
  * Noun: Population
  * Noun: Infected Population
  * Verb: H1N1 infection
  * Verb: H1N1 recovery
* Annotated Ontology with templates consisting of
  * Population
  * Virus

* Extracted Model contains:
  * SIRS model with H3N2 grounding on new infection event.

```
(:block,
  (:function, (:call, :main, :β, :γ, :μ), (:block,
      (:macrocall, Symbol("@grounding"), nothing, (:block,
          (:call, :(=>), :S, (:call, :Noun, :Susceptible, (:kw, :ontology, :Snowmed))),
          (:call, :(=>), :I, (:call, :Noun, :Infectious, (:kw, :ontology, :ICD9))),
          (:call, :(=>), :R, (:call, :Noun, :Recovered, (:kw, :ontology, :ICD9))),
          (:call, :(=>), :λ₁, (:call, :Verb, :H3N2 infection)),
          (:call, :(=>), :λ₂, (:call, :Verb, :H3N2 recovery))
        )),
      (:macrocall, Symbol("@reaction"), nothing, (:tuple, (:block,
            (:tuple, :λ₁, (:call, :+, :S, (:->, :I, (:block,
                    (:call, :*, 2, :I)
                  )))),
            (:tuple, :λ₂, (:->, :I, (:block,
                  :R
                )))
          ), :λ₁, :λ₂)),
      (:(=), :Δ, (:vect, (:->, (:tuple, :S, :I), (:block,
              (:tuple, (:call, :-, :S, 1), (:call, :+, :I, 1))
            )), (:->, (:tuple, :I, :R), (:block,
              (:tuple, (:call, :-, :I, 1), (:call, :+, :R, 1))
            )))),
      (:(=), :ϕ, (:vect, (:->, (:tuple, :S, :I), (:block,
              (:&&, (:call, :>, :S, 0), (:call, :>, :I, 0))
            )), (:->, :I, (:block,
              (:call, :>, :I, 0)
            )))),
      (:(=), :Λ, (:vect, (:(=), (:call, :λ₁, :S, :I), (:block,
              (:call, :/, (:call, :*, :β, :S, :I), (:call, :+, :S, :I, :R))
            )), (:(=), (:call, :λ₂, :I), (:block,
              (:call, :*, :γ, :I)
            )))),
      (:(=), :m, (:call, (:., :Petri, (:quote, #QuoteNode
              :Model
            )), :g, :Δ, :ϕ, :Λ)),
      (:(=), :d, (:call, :convert, :ODEProblem, :m)),
      (:(=), :soln, (:call, :solve, :m)),
      (:(=), :soln, (:call, :solve, :d))
    ))
)
```

Work flow:
* We should be able to automatically:
  * Detect the "Susceptible" grounding
    * Pair with existing non-template model by checking type and IR equivalence.
  * Detect the "Infectious" grounding
    * Pair with existing non-template model by checking type and IR equivalence.
  * Detect the "Recovered" grounding
    * Pair with existing non-template model by checking type and IR equivalence.
  * Detect "H3N2 Infection" grounding
    * Fail to find existing non-template model for grounding.
    * Find Virus infection template model
    * Generate new H3N2 Infection model based on this grounding with appropriate types.
    * Embed grounded model elements in the new VDSOL object.
  * Detect "H3N2 Recovery" grounding
    * Fail to find existing non-template model for grounding.
    * Find Virus infection template model
    * Generate new H3N2 Recovery model based on this grounding with appropriate types.
    * Embed grounded model elements in the new VDSOL object.
  * What to do when there is no template.
  * Build an Ebola AST.

* Questions about the Ontology
  * Query SQLite database?  Or navigate Snomed?
