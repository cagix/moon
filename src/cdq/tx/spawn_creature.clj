(ns cdq.tx.spawn-creature
  (:require [cdq.db :as db]
            [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [cdq.impl.world :as world]))

; # :z-order/flying has no effect for now
; * entities with :z-order/flying are not flying over water,etc. (movement/air)
; because using potential-field for z-order/ground
; -> would have to add one more potential-field for each faction for z-order/flying
; * they would also (maybe) need a separate occupied-cells if they don't collide with other
; * they could also go over ground units and not collide with them
; ( a test showed then flying OVER player entity )
; -> so no flying units for now
(defn- ->body [{:keys [body/width body/height #_body/flying?]}]
  {:width  width
   :height height
   :collides? true
   :z-order :z-order/ground #_(if flying? :z-order/flying :z-order/ground)})

(defn do! [{:keys [position creature-id components]}]
  (let [props (db/build ctx/db creature-id)]
    (world/spawn-entity! position
                         (->body (:entity/body props))
                         (-> props
                             (dissoc :entity/body)
                             (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                             (utils/safe-merge components)))))
