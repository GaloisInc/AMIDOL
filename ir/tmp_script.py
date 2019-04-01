
from scipy.integrate import odeint
import json
import numpy as np
import matplotlib.pyplot as plt

# This is so that we can call "json.dumps" on Numpy arrays
class NumpyEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, np.ndarray):
            return obj.tolist()
        return json.JSONEncoder.default(self, obj)

# User defined constants
Example = 0.0

# The ODE system
def deriv_(y_, t_):
    Recovered, Susceptible, Infected = y_
    dInfected_ = 0.0 - 0.333 * Infected + 0.413 * Susceptible * Infected / (Susceptible + Infected + Recovered)
    dRecovered_ = 0.0 + 0.333 * Infected
    dSusceptible_ = 0.0 - 0.413 * Susceptible * Infected / (Susceptible + Infected + Recovered)
    return dRecovered_, dSusceptible_, dInfected_

# Boundary conditions and setup
timeRange_ = [ 0.0, 5.0, 10.0, 15.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0, 50.0, 55.0, 60.0, 65.0, 70.0, 75.0, 80.0, 85.0, 90.0, 95.0, 100.0, 105.0, 110.0, 115.0, 120.0, 125.0, 130.0, 135.0, 140.0, 145.0, 150.0, 155.0, 160.0, 165.0, 170.0, 175.0, 180.0, 185.0, 190.0, 195.0, 200.0, 205.0, 210.0, 215.0, 220.0, 225.0, 230.0, 235.0, 240.0, 245.0, 250.0, 255.0, 260.0, 265.0, 270.0, 275.0, 280.0, 285.0, 290.0, 295.0, 300.0, 305.0, 310.0, 315.0, 320.0, 325.0, 330.0, 335.0, 340.0, 345.0, 350.0, 355.0, 360.0, 365.0, 370.0, 375.0, 380.0, 385.0, 390.0, 395.0, 400.0, 405.0, 410.0, 415.0, 420.0, 425.0, 430.0, 435.0, 440.0, 445.0, 450.0, 455.0, 460.0, 465.0, 470.0, 475.0, 480.0, 485.0, 490.0, 495.0 ]
y0_ = 0.0, 5.1999999E7, 1.0
output = odeint(deriv_, y0_, timeRange_).T



print(json.dumps(output, cls=NumpyEncoder))
