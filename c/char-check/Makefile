#
# This next line is for real:
ICUBLD=/usr/local/icu
# The next line is for test build:
# ICUBLD=/usr/local/icu-next

ICUINC=$(ICUBLD)/include
ICULIB=$(ICUBLD)/lib
ICULIBS=-L$(ICULIB) -licuuc -licui18n -licuio -licudata -Wl,-rpath -Wl,$(ICULIB)

all: char-check

char-check: $(ICUBLD) char-check.cpp
	$(CXX) -c -I $(ICUINC) char-check.cpp -o char-check.o
	$(CXX) -o char-check $(ICULIBS) char-check.o

$(ICUBLD) icu_rebuild:
	sh ./fetch-icu.sh
