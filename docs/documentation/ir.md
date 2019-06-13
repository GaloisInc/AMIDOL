---
layout: post
title: AMIDOL IR
toc: true
---

## Formal Definition

Formally, the IR is a 5-tuple, $$(S, E, L, \Phi, \Lambda, \Delta)$$ where:
* $$S$$ is a finite set of state variables $$\{s_0, s_1, \ldots, s_{n-1}\}$$ that take on values in $$\mathbb{N}$$.
* $$E$$ is a finite set of events $$\{e_0, e_1, \ldots, e_{m-1}\}$$ that may occur in the model.
* $$L: S \mid E \rightarrow \mathbb{N}$$ is the event and state variable labeling function that maps elements of $$S$$ and $$E$$ into the original ontology.
* $$\Phi: E \times N_0 \times N_1 \times \ldots \times N_{n-1} \rightarrow \{0, 1\}$$ is the event enabling predicate.
* $$\Lambda: E \times N_0 \times N_1 \times \ldots \times N_{n-1} \rightarrow (0, \infty)$$ is the transition rate specification.
* $$\Delta: E \times N_0 \times N_1 \times \ldots \times N_{n-1} \rightarrow N_0 \times N_1 \times \ldots \times N_{n-1}$$ is the state variable transition function specification.

Informally the IR represents a model using a universal and Turing-complete mathematical language using a formalism based on Generalized Stochastic Petri-nets with inhibitor arcs. Instead of inhibitor arcs, we utilize the more intuitive and compact method of allowing events to have input predicates $$\Phi$$ which can be evaluated to determine if an event is enabled, and output predicates $$\Delta$$ which define the side effects of event firing.

Intuitively, the *marking* of a model in the IR given as $$N_0 \times N_1 \times \ldots \times N_{n-1}$$ is the set values of each state variable.  We typically do not represent the entire marking when giving the definition of enabling conditions (input predicates), transition rates associated with an event, or state variable transition function (output predicates), and instead only indicate the markings on which an given input predicate depends, and those state variables whose values change when an event fires due to the output predicate, omitting the "don't care" values.

### State Variables

State variables are defined in the IR as objects which have a name, a set of semantic labels, a state variable type (such as int or float), and an initial value given as an expression.  

```json
{ "state_variable": {
    "name": "string",
    "labels": ["string"],
    "type": "sv_type",
    "initial_value": "expression"
  }
}
```

Intuitively, the set of state variables in a model define the current state.  While state variables are defined as taking on values in $$\mathbb{N}$$ in the formal definition, this does not restrict them from representing real numbers to arbitrary precision in modern computer hardware.  In practice, they are implemented as integers, and floating point numbers by solvers.

### Events

Events are defined in the IR as objects which have a name, a set of semantic labels, a rate given as an expression, and an input predicate and output predicate, defined either as objects, or undefined.  In the cases where the input predicate is undefined, it is interpreted as being always true.  In the case where the output predicate is undefined, the event is interpreted as having no effect on the state of the model when it fires.

```json
{ "event": {
    "name": "string",
    "labels": ["string"],
    "rate": "expression",
    "input_predicate": {},
    "output_predicate": {},
  }
}
```

Events can be interpreted in two ways: discrete and continuous.  In both cases events define the ways in which a model can change state by altering the value of state variables.  In the discrete case, events fire at discrete times defined by their rates, changing the value of the model at those times as defined by their output predicates.  In the continuous case, events define flow relations which alter the state in proportion to their output predicates scaled by their rates.

Currently in AMIDOL, all rates are considered exponential.  AMIDOL can be extended at a later date to account for general distributions.

### Input Predicates

```json
{ "input_predicate": {
    "enabling_condition": "expression"
  }
}
```

### Output Predicates

The transition function is specified as a partially defined state change vector

```json
{ "output_predicate": {
    "transition_function": [{"sv_name string" : "expression"}]
  }
}
```

### Labels

### Reward Variables

```json
{ "rate_reward": {
    "name": "string",
    "sv_name": "sv_name string",
    "temporal_type": "instant_of_time"|"interval_of_time"|"time_averaged_interval_of_time"|"steady_state",
    "temporal_domain": ["float"]
  }  
}
```

```json
{ "impulse_reward": {
    "name": "string",
    "ev_name": "ev_name string",
    "temporal_type": "instant_of_time"|"interval_of_time"|"time_averaged_interval_of_time"|"steady_state",
    "temporal_domain": ["float"]
  }
}
```

```json
{ "composed_reward": {
    "name": "string",
    "expression": "expression"
  }
}
```

### Constants

```json
{ "constant": {
    "name": "string",
    "value": "expression"
  }
}
```

## Expressions

## Practical Considerations

## Transformations

## Turing Completeness

## Compactness of Representation

## Examples
