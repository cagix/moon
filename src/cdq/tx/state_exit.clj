(ns cdq.tx.state-exit
  (:require [cdq.entity :as entity]
            [cdq.entity.state.player-item-on-cursor]))

(def exit
  {:npc-moving            (fn [_ eid _ctx]
                            [[:tx/dissoc eid :entity/movement]])
   :npc-sleeping          (fn [_ eid _ctx]
                            [[:tx/spawn-alert (entity/position @eid) (:entity/faction @eid) 0.2]
                             [:tx/add-text-effect eid "[WHITE]!" 1]])
   :player-item-on-cursor (fn [_ eid {:keys [ctx/world-mouse-position]}]
                            ; at clicked-cell when we put it into a inventory-cell
                            ; we do not want to drop it on the ground too additonally,
                            ; so we dissoc it there manually. Otherwise it creates another item
                            ; on the ground
                            (let [entity @eid]
                              (when (:entity/item-on-cursor entity)
                                [[:tx/sound "bfxr_itemputground"]
                                 [:tx/dissoc eid :entity/item-on-cursor]
                                 [:tx/spawn-item
                                  (cdq.entity.state.player-item-on-cursor/item-place-position world-mouse-position entity)
                                  (:entity/item-on-cursor entity)]])))
   :player-moving         (fn [_ eid _ctx]
                            [[:tx/dissoc eid :entity/movement]])})

(defn do! [[_ eid [state-k state-v]] ctx]
  (when-let [f (state-k exit)]
    (f state-v eid ctx)))
