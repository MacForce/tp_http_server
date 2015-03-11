# Options:
# -p - running port
# -c - cores count
# -r - path to httptest (by default: "./files")
#
# non-blocking mode
java -jar non-blocking-tp-http-server-with-dependencies.jar -p 8080 -c 2 -r "./files"
#
# blocking mode
# java -jar blocking-tp-http-server-with-dependencies.jar -p 8080 -c 2 -r "./files"