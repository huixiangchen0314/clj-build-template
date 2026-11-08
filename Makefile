# ==== Configurable variables ====
CLJ      ?= clj
BUILD    ?= -T:build
VERSION  ?= $(shell grep -oP '(?<=def version ")[^"]+' build.clj 2>/dev/null || echo unknown)

# ==== Phony targets ====
.PHONY: help clean copy pom jar install repl test deps version all

# Default target: show help
help:
	@echo "Available targets:"
	@echo "  make clean     Remove target/"
	@echo "  make copy      Copy sources to target/classes"
	@echo "  make pom       Generate POM file"
	@echo "  make jar       Build jar (including POM)"
	@echo "  make install   Install to local ~/.m2"
	@echo "  make test      Run tests (requires :test alias)"
	@echo "  make repl      Start REPL (requires :dev alias)"
	@echo "  make deps      Show dependency tree"
	@echo "  make version   Print current version"
	@echo "  make all       Same as make jar"
	@echo ""
	@echo "Current version: $(VERSION)"

# ==== Build targets ====

clean:
	$(CLJ) $(BUILD) clean

copy:
	$(CLJ) $(BUILD) copy-src

pom:
	$(CLJ) $(BUILD) pom

jar:
	$(CLJ) $(BUILD) jar

install:
	$(CLJ) $(BUILD) install

all: jar

# ==== Dev helpers ====

repl:
	$(CLJ) -M:dev

test:
	$(CLJ) -X:test

deps:
	$(CLJ) -Stree

version:
	@echo $(VERSION)