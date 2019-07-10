import math
import random
# From https://en.wikipedia.org/wiki/Gillespie_algorithm

### input parameters ####################

# int; total population
N = 501

# float; maximum elapsed time
T = 100.0

# float; start time
t = 0.0

# float; spatial parameter
V = float(N)

# float; rate of infection after contact
_alpha = 1.0/3.0

# float; rate of cure
_beta = 1.0/3.0 * 1.24

# int; initial infected population
n_I = 1

#########################################

# compute susceptible population, set recovered to zero
n_S = N - n_I
n_R = 0

# initialize results list
SIR_data = []
SIR_data.append((t, n_S, n_I, n_R))

# main loop
for runs in range(0,1000):
    t = 0.0
    n_S, nI, nR = (500, 1, 0)
    while t < T:

        if n_I == 0:
            break

        w1 = _alpha * n_S * n_I / V
        w2 = _beta * n_I
        W = w1 + w2

        dt = -math.log(random.uniform(0.0,1.0)) / W
        t = t + dt

        if random.uniform(0.0,1.0) < w1 / W:
            n_S = n_S - 1
            n_I = n_I + 1

        else:
            n_I = n_I - 1
            n_R = n_R + 1

        SIR_data.append((t, n_S, n_I, n_R))
