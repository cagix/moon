(ns cdq.ctx.tx-handler
  (:require [cdq.db :as db]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.entity.fsm :as fsm]
            [cdq.entity.timers :as timers]
            [cdq.inventory :as inventory]
            [cdq.modifiers :as modifiers]
            [cdq.rand :refer [rand-int-between]]
            [cdq.timer :as timer]
            [cdq.utils :as utils]
            [cdq.world :as world]
            [gdl.math.vector2 :as v]))

(defn- valid-tx? [transaction]
  (vector? transaction))

(defn- handle-world-event!
  [{:keys [ctx/world-event-handlers]
    :as ctx}
   [world-event-k params]]
  ((utils/safe-get world-event-handlers world-event-k) ctx params))

(defmulti do! (fn [[k & _params] _ctx]
                k))

(defn handle-txs!
  [ctx
   transactions]
  ;(println "\n~handle-txs! " (map first (remove nil? transactions)))
  ;(println "~\n")
  (let [world-events (remove nil?
                             (for [transaction transactions
                                   :when transaction]
                               (do
                                (assert (valid-tx? transaction) (pr-str transaction))
                                (try (do! transaction ctx)
                                     (catch Throwable t
                                       (throw (ex-info "" {:transaction transaction} t)))))))]
    (binding [*print-level* 2]
      ;(println "\n~world-events: " world-events)
      ;(println "~\n")
      )
    (run! (partial handle-world-event! ctx) world-events)))

(defmethod do! :tx/toggle-inventory-visible [_ _ctx]
  [:world.event/toggle-inventory-visible])

(defmethod do! :tx/show-message [[_ message] _ctx]
  [:world.event/show-player-message message])

(defmethod do! :tx/show-modal [[_ opts] _ctx]
  [:world.event/show-modal-window opts])

(defmethod do! :tx/sound [[_ sound-name] _ctx]
  [:world.event/sound sound-name])

(defmethod do! :tx/state-exit [[_ eid [state-k state-v]]
                               {:keys [ctx/entity-states] :as ctx}]
  (handle-txs! ctx
               (when-let [f (state-k (:state->exit entity-states))]
                 (f state-v eid ctx))))

(defmethod do! :tx/state-enter [[_ eid [state-k state-v]]
                                {:keys [ctx/entity-states] :as ctx}]
  (handle-txs! ctx
               (when-let [f (state-k (:state->enter entity-states))]
                 (f state-v eid))))

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

(defmethod do! :tx/effect [[_ effect-ctx effects] ctx]
  (run! #(handle-txs! ctx (effect/handle % effect-ctx ctx))
        (effect/filter-applicable? effect-ctx effects)))

(defmethod do! :tx/event [[_ eid event params] ctx]
  (handle-txs! ctx (fsm/event->txs ctx eid event params)))

(defmethod do! :tx/add-skill [[_ eid skill] _ctx]
  (swap! eid entity/add-skill skill)
  (when (:entity/player? @eid)
    [:world.event/player-skill-added skill]))

#_(defn remove-skill [eid {:keys [property/id] :as skill}]
    {:pre [(contains? (:entity/skills @eid) id)]}
    (swap! eid update :entity/skills dissoc id)
    (when (:entity/player? @eid)
      [:world.event/player-skill-removed skill]))

(defmethod do! :tx/set-cooldown [[_ eid skill] {:keys [ctx/elapsed-time]}]
  (swap! eid assoc-in
         [:entity/skills (:property/id skill) :skill/cooling-down?]
         (timer/create elapsed-time (:skill/cooldown skill)))
  nil)

(defmethod do! :tx/add-text-effect [[_ eid text duration] {:keys [ctx/elapsed-time]}]
  (swap! eid timers/add-text-effect text duration elapsed-time)
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
      [:world.event/player-item-set [cell item]])))

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
      [:world.event/player-item-removed cell])))

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

