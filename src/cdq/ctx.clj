(ns cdq.ctx)

(declare

 ^{:doc "Implements [[cdq.assets/Assets]] and `clojure.lang.IFn` with one arg, like this: `(assets \"my_image.png\")`.

        Returns the asset or throws an exception if it cannot be found."}
 assets

 ^{:doc "Instance of [[cdq.graphics/Graphics]]."}
 graphics

 ^{:doc "[[cdq.db/DB]]"}
 db

 ; TODO hide 'grid' , spawn-entity!, spawn-enemies!,
 ; :raycaster
 ; ;active-entities ( ? )  -> render-entities! ?
 ; hide 'elapsed-time' ?

 ^{:doc "Instance of [[cdq.world/World]]

        Keys:

        * `:mouseover-eid` - may be nil or an `eid`

        * `:player-eid`

        * `:elapsed-time` - in milliseconds non-paused game time

        * `:paused?` - wheter the game world is paused and not updating

   [[cdq.world/World]]"}
 world
 )
