# com.badlogic.gdx namespaces

Only class-internal APIs (e.g. configuration can create via map)
No cross-dependencies

Sometimes very simple e.g. just application/create changing the name only.

Purpose: Owning all methods/constructors, etc. Can re-implement in clojure as needed without touching other code.

# clojure.gdx

Combination of class internal APIs into a clojure API, depending in 'com.badlogic.gdx' namespaces
