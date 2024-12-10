# Operation minimal dependencies - _only_ _simple_ parts

* draw on namespace graph green/orange/red ...

* depend only on what you need -> simple API

e.g. change-screen only requires enter/exit not even render/dispose .... so can be a different API?

* render API/protocol ... interesting .... and dispose protocol already exists ....

* Every function minimal context/environment/dependencies - direct .... ?
* But that means we don't wrap gdx at all?

# What are my _real_ building blocks/dependencies/structure ?

# Abstract stuff and put it into context not for making a library but relative or where things just should be

# =>

# Every form in the right place in the context of the whole project etc.

e.g. play-sound only depends on assets/get -> so there needs to be a namespace with only assets/get...

=> But then again how are the elements related to another in the folder structure?

.....

=> e.g. 'forge' is just the gdx abstraction/context ??
forge/assets related to gdx only ... not any game or not even to 'forge.app'
