#!/bin/bash
function killPro() {
  name=${1}
  pid=$(ps ax | grep -i ${name} | grep java | grep -v grep | awk '{print $1}')
  if [ -n "$pid" ] ; then
          kill "${pid}"
          echo "Send shutdown request to ${name}(${pid}) OK"
  fi
}
killPro "gateway"
killPro "task"
