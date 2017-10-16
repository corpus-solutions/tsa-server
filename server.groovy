#!/usr/bin/env groovy

//VERSION 1.0

//parameters
def PORT = 318;
def OPENSSL_CFG_FILE = "/etc/ssl/openssl.cnf";
def GRACEFUL_DELAY_SECONDS = 3
def openssl = "openssl"; ///usr/bin/openssl

/*
Copyright [2015] [Jan Zajic]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import com.sun.net.httpserver.*;
import java.util.concurrent.Executors;
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.FileOutputStream
import java.nio.file.Files;

println "Starting server on port $PORT"

//configuring a Java 6 HttpServer
InetSocketAddress addr = new InetSocketAddress(PORT)
httpServer = com.sun.net.httpserver.HttpServer.create(addr, 0)
httpThreadPool = Executors.newCachedThreadPool()
httpServer.with {
    createContext('/', new ReverseHandler(OPENSSL_CFG_FILE,openssl))
    setExecutor(httpThreadPool)
    start()
}

println "READY for use !"

Runtime.runtime.addShutdownHook {
  println "Shutting down..."
  httpServer.stop(GRACEFUL_DELAY_SECONDS)
  httpThreadPool.shutdownNow()
  println "server stopped"
}

//test it:
// curl -X POST -H "Content-Type:application/timestamp-query" -d @myfilename localhost:318 -v

class ReverseHandler implements HttpHandler {
	
	private String openSslCfg;
	private String openSslBin;
	
	ReverseHandler(String openSslCfg, String openSslBin)
	{
		this.openSslBin = openSslBin;
		this.openSslCfg= openSslCfg;
	}
	
	private void handleTimestampRequest(HttpExchange httpExchange)
	{	
		File input = null;
		File output = null;
		byte[] buf = new byte[2048];
		int read = 0;
		
		try {
			println("handleTimestampRequest");
			input = File.createTempFile("req-in", ".tmp");
			output = File.createTempFile("req-out", ".tmp");
			input.deleteOnExit();						
			output.deleteOnExit();						
		} catch(RuntimeException e) {
			e.printStackTrace();
			return;
		}
		
		try {	
			println(input.absolutePath);
			
			InputStream body;
			OutputStream inputFileOutStream;
			//copy request to temp file
			try
			{
				body = httpExchange.getRequestBody();
				inputFileOutStream = new FileOutputStream(input);
				
				while ((read = body.read(buf)) != -1) {
					inputFileOutStream.write(buf, 0, read);
				}
				println("request written to file "+input.absolutePath)
			} finally {
				body.close(); inputFileOutStream.close();
			}
			
			def sout = new StringBuffer(), serr = new StringBuffer()
			println("starting "+openSslBin);
			def proc = "$openSslBin ts -reply -config $openSslCfg -queryfile $input.absolutePath -out $output.absolutePath".execute()			
			println("$openSslBin wait")
			proc.waitForProcessOutput(sout, serr)			
			println("$openSslBin end")
			def exitValue = proc.exitValue()
			//log
			println("openssl result "+exitValue);
			println(serr.toString())
			println(sout.toString())
			println("response written to file "+output.absolutePath)
			
			httpExchange.responseHeaders.set('Content-Type', 'application/timestamp-reply')
			httpExchange.sendResponseHeaders(200, 0)
			
			//write response to client
			InputStream outputFileInStream;
			OutputStream out;
			
			try
			{
				outputFileInStream = new FileInputStream(output);
				out = httpExchange.getResponseBody();
				
				while ((read = outputFileInStream.read(buf)) != -1) {
					out.write(buf, 0, read);
				}
				out.flush();
			} finally {
				outputFileInStream.close(); out.close();
			}			
			
			httpExchange.close();
		} catch(Exception e) {
			e.printStackTrace();
			httpExchange.sendResponseHeaders(400,0)
			httpExchange.responseBody.close()
		} finally {
			input.delete();
			output.delete();
		}
	}
	
    @Override
    public void handle(HttpExchange httpExchange) throws IOException
    {
		try {
			String requestMethod = httpExchange.requestMethod
			println "requestMethod: '"+requestMethod+"'"
			if(requestMethod == 'POST') {				
				String type = httpExchange.requestHeaders.get('Content-Type')
				println "Content-Type: "+type
				if(type == "[application/timestamp-query]")
				{
					println("timestamp-query");
					handleTimestampRequest(httpExchange)
					return;
				}				
			}
			httpExchange.sendResponseHeaders(400,0)
			httpExchange.responseBody.close()
		} catch(RuntimeException e) {
			e.printStackTrace();
		}		
    }
}