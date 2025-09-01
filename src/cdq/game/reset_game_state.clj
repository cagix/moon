(ns cdq.game.reset-game-state
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.db :as db]
            [cdq.world.effect :as effect]
            [cdq.world.entity :as entity]
            [cdq.entity.fsm :as fsm]
            [cdq.entity.timers :as timers]
            [cdq.ctx.graphics :as graphics]
            [cdq.inventory :as inventory]
            [cdq.timer :as timer]
            [cdq.ctx.stage :as stage]
            [cdq.ui.stage]
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]
            [cdq.ctx.world :as world]))

(defn- add-skill!
  [{:keys [ctx/graphics
           ctx/stage]}
   skill]
  (stage/add-action-bar-skill! stage
                               {:skill-id (:property/id skill)
                                :texture-region (graphics/image->texture-region graphics (:entity/image skill))
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
                             {:texture-region (graphics/image->texture-region graphics (:entity/image item))
                              :tooltip-text (ctx/info-text ctx item)}))

(defn- remove-item! [{:keys [ctx/stage]} inventory-cell]
  (stage/remove-inventory-item! stage inventory-cell))

(defmethod ctx/do! :tx/assoc [[_ eid k value] _ctx]
  (swap! eid assoc k value)
  nil)

(defmethod ctx/do! :tx/assoc-in [[_ eid ks value] _ctx]
  (swap! eid assoc-in ks value)
  nil)

(defmethod ctx/do! :tx/dissoc [[_ eid k] _ctx]
  (swap! eid dissoc k)
  nil)

(defmethod ctx/do! :tx/mark-destroyed [[_ eid] _ctx]
  (swap! eid assoc :entity/destroyed? true)
  nil)

(defmethod ctx/do! :tx/mod-add [[_ eid modifiers] _ctx]
  (swap! eid entity/mod-add modifiers)
  nil)

(defmethod ctx/do! :tx/mod-remove [[_ eid modifiers] _ctx]
  (swap! eid entity/mod-remove modifiers)
  nil)

(defmethod ctx/do! :tx/effect [[_ effect-ctx effects] {:keys [ctx/world]}]
  (mapcat #(effect/handle % effect-ctx world)
          (effect/filter-applicable? effect-ctx effects)))

(defmethod ctx/do! :tx/event [[_ eid event params] {:keys [ctx/world]}]
  (fsm/event->txs world eid event params))

(defmethod ctx/do! :tx/add-skill [[_ eid skill] ctx]
  (swap! eid entity/add-skill skill)
  (when (:entity/player? @eid)
    (add-skill! ctx skill))
  nil)

#_(defn remove-skill [eid {:keys [property/id] :as skill}]
    {:pre [(contains? (:entity/skills @eid) id)]}
    (swap! eid update :entity/skills dissoc id)
    (when (:entity/player? @eid)
      (remove-skill! ctx skill)))

(defmethod ctx/do! :tx/set-cooldown [[_ eid skill] {:keys [ctx/world]}]
  (swap! eid assoc-in
         [:entity/skills (:property/id skill) :skill/cooling-down?]
         (timer/create (:world/elapsed-time world) (:skill/cooldown skill)))
  nil)

(defmethod ctx/do! :tx/add-text-effect [[_ eid text duration] {:keys [ctx/world]}]
  (swap! eid timers/add-text-effect text duration (:world/elapsed-time world))
  nil)

(defmethod ctx/do! :tx/pay-mana-cost [[_ eid cost] _ctx]
  (swap! eid entity/pay-mana-cost cost)
  nil)

(defmethod ctx/do! :tx/set-item [[_ eid cell item] ctx]
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

(defmethod ctx/do! :tx/pickup-item [[_ eid item] ctx]
  (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
    (assert cell)
    (assert (or (inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (inventory/stackable? item cell-item)
      (do
       #_(tx/stack-item ctx eid cell item))
      (ctx/do! [:tx/set-item eid cell item] ctx))))

(defmethod ctx/do! :tx/remove-item [[_ eid cell] ctx]
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

#_(defn ctx/do! [ctx eid cell item]
  #_(tx/stack-item ctx eid cell item))

; alternative: add actors at beginning
; and call 'reset-state!' function on each actor
(defn- reset-stage!
  [{:keys [ctx/config
           ctx/stage]
    :as ctx}]
  (cdq.ui.stage/clear! stage)
  (doseq [actor ((requiring-resolve (:create-ui-actors config)) ctx)]
    (cdq.ui.stage/add! stage actor))
  ctx)

(defn- add-ctx-world
  [{:keys [ctx/config]
    :as ctx}
   world-fn]
  (assoc ctx :ctx/world (world/create (merge (::world config)
                                             (let [[f params] world-fn]
                                               ((requiring-resolve f) ctx params))))))

(defn- spawn-player!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
    :as ctx}]
  (->> (let [{:keys [creature-id
                     components]} (:cdq.ctx.game/player-props config)]
         {:position (utils/tile->middle (:world/start-position world))
          :creature-property (db/build db creature-id)
          :components components})
       (world/spawn-creature! world)
       (ctx/handle-txs! ctx))
  (let [player-eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @player-eid))
    (assoc ctx :ctx/player-eid player-eid)))

(defn- spawn-enemies!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
    :as ctx}]
  (doseq [[position creature-id] (tiled/positions-with-property (:world/tiled-map world)
                                                                "creatures"
                                                                "id")]
    (->> {:position (utils/tile->middle position)
          :creature-property (db/build db (keyword creature-id))
          :components (:cdq.ctx.game/enemy-components config)}
         (world/spawn-creature! world)
         (ctx/handle-txs! ctx)))
  ctx)

; TODO dispose old tiled-map if already ctx/world present - or call 'dispose!'
(defn do! [ctx world-fn]
  (-> ctx
      reset-stage!
      (add-ctx-world world-fn)
      spawn-player!
      spawn-enemies!))

(def ^:private reset-game-state! do!)
