(ns cdq.ctx)

(declare

 ^{:doc "Implements [[cdq.assets/Assets]] and `clojure.lang.IFn` with one arg, like this: `(assets \"my_image.png\")`.

        Returns the asset or throws an exception if it cannot be found."}
 assets

 ^{:doc "Instance of [[cdq.graphics/Graphics]]."}
 graphics

 ^{:doc "[[cdq.db/DB]]"}
 db

 ^{:doc "[[cdq.world/World]]"}
 world
 )
