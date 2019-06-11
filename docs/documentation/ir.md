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

### State Variables

Intuitively, state-variables make up the current state of the model, and measure the configuration and capabilities of all modeled components.  While state variables are defined as taking on values in $$\mathbb{N}$$, this does not restrict them from representing real numbers to arbitrary precision in modern computer hardware.  In practice, they are implemented as integers, and floating point numbers by the AMIDOL source code.



### Events



### Input Predicates

### Output Predicates

### Labels

### Reward Variables

## Practical Considerations

## Transformations

## Turing Completeness

## Compactness of Representation

## Examples
