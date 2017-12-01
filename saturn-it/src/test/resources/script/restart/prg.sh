#!/bin/bash

RESTART()
{
    echo -e "Hebe! Hebe! Hebe!\c"
}

CMD="$1"
case "$CMD" in
  restart) RESTART;;
esac
