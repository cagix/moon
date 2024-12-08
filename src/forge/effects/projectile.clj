(ns forge.effects.projectile
  (:require [clojure.gdx.math.vector2 :as v]
            [forge.app.asset-manager :refer [play-sound]]
            [forge.world :refer [projectile-size spawn-projectile]]
            [forge.world.raycaster :refer [path-blocked?]]))

(defn- projectile-start-point [entity direction size]
  (v/add (:position entity)
         (v/scale direction
                  (+ (:radius entity) size 0.1))))

; TODO for npcs need target -- anyway only with direction
(defn applicable? [_ {:keys [effect/target-direction]}]
  target-direction) ; faction @ source also ?

; TODO valid params direction has to be  non-nil (entities not los player ) ?
(defn useful? [[_ {:keys [projectile/max-range] :as projectile}]
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

(defn handle [[_ projectile] {:keys [effect/source effect/target-direction]}]
  (play-sound "bfxr_waypointunlock")
  (spawn-projectile {:position (projectile-start-point @source
                                                       target-direction
                                                       (projectile-size projectile))
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
