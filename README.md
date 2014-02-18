jLattice
=================

JLattice holds the code for a prototype `java.nio`-based (`Selectors` and `Channels`) server framework.  
  
To use the framework, one needs only create versions of the classes that reside in the `com.fhs.niosrv.you.*` package and sub-packages.  
  
This package contains two sub-packages: `you.impl` and `you.sub`.  At the moment, the following are the classes one must implement:
    
  `          you.sub.Message` - with 1 method    
  `  you.impl.MessageHandler` - with 1 method     
  `you.impl.ExceptionHandler` - with 7 methods    
  `       you.impl.Resource` - with 3 methods   
    
All classes in `you.impl` are interfaces, and must be filled out completely (don't worry, for the most part they require very little!).    
Meanwhile, all classes in `you.sub` are abstract classes, and must be sub-classed.   
  
To create a server, one needs to implement (depending on server complexity) some or all of these classes. Some useful implementations are included already.

#### you.sub.Message    
  This class serves largely as a translation class - it is a wrapper for the actual message object (be that a String, HTTPHeader + XML, String + JSON, etc) that is returned to the server to be written out.  It has a single method `getEncodedMessage` that returns a `byte[]` object that is then written, verbatim, to the client.

#### you.impl.MessageHandler    
  This class is the proverbial 'dark meat' of your server - it does all of the message handling.  It also has a single method: `messageRecieved`, that one must implement.

#### you.impl.ExceptionHandler    
  Like with many Java projects, 'Handler'-classes largely do what they are named for.  `ExceptionHandler` is no exception (see what I did there?).  This class has a number of methods that are called when a specific exception occurs, either in during message processing, or otherwise.  The default implementation logs all exceptions to the console.

#### you.impl.Resource    
  If your `MessageHandler` implementation is the 'dark meat' of the server, your `Resource`s are the light meat.  These are singleton or pooled objects, held by the server, that are available to each `MessageHandler` when processing a message.  This can be anything from static resources such as custom http headers or 404 pages, to a cache, to a persistent database connection.  The sky is the limit, but it is up to you to make each `Resource` thread-safe, so be careful!

 ----
 
 TODO List:
 - [ ] Documentation!
 - [x] Read/write queues+consume-threads
 - [x] Allow non-string i/o
 - [x] Create sub-module for server-specific resource attachment (database, etc)
 - [ ] CLI interaction (gotta stop the server at some point)
 - [ ] Handler class pooling
 - [ ] Handler threading?
 - [ ] Multi-port listening
 - [ ] Multi-address listening
 - [ ] Logging service
 - [ ] ... other things?
