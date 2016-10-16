TESTCASES := jq-test-all-ok.json jq-test-all-ng.json jq-test-official-ok.json jq-test-official-ng.json

.PHONY: all
all: $(TESTCASES)

$(TESTCASES): download.py exclude.lst
	python ./download.py

.PHONY: clean
clean:
	$(RM) -f $(TESTCASES)
