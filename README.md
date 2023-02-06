## GemFire Disk Store Compact Function

This function compacts online disk stores. The function provides the option to compact 
all disk stores, region disk stores, gateway queue disk stores, asynchronous queue
disk stores or individual named disk stores.

### Deployment
`gsfh> deploy --jar gemfire-compact-function-1.0.0-SNAPSHOT.jar`

### Function Call

`gfsh> execute function --id CompactDiskStoreFunction --member {member-name} --arguments {see below}` 

Arguments can be:
  
  #### Argument 1 - Types
  - ALL - Compacts all disk stores defined in server     
  - REGION - Compacts all region disk stores
  - GATEWAY - Compacts all gateway disk stores
  - QUEUE - Compacts all asynchronous queue disk stores
  - STORE - Compacts a single disk store 

  #### Argument 2
  Disk Store Name

When specifying ALL,REGION,GATEWAY,QUEUE argument types, only one argument is needed.

Example: `--arguments "ALL"`

When specifying the STORE argument, the second argument must be the name of the disk store. 
If the disk store defined as the second argument, does not exist, the function will throw the exception `NoDiskStoreExists`

Example: `--arguments "STORE,{disk-store-name}"`

If no or an invalid type is specified in first argument, the function will throw the exception `Invalid Argument Type Specified`





