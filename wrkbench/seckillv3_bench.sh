#!/bin/bash

wrk -t8 -c200 -d30s -T1s --script=sec_kill_v3.lua --latency  "http://127.0.0.1:8080/sec_kill/v3/sec_kill"