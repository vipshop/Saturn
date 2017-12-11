#!/bin/bash

RESTART()
{
    echo -e "Hebe! Hebe! Hebe!\c"
}

DUMP()
{
    echo -e "Dump! Dump! Dump!\c"
}

CMD="$1"
case "$CMD" in
  restart) RESTART;;
  dump) DUMP;;
esac
