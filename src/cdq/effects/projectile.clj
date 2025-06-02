(ns cdq.effects.projectile
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.projectile :as projectile]
            [cdq.raycaster :as raycaster]
            [cdq.vector2 :as v]
            [clojure.utils :refer [defcomponent]]))

(defn- start-point [entity direction size]
  (v/add (entity/position entity)
         (v/scale direction
                  (+ (:radius entity) size 0.1))))

(defcomponent :effects/projectile
  ; TODO for npcs need target -- anyway only with direction
  (effect/applicable? [_ {:keys [effect/target-direction]}]
    target-direction) ; faction @ source also ?

  ; TODO valid params direction has to be  non-nil (entities not los player ) ?
  (effect/useful? [[_ {:keys [projectile/max-range] :as projectile}]
                   {:keys [effect/source effect/target]}
                   {:keys [ctx/raycaster]}]
    (let [source-p (entity/position @source)
          target-p (entity/position @target)]
      ; is path blocked ereally needed? we need LOS also right to have a target-direction as AI?
      (and (not (raycaster/path-blocked? raycaster
                                         source-p
                                         target-p
                                         (projectile/size projectile)))
           ; TODO not taking into account body sizes
           (< (v/distance source-p ; entity/distance function protocol EntityPosition
                          target-p)
              max-range))))

  (effect/handle [[_ projectile] {:keys [effect/source effect/target-direction]} _ctx]
    [[:tx/spawn-projectile
      {:position (start-point @source
                              target-direction
                              (projectile/size projectile))
       :direction target-direction
       :faction (entity/faction @source)}
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
   [:world/projectile projectile-id ...]
   )
 )
