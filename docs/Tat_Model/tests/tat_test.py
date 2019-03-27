import pandas as pd
from scipy.integrate import odeint
def params():
    def __init__(self):
        self.dtt = 0.154
        self.b = 0.01
        self.dr = 1.6
        self.ex = 2.6
        self.dp = 0.39
        self.v = 150
        self.k = 50
        self.vp = 11
        self.kp = 0.676

def deriv()

  d/dt (tat) = -dtt * tat
  d/dt (nRNA) = (b + v * tat) / (k + tat) - ex * nRNA - dr * nRNA
  d/dt (cRNA) = ex * nRNA - dr * cRNA
  d/dt (P) = vp * cRNA / (kp + cRNA) - dp * P
  RNA = nRNA + cRNA
