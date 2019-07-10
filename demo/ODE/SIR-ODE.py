from scipy.integrate import odeint
import numpy as np
import matplotlib.pyplot as plt

""" SIR Model implemented as a solution to a system of ODEs.

State Variables: S, I, R
    S - Susceptible population
    I - Infected population
    R - Recovered population

Events: Infection, Recovery
"""

# float: Initial value of S
S0 = 500.0
# float: Initial value of I
I0 = 10.0
# float: Initial value of R
R0 = 0.0


# float: Rate of recovery from an infection
gamma = 1.0/3.0
# float: Basic reproduction Number
rho = 2.0
# float: Rate of infection
beta = rho*gamma

# We define our measures over S, I, and R; each as instant of time variables collected at intervals
# of 1.0 over the range [0.0, 100.0]
Measures = [("n_S", [0.0, 100.0, 1.0]), ("n_I", [0.0, 100.0, 1.0]), ("n_R", [0.0, 100.0, 1.0])]

def deriv(y, t, beta, gamma):
    """ derivitive of the SIR system of ordinary differential equations
    Args:
        y ((float, float, float)): Current values of S, I, and R state variables
        t (float array): Time series to evaluate the derivitive
        beta (float): beta parameter for the infection rate
        gamma (float): gamma parameter for the recovery rate
    """

    # Set the current values of S, I, and R
    S, I, R = y

    # Delta S
    dSdt = -beta * S * I / (S+I+R)
    # Delta I
    dIdt = beta * S * I / (S+I+R) - gamma * I
    # Delta R
    dRdt = gamma * I
    return dSdt, dIdt, dRdt

# float array: Initial conditions for S, I, and R
y0 = S0, I0, R0

# Evaluate from 0 to 100
min = 0
max = 100

# Points to evaluate for measures
t = list(np.arange(min, max, 1.0))

# Solve the model using the odeint class from scipy.integrate
ret = odeint(deriv, y0, t, args=(beta, gamma))
S, I, R = ret.T

fig, ax = plt.subplots(figsize=(20, 10))
ax.plot(t, S, label="S")
ax.plot(t, I, label="I")
ax.plot(t, R, label="R")
ax.legend()
fig.savefig("Python-ODE.pdf", bbox_inches='tight')
