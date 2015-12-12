# portable Time Stamp Server (over HTTP)

This is Time-Stamp Protocol via HTTP server (https://tools.ietf.org/html/rfc3161 3.4.)
https://en.wikipedia.org/wiki/Trusted_timestamping

All operations are based on OpenSSL extension called ts. From version 0.9.8 it is a part of openssl binary so patching is not required anymore.

It is based on idea from Grzegorz Golec (http://gregs.eu/linux-time-stamp-server/), but his server received request from tcp connection. Java libraries uses only HTTP to send TSA request, so i wrote this http version of TSA server.

## USAGE:
install groovy
groovy ./server.groovy
curl -X POST -H "Content-Type:application/timestamp-query" -d @mycertificatefile localhost:318 -v

or call it from Java using class TSAClientBouncyCastle(itext).

## Requirements
* groovy (and java of cource)
* OpenSSL > 0.9.8
* CA or request for certificate
* certificate for TSA signing

## Installation:

You need ssl key and certificate that can be used for Timestamping. 

This certificate must have attributes:
* keyUsage = nonRepudiation
* extendedKeyUsage = timeStamping, critical

Contant your certification authority to get this cert or for testing create self signed (see below)

Than configure openssl in openssl.cnf:

```sh

[ tsa ]

default_tsa = tsa_config1 # the default TSA section

[ tsa_config1 ]

# These are used by the TSA reply generation only. 
dir = /etc/ssl/tsa # TSA root directory 
serial = $dir/serial # The current serial number (mandatory) 
crypto_device = builtin # OpenSSL engine to use for signing 

signer_cert = $dir/tsa.crt # The TSA signing certificate
signer_key = $dir/tsa.key # The TSA private key

certs = $dir/cacert.pem # Certificate chain to include in reply # (optional) 

default_policy = tsa_policy1 # Policy if request did not specify it # (optional) 
other_policies = tsa_policy2, tsa_policy3 # acceptable policies (optional) 
digests = md5, sha1 # Acceptable message digests (mandatory) 
accuracy = secs:1, millisecs:500, microsecs:100 # (optional) 
clock_precision_digits = 0 # number of digits after dot. (optional) 
ordering = yes # Is ordering defined for timestamps? # (optional, default: no) 
tsa_name = yes # Must the TSA name be included in the reply? # (optional, default: no) 
ess_cert_id_chain = no # Must the ESS cert id chain be included? # (optional, default: no) 

```

