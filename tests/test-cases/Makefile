TESTCASES := jq-1.5.yaml jq-1.5-manual.yaml jq-1.5-onig.yaml jq-1.6.yaml jq-1.6-manual.yaml jq-1.6-onig.yaml

.PHONY: all
all: $(TESTCASES)

$(TESTCASES): download.py exclude.lst
	python ./download.py

.PHONY: clean
clean:
	$(RM) -f $(TESTCASES)
