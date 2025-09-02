(ns cdq.tx-impl
  (:require [cdq.ctx :refer [do!]]
            [cdq.timer :as timer]
            [cdq.inventory :as inventory]
            [cdq.entity.timers :as timers]
            [cdq.world.effect :as effect]
            [cdq.world.entity.stats :as modifiers]))

(defmethod do! :tx/assoc [[_ eid k value] _ctx]
  (swap! eid assoc k value)
  nil)

(defmethod do! :tx/assoc-in [[_ eid ks value] _ctx]
  (swap! eid assoc-in ks value)
  nil)

(defmethod do! :tx/dissoc [[_ eid k] _ctx]
  (swap! eid dissoc k)
  nil)

(defmethod do! :tx/mark-destroyed [[_ eid] _ctx]
  (swap! eid assoc :entity/destroyed? true)
  nil)

(defmethod do! :tx/mod-add [[_ eid modifiers] _ctx]
  (swap! eid update :creature/stats modifiers/add modifiers)
  nil)

(defmethod do! :tx/mod-remove [[_ eid modifiers] _ctx]
  (swap! eid update :creature/stats modifiers/remove modifiers)
  nil)

(defmethod do! :tx/effect [[_ effect-ctx effects] {:keys [ctx/world]}]
  (mapcat #(effect/handle % effect-ctx world)
          (effect/filter-applicable? effect-ctx effects)))

(defmethod do! :tx/set-cooldown [[_ eid skill] {:keys [ctx/world]}]
  (swap! eid assoc-in
         [:entity/skills (:property/id skill) :skill/cooling-down?]
         (timer/create (:world/elapsed-time world) (:skill/cooldown skill)))
  nil)

(defmethod do! :tx/add-text-effect [[_ eid text duration] {:keys [ctx/world]}]
  (swap! eid timers/add-text-effect text duration (:world/elapsed-time world))
  nil)

(defn- pay-mana-cost [entity cost]
  (update entity :creature/stats modifiers/pay-mana-cost cost))

(defmethod do! :tx/pay-mana-cost [[_ eid cost] _ctx]
  (swap! eid pay-mana-cost cost)
  nil)

(defmethod do! :tx/pickup-item [[_ eid item] ctx]
  (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
    (assert cell)
    (assert (or (inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (inventory/stackable? item cell-item)
      (do
       #_(tx/stack-item ctx eid cell item))
      (do! [:tx/set-item eid cell item] ctx))))



; TODO doesnt exist, stackable, usable items with action/skillbar thingy
#_(defn remove-one-item [eid cell]
  (let [item (get-in (:entity/inventory @eid) cell)]
    (if (and (:count item)
             (> (:count item) 1))
      (do
       ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
       ; first remove and then place, just update directly  item ...
       (cdq.tx.remove-item/do! eid cell)
       (cdq.tx.set-item/do! eid cell (update item :count dec)))
      (cdq.tx.remove-item/do! eid cell))))

; TODO no items which stack are available
#_(defn stack-item [eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (inventory/stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (cdq.tx.remove-item/do! eid cell)
            (cdq.tx.set-item/do! eid cell (update cell-item :count + (:count item))))))

#_(defn do! [ctx eid cell item]
  #_(tx/stack-item ctx eid cell item))