(defmethod do! :tx/audiovisual [[_ position audiovisual]
                                {:keys [ctx/db]
                                 :as ctx}]
  (let [{:keys [tx/sound
                entity/animation]} (if (keyword? audiovisual)
                                     (db/build db audiovisual)
                                     audiovisual)]
    (do! [:tx/sound sound]
         ctx)
    (do! [:tx/spawn-effect
          position
          {:entity/animation animation
           :entity/delete-after-animation-stopped? true}]
         ctx)
    nil))

(defmethod do! :tx/spawn-alert [[_ position faction duration]
                                {:keys [ctx/elapsed-time] :as ctx}]
  (do! [:tx/spawn-effect
        position
        {:entity/alert-friendlies-after-duration
         {:counter (timer/create elapsed-time duration)
          :faction faction}}]
       ctx)
  nil)

(defmethod do! :tx/spawn-line [[_ {:keys [start end duration color thick?]}] ctx]
  (do! [:tx/spawn-effect
        start
        {:entity/line-render {:thick? thick? :end end :color color}
         :entity/delete-after-duration duration}]
       ctx)
  nil)

(defmethod do! :tx/deal-damage [[_ source target damage] ctx]
  (let [source* @source
        target* @target
        hp (entity/hitpoints target*)]
    (handle-txs! ctx
                 (cond
                  (zero? (hp 0))
                  nil

                  (< (rand) (modifiers/effective-armor-save (:creature/stats source*)
                                                            (:creature/stats target*)))
                  [[:tx/add-text-effect target "[WHITE]ARMOR" 0.3]]

                  :else
                  (let [min-max (:damage/min-max (modifiers/damage (:creature/stats source*)
                                                                   (:creature/stats target*)
                                                                   damage))
                        dmg-amount (rand-int-between min-max)
                        new-hp-val (max (- (hp 0) dmg-amount)
                                        0)]
                    [[:tx/assoc-in target [:creature/stats :entity/hp 0] new-hp-val]
                     [:tx/event    target (if (zero? new-hp-val) :kill :alert)]
                     [:tx/audiovisual (entity/position target*) :audiovisuals/damage]
                     [:tx/add-text-effect target (str "[RED]" dmg-amount "[]") 0.3]])))))

(defmethod do! :tx/set-movement [[_ eid movement-vector] _ctx]
  (swap! eid entity/set-movement movement-vector)
  nil)

(defmethod do! :tx/move-entity [[_ & opts] ctx]
  (apply world/move-entity! ctx opts))

(defmethod do! :tx/spawn-projectile
  [[_
    {:keys [position direction faction]}
    {:keys [entity/image
            projectile/max-range
            projectile/speed
            entity-effects
            projectile/size
            projectile/piercing?] :as projectile}]
   ctx]
  (world/spawn-entity! ctx
                       position
                       {:width size
                        :height size
                        :z-order :z-order/flying
                        :rotation-angle (v/angle-from-vector direction)}
                       {:entity/movement {:direction direction
                                          :speed speed}
                        :entity/image image
                        :entity/faction faction
                        :entity/delete-after-duration (/ max-range speed)
                        :entity/destroy-audiovisual :audiovisuals/hit-wall
                        :entity/projectile-collision {:entity-effects entity-effects
                                                      :piercing? piercing?}})
  nil)

(defmethod do! :tx/spawn-effect
  [[_ position components]
   {:keys [ctx/config]
    :as ctx}]
  (world/spawn-entity! ctx
                       position
                       (:effect-body-props config)
                       components)
  nil)

(defmethod do! :tx/spawn-item [[_ position item] ctx]
  (world/spawn-entity! ctx
                       position
                       {:width 0.75
                        :height 0.75
                        :z-order :z-order/on-ground}
                       {:entity/image (:entity/image item)
                        :entity/item item
                        :entity/clickable {:type :clickable/item
                                           :text (:property/pretty-name item)}})
  nil)

(defmethod do! :tx/spawn-creature [[_ opts] ctx]
  (world/spawn-creature! ctx opts)
  nil)
