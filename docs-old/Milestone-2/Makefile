TEX = pdflatex
BIB = bibtex
PDF = AMIDOL_Milestone-2.pdf
SRC = AMIDOL_Milestone-2
BDB = AMIDOL-Milestone-2.bib

all : $(PDF)

$(PDF): $(SRC).tex $(BDB)
	$(TEX) $(SRC).tex
	$(TEX) $(SRC).tex
	$(BIB) $(SRC)
	$(TEX) $(SRC).tex
	$(TEX) $(SRC).tex

clean:
	rm $(PDF) $(SRC).aux $(SRC).bbl $(SRC).blg $(SRC).log $(SRC).out
