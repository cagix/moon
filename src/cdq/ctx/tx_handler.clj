(ns cdq.ctx.tx-handler
  (:require [cdq.ctx :as ctx]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.entity.fsm :as fsm]
            [cdq.entity.timers :as timers]
            [cdq.inventory :as inventory]
            [cdq.timer :as timer]
            [cdq.ui :as stage]
            [cdq.utils :as utils]
            [cdq.graphics :as g]))

(defn- valid-tx? [transaction]
  (vector? transaction))

(defmulti do! (fn [[k & _params] _ctx]
                k))

(defn- handle-tx! [tx ctx]
  (assert (valid-tx? tx) (pr-str tx))
  (try
   (do! tx ctx)
   (catch Throwable t
     (throw (ex-info "Error handling transaction" {:transaction tx} t)))))

(defn handle-txs!
  "Handles transactions and returns a flat list of all transactions handled, including nested."
  [ctx transactions]
  (loop [ctx ctx
         txs transactions
         handled []]
    (if (seq txs)
      (let [tx (first txs)]
        (if tx
          (let [new-txs (handle-tx! tx ctx)]
              (recur ctx
                     (concat (or new-txs []) (rest txs))
                     (conj handled tx)))
          (recur ctx (rest txs) handled)))
      handled)))

(defn- add-skill!
  [{:keys [ctx/graphics
           ctx/stage]}
   skill]
  (stage/add-action-bar-skill! stage
                               {:skill-id (:property/id skill)
                                :texture-region (g/image->texture-region graphics (:entity/image skill))
                                ; (assoc ctx :effect/source (world/player)) FIXME
                                :tooltip-text #(ctx/info-text % skill)})
  nil)

(defn- remove-skill! [{:keys [ctx/stage]} skill]
  (stage/remove-action-bar-skill! stage (:property/id skill))
  nil)

(defn- set-item!
  [{:keys [ctx/graphics
           ctx/stage]
    :as ctx}
   inventory-cell item]
  (stage/set-inventory-item! stage
                             inventory-cell
                             {:texture-region (g/image->texture-region graphics (:entity/image item))
                              :tooltip-text (ctx/info-text ctx item)}))

(defn- remove-item! [{:keys [ctx/stage]} inventory-cell]
  (stage/remove-inventory-item! stage inventory-cell))

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
  (swap! eid entity/mod-add modifiers)
  nil)

(defmethod do! :tx/mod-remove [[_ eid modifiers] _ctx]
  (swap! eid entity/mod-remove modifiers)
  nil)

(defmethod do! :tx/effect [[_ effect-ctx effects] {:keys [ctx/world]}]
  (mapcat #(effect/handle % effect-ctx world)
          (effect/filter-applicable? effect-ctx effects)))

(defmethod do! :tx/event [[_ eid event params] {:keys [ctx/world]}]
  (fsm/event->txs world eid event params))

(defmethod do! :tx/add-skill [[_ eid skill] ctx]
  (swap! eid entity/add-skill skill)
  (when (:entity/player? @eid)
    (add-skill! ctx skill))
  nil)

#_(defn remove-skill [eid {:keys [property/id] :as skill}]
    {:pre [(contains? (:entity/skills @eid) id)]}
    (swap! eid update :entity/skills dissoc id)
    (when (:entity/player? @eid)
      (remove-skill! ctx skill)))

(defmethod do! :tx/set-cooldown [[_ eid skill] {:keys [ctx/world]}]
  (swap! eid assoc-in
         [:entity/skills (:property/id skill) :skill/cooling-down?]
         (timer/create (:world/elapsed-time world) (:skill/cooldown skill)))
  nil)

(defmethod do! :tx/add-text-effect [[_ eid text duration] {:keys [ctx/world]}]
  (swap! eid timers/add-text-effect text duration (:world/elapsed-time world))
  nil)

(defmethod do! :tx/pay-mana-cost [[_ eid cost] _ctx]
  (swap! eid entity/pay-mana-cost cost)
  nil)

(defmethod do! :tx/set-item [[_ eid cell item] ctx]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (inventory/valid-slot? cell item)))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-add (:entity/modifiers item)))
    (when (:entity/player? entity)
      (set-item! ctx cell item))
    nil))

(defmethod do! :tx/pickup-item [[_ eid item] ctx]
  (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
    (assert cell)
    (assert (or (inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (inventory/stackable? item cell-item)
      (do
       #_(tx/stack-item ctx eid cell item))
      (do! [:tx/set-item eid cell item] ctx))))

(defmethod do! :tx/remove-item [[_ eid cell] ctx]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-remove (:entity/modifiers item)))
    (when (:entity/player? entity)
      (remove-item! ctx cell))
    nil))

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
