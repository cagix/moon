(ns ^:no-doc moon.effects.projectile
  (:require [forge.audio :refer [play-sound]]
            [forge.math.vector :as v]
            [forge.world.raycaster :refer [path-blocked?]]
            [moon.world :as world :refer [projectile-size]]))

(defn- start-point [entity direction size]
  (v/add (:position entity)
         (v/scale direction
                  (+ (:radius entity) size 0.1))))

; TODO for npcs need target -- anyway only with direction
(defn applicable? [_ {:keys [effect/target-direction]}]
  target-direction) ; faction @ source also ?

; TODO valid params direction has to be  non-nil (entities not los player ) ?
(defn useful? [{:keys [projectile/max-range] :as projectile}
               {:keys [effect/source effect/target]}]
  (let [source-p (:position @source)
        target-p (:position @target)]
    ; is path blocked ereally needed? we need LOS also right to have a target-direction as AI?
    (and (not (path-blocked? ; TODO test
                             source-p
                             target-p
                             (projectile-size projectile)))
         ; TODO not taking into account body sizes
         (< (v/distance source-p ; entity/distance function protocol EntityPosition
                        target-p)
            max-range))))

(defn handle [projectile
              {:keys [effect/source effect/target-direction]}]
  (play-sound "bfxr_waypointunlock")
  (world/projectile {:position (start-point @source target-direction (projectile-size projectile))
                     :direction target-direction
                     :faction (:entity/faction @source)}
                    projectile))

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
