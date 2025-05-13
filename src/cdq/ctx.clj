(ns cdq.ctx)

(declare ^{:doc "Implements [[cdq.assets/Assets]] and `clojure.lang.IFn` with one arg, like this: `(ctx/assets \"my_image.png\")`.

                Returns the asset or throws an exception if it cannot be found."}
         assets

         ^{:doc "[[cdq.graphics/Graphics]]."}
         graphics

         stage

         ^{:doc "[[cdq.db/DB]]"}
         db

         ^{:doc "[[cdq.world/World]]."}
         world

         elapsed-time
         delta-time
         player-eid
         paused?)

(def mouseover-eid nil)

(declare reset-game!)
