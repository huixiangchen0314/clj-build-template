.PHONY: clean compile test jar uberjar repl

all: clean install

clean:
	clj -T:build clean

jar:
	clj -T:build jar

install: jar
	clj -T:build install
