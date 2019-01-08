# Documentation for AMIDOL Project

## ODE Solution
* Sundials from LLNL is one of the most respected ODE solvers.
  * https://computation.llnl.gov/projects/sundials/sundials-software
  * Supported by many major languages: [Stan](https://github.com/stan-dev/math/), [Python](https://pypi.python.org/pypi/Assimulo), [Scikit](https://github.com/bmcage/odes), [Julia](https://github.com/JuliaDiffEq/DifferentialEquations.jl)

## ODE Representation, CTMC <--> ODE
* [Ward Whitt "CONTINUOUS-TIME MARKOV CHAINS"](refs/CTMCnotes120413.pdf)
* [Fundamentals of Compartmental Kinetics](refs/Compartmental-Kinetics.pdf)

## Why we need predicates
Inhibitor arcs are necessary to give SPNs turing complete capabilities.  Input predicates are a generalized case of inhibitor arcs, that are easier and more compact to specify.
* [Gianfranco Balbo "Introduction to Stochastic Petri Nets"](refs/GSPN-balbo.pdf)
