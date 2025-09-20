# TODOs

## TODO
- HTTP client - Do we add methods to the dudestore-client for recreate, summary, list, download image...?
- HTTP client - maybe use Result type (either Kotlin's builtin Result or maybe kotlin-result...?)

- dudestore - ability to store master image so we don't have to put it on the filesystem (but do protect with secret)
- dudestore - OpenAPI spec to allow us to call APIs without having to write a client?
  - Might be handy if we ever call this stuff from JavaScript...
- dudestore - move the S3 stuff into DudeStore so we don't have to have AWS credentials in the client

- GUI - React app to interact with the above
- GUI - Proper auth

- Logging - log to a file
- Logging - log to an external service

- Modules - split codebase into modules. Note that I'm not sure if this is worth the hassle.
  - dudestore
  - dudestore client
  - evolver
  - individual-model
 
## Doing

## Done
- Logging - use kotlin-logging instead of using SLF4J directly
- HTTP client - Get integration tests to use the dudestore-client
- Config - dude config parameters in DB
- Config - API to change said parameters (note the need to inform the evolver about the change)
- Assertions - use kotest assertions, not hamkrest

## Rejected
- HTTP client - Stop using threads in dudestore-client... there ought to be an option involving coroutines, surely...
  - Apparently HTTP4K does not support coroutines and there aren't any plans to change that any time soon
  - Frankly, farming off a request to a separate thread (as we currently do) isn't the *worst* thing in the world
  - Maybe Loom will help (farm the request off to a virtual thread instead) but that's for the future when I learn Loom
- JSON - use kotlinx serialisation, not Jackson
  - More hassle than I expected, given class hierarchy of Shapes
