TESTCASES := jq-test-all-ok.json jq-test-all-ng.json jq-test-manual-ok.json jq-test-manual-ng.json jq-test-onig-ok.json jq-test-onig-ng.json

.PHONY: all
all: $(TESTCASES)

$(TESTCASES): download.py exclude.lst
	python ./download.py

.PHONY: clean
clean:
	$(RM) -f $(TESTCASES)
