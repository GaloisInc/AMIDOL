---
layout: post
title: AMIDOL Brainstorming
toc: true
---

## Some VDSOL Targets
* Stochastic Chemical

```
A + C -> C + D [alpha]
... etc ...
```

## Targets "between" the IR and VDSOL
* Equations - can we take an ODE of a compartmental model and synthesize graphical representations and IR?

Given the SIR model:

$$ \frac{dS}{dt} = -\frac{\beta IS}{N} $$

$$ \frac{dI}{dt} = \frac{\beta IS}{N} - \gamma I $$

$$ \frac{dR}{dt} = \gamma I $$

Some observations:
* Compartments are identified in the left hand side of the equations.  $$\frac{dX}{dt}$$ implies a compartment $$X$$ exists.
* Right hand sides are lists of additive events.  $$\frac{dX}{dt} = -\alpha X + \rho Y$$ gives us two transitions, one which removes from $$X$$ at rate $$-\alpha X$$ with predicate $$X \neq 0$$ and one which adds to $$X$$ at rate $$\rho Y$$ and predicate $$Y \neq 0$$.

## Agent Based Models

* Implement a good agent based modeling framework.
