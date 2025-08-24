# TODOs

## TODO

- HTTP client - Do we add methods to the dudestore-client for recreate, summary, list, download image...?
- HTTP client - maybe use Result type (either Kotlin's builtin Result or maybe kotlin-result...?)
- HTTP client - Stop using threads in dudestore-client... there ought to be an option involving coroutines, surely...

- dudestore - ability to store master image so we don't have to put it on the filesystem (but do protect with secret)
- dudestore - OpenAPI spec to allow us to call APIs without having to write a client?
  - Might be handy if we ever call this stuff from JavaScript...

- JSON - use kotlinx serialisation, not Jackson

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
- Config - dude config parameters in DB
- Config - API to change said parameters (note the need to inform the evolver about the change)

## Done
- Logging - use kotlin-logging instead of using SLF4J directly
- HTTP client - Get integration tests to use the dudestore-client
