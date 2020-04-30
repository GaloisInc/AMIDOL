
# Generate the equations for an SEIR model with two infects and cross-immunity
# See DOI: 10.1126/science.abb5793
# NOTE: time unit is days

states = ["S","E","I","R"]
chi12 = "X₁₂"
chi21 = "X₂₁"
rho1 = "σ₁"
rho2 = "σ₂"
beta = "[beta]" #  "\\beta"
nu = "\\nu"
mu = "\\mu"
gamma = "\\gamma"
n = "N"

def state(x1,x2):
    return x1 + "₁" + x2 + "₂"

# Initialize all the states to nothing
derivatives = { state(x1,x2): [] for x1 in states for x2 in states }

# Add in death for all states
for x1 in states:
    for x2 in states:
        x1x2 = state(x1,x2)
        derivatives[x1x2].append("- "+mu+" "+x1x2)

# Add in birth for healthy people
healthy = state("S","S")
derivatives[healthy].append(mu+" "+n)

# Infection along strain 2
i2 = "(" + ' + '.join([ state(x1, "I") for x1 in states ]) + ")"
for x1 in states:
    x1s2 = state(x1,"S")
    x1e2 = state(x1,"E")
    x1i2 = state(x1,"I")
    x1r2 = state(x1,"R")

    # Exposed
    crossImmunity = "" if x1 == "S" else "(1 - "+chi12+") "
    exposureRate = crossImmunity+beta+" \\frac{"+x1s2+" "+i2+"}{"+n+"}"
    derivatives[x1s2].append("- " + exposureRate)
    derivatives[x1e2].append(exposureRate)

    # Infected
    infectionRate = nu+" "+x1e2
    derivatives[x1e2].append("- " + infectionRate)
    derivatives[x1i2].append(infectionRate)

    # Recovery
    recoveryRate = gamma+" "+x1i2
    derivatives[x1i2].append("- " + recoveryRate)
    derivatives[x1r2].append(recoveryRate)

    # Loss of immunity
    immunityLossRate = rho2+" "+x1r2
    derivatives[x1r2].append("- " + immunityLossRate)
    derivatives[x1s2].append(immunityLossRate)


# Infection along strain 1
i1 = "(" + ' + '.join([ state("I", x2) for x2 in states ]) + ")"
for x2 in states:
    s1x2 = state("S",x2)
    e1x2 = state("E",x2)
    i1x2 = state("I",x2)
    r1x2 = state("R",x2)

    # Exposed
    crossImmunity = "" if x2 == "S" else "(1 - "+chi21+") "
    exposureRate = crossImmunity+beta+" \\frac{"+s1x2+" "+i1+"}{"+n+"}"
    derivatives[s1x2].append("- " + exposureRate)
    derivatives[e1x2].append(exposureRate)

    # Infected
    infectionRate = nu+" "+e1x2
    derivatives[e1x2].append("- " + infectionRate)
    derivatives[i1x2].append(infectionRate)

    # Recovery
    recoveryRate = gamma+" "+i1x2
    derivatives[i1x2].append("- " + recoveryRate)
    derivatives[r1x2].append(recoveryRate)

    # Loss of immunity
    immunityLossRate = rho2+" "+r1x2
    derivatives[r1x2].append("- " + immunityLossRate)
    derivatives[s1x2].append(immunityLossRate)

# Print out the derivatives
for k,v in derivatives.items():
    v1 = map(lambda d: " " + d if d[0] == "-" else " + " + d, v[1:])
    print("\\frac{d " + k + "}{dt} = " + v[0] + ''.join(v1))

# Print out constants
print()
print(chi12+" = 0.74")
print(chi21+" = 0.50")
print(rho1+" = " + str(1 / (40 * 7)))
print(rho2+" = " + str(1 / (38 * 7)))
# print(beta+" = 2")
print(nu+" = " + str(1 / 5))
print(mu+" = " + str(1 / (80 * 365)))
print(gamma+" = " + str(1 / 4.9))

# Print out initial conditions
print()
print(n+" = 100")
print(state("S","S")+"_0 = 98")
print(state("S","I")+"_0 = 1")
print(state("I","S")+"_0 = 1")

