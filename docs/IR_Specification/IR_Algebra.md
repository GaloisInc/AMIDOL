# AMIDOL Intermediate Representation
## Formal definition and terminology

**Definition 1.*** The AIR defines models as a tuple M = (S, E, L, Phi, Lambda, Delta) where:
* S is a finite set of state variables {s(0), s(1), ..., s(n-1)} that take on values in N.
* E is a finite set of events {e(0), e(1), ..., e(m-1)} that may occur in the model.
* L: S|E -> N is the event and state variable labeling function that maps elements of S and E into the original ontology.
* Phi: E x N(0) x N(1) x ... x N(n-1) -> {0, 1} is the event enabling predicate.
* Lambda: E x N(0) x N(1) x ... x N(n-1) -> R+ is the transition rate specification.
* Delta: E x N(0) x N(1) x ... x N(n-1) -> N(0) x N(1) x ... x N(n-1) is the state variable transition function specification.

Informally the IR represents models defined in a given VDSOL using an formalism based on Generalized Stochastic Petri-nets with inhibitor arcs (which have the result of making Petri-nets Turing complete). Instead of inhibitor arcs, we utilize the more intuitive and performable method of allowing events to have input predicates (Phi) which can be evaluated to determine if an event is enabled, and output predicates which define the side effects of event firing.

Intuitively, N(0) x N(1) x ... x N(n-1) represents the *marking* of a model in the AIR.  It is the set of values the state variables take on in any given model state.  We typically do not represent the entire marking when giving the enabling conditions, transition rate, or state variable transition function, and instead only indicate the cases for which the event is enabled, i.e. define the conditions under which Phi(e(i)) == TRUE, providing expressions for those variables whose values impact evaluation, and omitting don't-cares.

```
  Phi: e(i) = True # Always enabled
  Phi: e(i) x (s(0) > 5) = True # Enabled when s(0) is > 5
```

The transition rate specification maps each event, and a marking to a rate, a positive real value which initially represents the exponential rate at which the function fires.  This assumes the model is Markovian **we will relax this assumption in the future**.

The state variable transition function maps events, and markings to new markings.  It is more compactly represented as a mapping of events and a tuple of expressions which define a set of markings, to a tuple of expressions which indicate how to update the marking.

```
  Delta: e(1) = (s0 = s0 - 1, s1 = s1 + 1) # When e(1) fires, decrement s0, increment s1.
  Delta: e(2) x (s0 > 1) = (s0 = s0 + 1)   # When e(2) fires, if s0 > 1, increment s0
  Delta: e(2) x (s0 <= 1) = (s1 = s1 + 1)  #        else, increment s1
```

## Example AIR Schema

```json
{ "irModel": {
  "irModelName": "string",
  "stateVariables": [{
    "name": "string",
    "label": "string",
    "type": "sv_type",
    "initial_value": "expression"
  }],
  "events": [{
    "name": "string",
    "label": "string",
    "rate": "expression",
    "input_predicate": {
      "enabling_condition": "expression"
    },
    "output_predicate": {
      "transition_function": ["lvalue = expression", "lvalue = expression", ...]
    }
  }],
  "constants": [{
    "name": "string",
    "value": "extern"|"expression"
  }],
  "expressions": [{
    "name": "string",
    "value": "extern"|"expression"
  }],
  "rateRewards": [{
    "name": "string",
    "variable": "string",
    "temporaltype": "instantoftime"|"intervaloftime"|"timeaveragedintervaloftime"|"steadystate",
    "samplingpoints": [{
      "time": "float"
      }]    
  }],
  "impulseRewards": [{
    "name": "string",
    "event": "string",
    "temporaltype": "instantoftime"|"intervaloftime"|"timeaveragedintervaloftime"|"steadystate",
    "samplingpoints": [{
      "time": "float"
      }]    
  }],
  "composedrewards": [{
    "name": "string",
    "expression": "expression"
  }]
 }}
 ```
