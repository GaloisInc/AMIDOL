import math
import json

a = 0.66
b = 1.4
phi = -3.8 * 7
gamma = 1 / 4.9

def R0(t):
    return a/2 * math.cos((math.pi * 2) / 365 * (t - phi)) + (a/2 + b)

def beta(t):
    return gamma * R0(t)


# Write out a JSON file containing the data trace that is `beta`'s evolution
# over 5 years, per day
with open('beta.json', 'w') as outfile:
    timeRange = range(0, 5 * 365) # 5 years
    data = {
        'time': list(timeRange),
        'data': [ beta(t) for t in timeRange ]
    }
    json.dump(data, outfile)

