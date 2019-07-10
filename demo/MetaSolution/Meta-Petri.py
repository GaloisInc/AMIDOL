import numpy as np
import itertools
import math
from tqdm import tqdm_notebook
import pandas as pd
import matplotlib.pyplot as plt
import PetriNet

""" SIR Model implemented as a solution to a system of ODEs.

State Variables: S, I, R
    S - Susceptible population
    I - Infected population
    R - Recovered population

Events: Infection, Recovery, ReturnToSusceptibility
"""

# str array: Set of state variables S, I, and R
SV = ["S", "I", "R"]
# str array: Integer initial values for the state variables S, I, and R
SV0 = ["500", "10", "0"]
# str array: Set of constants, gamma, rho, mu, and beta
# gamma indicates the rate of recovery from an infection
# rho indicates the basic reproduction Number
# mu indicates the rate of return to susceptibility
# beta indicates the rate of infection
C = ["gamma", "rho", "mu", "beta"]
# str array: Expressions for the value of constants
CVals = ["1.0/3.0", "2.0", "0.0", "gamma*rho"]
# str array: Expressions for the enabling conditions of the three events:
# infect
# recover
# returnToSusceptibility
Phi = ["(S > 0) and (I > 0)", "I > 0", "R > 0"]
# str array: Lvalues and expressions for the transition functions for the three events
Delta = [("S = S - 1", "I = I + 1"), ("I = I - 1", "R = R + 1"), ("R = R - 1", "S = S + 1")]
# str array: Expressions for the rate functions for the three events
Lambda = ["beta * float(S) * float(I) / float(S+I+R)", "gamma * float(I)", "mu * float(R)"]
# str array: Names of measures, and float arrays describing their time domains as [start, end, step]
Measures = [("S", [0.0, 100.0, 1.0]), ("I", [0.0, 100.0, 1.0]), ("R", [0.0, 100.0, 1.0])]

# float: initial time of the simulation
t0 = 0.0
# float: Maximum time of the simulation
tMax = 100.0
# float: total number of trajectories to generate
runs = 1000

# Solve using the PetriNet metasolution library
results = PetriNet.metasolve(SV, SV0, C, CVals, Phi, Delta, Lambda, Measures, t0=t0, tMax=tMax, runs=runs)
fig, ax = plt.subplots(figsize=(20, 10))
for sv in SV:
    # float: negative and positive confidence intervals for E[S], E[I], and E[R]
    pCI = [x + y for x,y in zip(results['Mean ' + sv].tolist(), results['Confidence Interval ' + sv].tolist())]
    nCI = [x - y for x,y in zip(results['Mean ' + sv].tolist(), results['Confidence Interval ' + sv].tolist())]
    ax.plot(list(results.index), results['Mean ' + sv].tolist(), label=sv)
    ax.fill_between(list(results.index), nCI, pCI, alpha=.1)
    ax.legend()
fig.savefig("Python-Meta.pdf", bbox_inches='tight')
