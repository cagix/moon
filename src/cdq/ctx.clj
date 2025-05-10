(ns cdq.ctx)

; -> all libgdx or even 'clojure.gdx' hide behind main protocols ??

(declare

 ^{:doc "Implements [[cdq.assets/Assets]] and `clojure.lang.IFn` with one arg, like this: `(assets \"my_image.png\")`.

        Returns the asset or throws an exception if it cannot be found."}
 assets ; -> separate into 'audio' and 'graphics' ( ?)

 ^{:doc "Instance of [[cdq.graphics/Graphics]]."}
 graphics

 ; stage

 ; world

 ; db

 ; app => post-runnable

 ; files -> internal file-handle  (again protocol ?)

 ; input -> key/button - pressed ?

 )
