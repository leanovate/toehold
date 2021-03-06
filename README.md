toehold
=======

Get a toehold on PHP (behind Play or java in general)

# Main elements

* FastCGI client for the play framework to use `php-fpm` behind a play application

# Example

Run the example to see it in action

```
git submodule init
git submodule update
cd examples/play
sbt run
```

Now you can visit `http://localhost:9000/dokuwiki/index.php` 

# Build info

Cross compiling is done via system properties. As default the project will build as scala "2.11.7":

svt -Dtoehold.scalaVersion=2.10.6

# Build status

[![Build Status](https://travis-ci.org/leanovate/toehold.svg?branch=master)](https://travis-ci.org/leanovate/toehold)

# Licence

[MIT Licence](http://opensource.org/licenses/MIT)
