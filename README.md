## ElasticBand
Run non-java webapps on [Elastic
Beanstalk](http://aws.amazon.com/elasticbeanstalk/).

### WARNING!
This is alpha quality software. This project, for the moment, should be
considered nothing more than an exercise in yak shaving. Do not use this in a
production environment. That said...fiddling around is good.

### How it works
**ElasticBand** forks a process listening on a free HTTP port. All incoming
requests are then proxied to that process. Depending on how your `web.xml` is
set up, it will use a `Runtime` to determine how to set up that subprocess
correctly given the potentially stripped down AWS image your application has
started with.

For the moment, there is are `python` and `virtualenv` runtimes, but it wouldn't
take a monster effort to implement new ones.

### Python runtime
For an application that really only depends on Python, this runtime may be
enough. An example is as follows:

First, the simplest python web server Googling could pull up.

`main.py`

```python
import sys
import BaseHTTPServer
from SimpleHTTPServer import SimpleHTTPRequestHandler


HandlerClass = SimpleHTTPRequestHandler
ServerClass  = BaseHTTPServer.HTTPServer
Protocol     = "HTTP/1.0"

port = int(sys.argv[1])
server_address = ('127.0.0.1', port)

HandlerClass.protocol_version = Protocol
httpd = ServerClass(server_address, HandlerClass)

sa = httpd.socket.getsockname()
print "Serving HTTP on", sa[0], "port", sa[1], "..."
httpd.serve_forever()
```

Next, the `web.xml` definition. Note how `application.command` is the name of
the script followed by `%d`. This will be the given port number the application
should start serving HTTP on (referenced above where we get the port from
`sys.argv[1]`).

`web.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns="http://java.sun.com/xml/ns/javaee"
	xmlns:web="http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd"
	xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd"
	version="2.4">

	<context-param>
	  <param-name>application.command</param-name>
	  <param-value>main.py %d</param-value>
	</context-param>
	
	<context-param>
	  <param-name>application.runtime</param-name>
	  <param-value>python</param-value>
	</context-param>

  <listener>
    <listener-class>vistarmedia.elasticband.servlet.ContextListener</listener-class>
  </listener>

  <filter>
    <filter-name>guiceFilter</filter-name>
    <filter-class>com.google.inject.servlet.GuiceFilter</filter-class>
  </filter>

  <filter-mapping>
    <filter-name>guiceFilter</filter-name>
    <url-pattern>/*</url-pattern>
  </filter-mapping>
</web-app>
```

### Virtualenv runtime
Anything more involved, of course, is going to have external requirements. The
odds that they'll be installed on a vanilla Elastic Beanstalk image are about
zero. In fact, `virtualenv` itself isn't installed. If the runtime can't find
it, a wrapper will be downloaded to bootstrap from there.

Each time the application server (ie: Tomcat) is started, the virtualenv will be
set up from scratch. This is pretty darn slow, but seems most correct to me. One
problem here is that if pypi is having problems that day, your boot could hang
or fail waiting for packages to come down.

Check out the [example](/vistarmedia/elasticband/tree/master/example) directory
for an example of a virtualenv setup.

### Creating warfiles
When this project is compile, it'll generate an empty war file which won't do
much. This isn't enough to upload to Elastic Beanstalk. The supplied `package`
command in the root will help create it.

```bash
$ mvn clean package
$ cd example
$ ../package -o example.war -w ./web.xml ./site ../target/elasticband-0.1-SNAPSHOT.war
```

If that completes successfully, you'll have an `example.war` ready to be pushed
up to Elastic Beanstalk.

### Project Status
This project was hacked out in an evening to see if I could. It's not presently
used in a production environment as far as I know. Some of the code is very
round about. Patches are welcome.


### TODOs
* The Async HTTP Client seems a bit overkill since the servlet processing thread
  must block until completion, but the idea of using http-client just hurts me.
* The target is Elastic Beanstalk, so perhaps there's a way to force Tomcat to
  handle async responses? It does't seem to want to run the servlet 3.0 API, but
  there is
  [this](http://tomcat.apache.org/tomcat-7.0-doc/aio.html#Asynchronous_writes),
  but I don't think EB is using APR or NIO HTTP connectors.
* Static files served directly by the servlet container
* There are some issues on AWS I can't recreate locally where the environment
  will try to spawn many runtimes. /me scratches head.
* Of course, some non-python runtime
* When the container starts up, it doesn't wait to see when the subprocess binds
  to the port, so the first N requests probably just bomb. Not a big deal w/ EB
  due to the LB health checks
