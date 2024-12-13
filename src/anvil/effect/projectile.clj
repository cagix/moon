(ns anvil.effect.projectile
  (:require [anvil.component :refer [applicable? useful? handle]]
            [anvil.world :as world]
            [gdl.assets :refer [play-sound]]
            [gdl.math.vector :as v]
            [gdl.utils :refer [defmethods]]))

(defn- projectile-start-point [entity direction size]
  (v/add (:position entity)
         (v/scale direction
                  (+ (:radius entity) size 0.1))))

(defmethods :effects/projectile
  ; TODO for npcs need target -- anyway only with direction
  (applicable? [_ {:keys [effect/target-direction]}]
    target-direction) ; faction @ source also ?

  (handle [[_ projectile] {:keys [effect/source effect/target-direction]}]
    (play-sound "bfxr_waypointunlock")
    (world/projectile {:position (projectile-start-point @source
                                                         target-direction
                                                         (world/projectile-size projectile))
                       :direction target-direction
                       :faction (:entity/faction @source)}
                      projectile)))

(comment
 ; mass shooting
 (for [direction (map math.vector/normalise
                      [[1 0]
                       [1 1]
                       [1 -1]
                       [0 1]
                       [0 -1]
                       [-1 -1]
                       [-1 1]
                       [-1 0]])]
   [:tx/projectile projectile-id ...]
   )
 )
