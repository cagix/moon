(ns ^:no-doc anvil.effect.projectile
  (:require [anvil.component :as component]
            [anvil.world :as world]
            [gdl.context :refer [play-sound]]
            [gdl.math.vector :as v]))

(defn- projectile-start-point [entity direction size]
  (v/add (:position entity)
         (v/scale direction
                  (+ (:radius entity) size 0.1))))

(defmethods :effects/projectile
  ; TODO for npcs need target -- anyway only with direction
  (component/applicable? [_ {:keys [effect/target-direction]}]
    target-direction) ; faction @ source also ?

  ; TODO valid params direction has to be  non-nil (entities not los player ) ?
  (component/useful? [[_ {:keys [projectile/max-range] :as projectile}]
            {:keys [effect/source effect/target]}]
    (let [source-p (:position @source)
          target-p (:position @target)]
      ; is path blocked ereally needed? we need LOS also right to have a target-direction as AI?
      (and (not (world/path-blocked? ; TODO test
                                     source-p
                                     target-p
                                     (world/projectile-size projectile)))
           ; TODO not taking into account body sizes
           (< (v/distance source-p ; entity/distance function protocol EntityPosition
                          target-p)
              max-range))))

  (component/handle [[_ projectile] {:keys [effect/source effect/target-direction]}]
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
