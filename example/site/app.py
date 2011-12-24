import sys

from flask import Flask

app = Flask(__name__)

@app.route("/")
def hello():
  return "<h1>Hello World!</h1>"

@app.route("/<name>")
def hello_name(name):
  return "<h1>Hello, %s!</h1>" % name

if __name__ == '__main__':
  app.run(port=int(sys.argv[1]))
