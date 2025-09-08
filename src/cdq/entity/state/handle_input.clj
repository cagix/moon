(ns cdq.entity.state.handle-input
  (:require [cdq.controls :as controls]
            [cdq.entity.state.player-idle]
            [cdq.entity.state.player-item-on-cursor]
            [cdq.inventory :as inventory]
            [cdq.stats :as stats]
            [cdq.ui.windows.inventory :as inventory-window]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d.actor :as actor]))

(defn- speed [{:keys [creature/stats]}]
  (or (stats/get-stat-value stats :entity/movement-speed)
      0))

(defn inventory-window-visible? [stage]
  (-> stage :windows :inventory-window actor/visible?))

(defn can-pickup-item? [entity item]
  (inventory/can-pickup-item? (:entity/inventory entity) item))

(defn interaction-state->txs [ctx player-eid]
  (let [[k params] (cdq.entity.state.player-idle/interaction-state ctx player-eid)]
    (case k
      :interaction-state/mouseover-actor nil ; handled by ui actors themself.

      :interaction-state/clickable-mouseover-eid
      (let [{:keys [clicked-eid
                    in-click-range?]} params]
        (if in-click-range?
          (case (:type (:entity/clickable @clicked-eid))
            :clickable/player
            [[:tx/toggle-inventory-visible]]

            :clickable/item
            (let [item (:entity/item @clicked-eid)]
              (cond
               (inventory-window-visible? (:ctx/stage ctx))
               [[:tx/sound "bfxr_takeit"]
                [:tx/mark-destroyed clicked-eid]
                [:tx/event player-eid :pickup-item item]]

               (can-pickup-item? @player-eid item)
               [[:tx/sound "bfxr_pickup"]
                [:tx/mark-destroyed clicked-eid]
                [:tx/pickup-item player-eid item]]

               :else
               [[:tx/sound "bfxr_denied"]
                [:tx/show-message "Your Inventory is full"]])))
          [[:tx/sound "bfxr_denied"]
           [:tx/show-message "Too far away"]]))

      :interaction-state.skill/usable
      (let [[skill effect-ctx] params]
        [[:tx/event player-eid :start-action [skill effect-ctx]]])

      :interaction-state.skill/not-usable
      (let [state params]
        [[:tx/sound "bfxr_denied"]
         [:tx/show-message (case state
                             :cooldown "Skill is still on cooldown"
                             :not-enough-mana "Not enough mana"
                             :invalid-params "Cannot use this here")]])

      :interaction-state/no-skill-selected
      [[:tx/sound "bfxr_denied"]
       [:tx/show-message "No selected skill"]])))

(def function-map
  {:player-idle           (fn [player-eid {:keys [ctx/input] :as ctx}]
                            (if-let [movement-vector (controls/player-movement-vector input)]
                              [[:tx/event player-eid :movement-input movement-vector]]
                              (when (input/button-just-pressed? input :left)
                                (interaction-state->txs ctx player-eid))))

   :player-item-on-cursor (fn [eid
                               {:keys [ctx/input
                                       ctx/mouseover-actor]}]
                            (when (and (input/button-just-pressed? input :left)
                                       (cdq.entity.state.player-item-on-cursor/world-item? mouseover-actor))
                              [[:tx/event eid :drop-item]]))
   :player-moving         (fn [eid {:keys [ctx/input]}]
                            (if-let [movement-vector (controls/player-movement-vector input)]
                              [[:tx/assoc eid :entity/movement {:direction movement-vector
                                                                :speed (speed @eid)}]]
                              [[:tx/event eid :no-movement-input]]))})
