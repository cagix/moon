(ns moon.effect.projectile
  (:require [gdl.math.vector :as v]
            [moon.component :as component]
            [moon.entity :as entity]
            [moon.effect :refer [source target target-direction]]
            [moon.projectile :as projectile]
            [moon.world.raycaster :refer [path-blocked?]]))

(defn- projectile-start-point [entity direction size]
  (v/add (:position entity)
         (v/scale direction
                  (+ (:radius entity) size 0.1))))

(defmethods :effect/projectile
  {:let {:keys [entity-effects projectile/max-range] :as projectile}}
  ; TODO for npcs need target -- anyway only with direction
  (component/applicable? [_]
    target-direction) ; faction @ source also ?

  ; TODO valid params direction has to be  non-nil (entities not los player ) ?
  (component/useful? [_]
    (let [source-p (:position @source)
          target-p (:position @target)]
      (and (not (path-blocked? ; TODO test
                               source-p
                               target-p
                               (projectile/size projectile)))
           ; TODO not taking into account body sizes
           (< (v/distance source-p ; entity/distance function protocol EntityPosition
                          target-p)
              max-range))))

  (component/handle [_]
    [[:tx/sound "sounds/bfxr_waypointunlock.wav"]
     [:tx/projectile
      {:position (projectile-start-point @source
                                         target-direction
                                         (projectile/size projectile))
       :direction target-direction
       :faction (:entity/faction @source)}
      projectile]]))

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
