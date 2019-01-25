TEX = pdflatex
BIB = bibtex
PDF = AMIDOL_MWS_2019.pdf
SRC = AMIDOL_MWS_2019
BDB = AMIDOL-MWS.bib

all : $(PDF)

$(PDF): $(SRC).tex $(BDB)
	$(TEX) $(SRC).tex
	$(TEX) $(SRC).tex
	$(BIB) $(SRC)
	$(TEX) $(SRC).tex

clean:
	rm $(PDF) $(SRC).aux $(SRC).bbl $(SRC).blg $(SRC).log $(SRC).out
