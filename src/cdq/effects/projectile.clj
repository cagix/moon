(ns cdq.effects.projectile
  (:require [cdq.world :as world]
            [cdq.entity :as entity]
            [clojure.math.vector2 :as v]))

(defn- proj-start-point [entity direction size]
  (v/add (entity/position entity)
         (v/scale direction
                  (+ (/ (:body/width (:entity/body entity)) 2) size 0.1))))
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
   [:world/projectile projectile-id ...]
   )
 )

; TODO for npcs need target -- anyway only with direction
(defn applicable? [_ {:keys [effect/target-direction]}]
  target-direction) ; faction @ source also ?

; TODO valid params direction has to be  non-nil (entities not los player ) ?
(defn useful? [[_ {:keys [projectile/max-range] :as projectile}]
               {:keys [effect/source effect/target]}
               world]
  (let [source-p (entity/position @source)
        target-p (entity/position @target)]
    ; is path blocked ereally needed? we need LOS also right to have a target-direction as AI?
    (and (not (world/path-blocked? world source-p target-p (:projectile/size projectile)))
         ; TODO not taking into account body sizes
         (< (v/distance source-p ; entity/distance function protocol EntityPosition
                        target-p)
            max-range))))

(defn handle [[_ projectile] {:keys [effect/source effect/target-direction]} _world]
  [[:tx/spawn-projectile
    {:position (proj-start-point @source
                                 target-direction
                                 (:projectile/size projectile))
     :direction target-direction
     :faction (:entity/faction @source)}
    projectile]])
