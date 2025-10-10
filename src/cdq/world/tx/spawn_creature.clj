(ns cdq.world.tx.spawn-creature
  (:require [clojure.utils :as utils]))

(defn do!
  [_ctx
   {:keys [position
           creature-property
           components]}]
  (assert creature-property)
  [[:tx/spawn-entity
    (-> creature-property
        (assoc :entity/body (let [{:keys [body/width body/height #_body/flying?]} (:entity/body creature-property)]
                              {:position position
                               :width  width
                               :height height
                               :collides? true
                               :z-order :z-order/ground #_(if flying? :z-order/flying :z-order/ground)}))
        (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
        (utils/safe-merge components))]])

; # :z-order/flying has no effect for now
; * entities with :z-order/flying are not flying over water,etc. (movement/air)
; because using potential-field for z-order/ground
; -> would have to add one more potential-field for each faction for z-order/flying
; * they would also (maybe) need a separate occupied-cells if they don't collide with other
; * they could also go over ground units and not collide with them
; ( a test showed then flying OVER player entity )
; -> so no flying units for now
