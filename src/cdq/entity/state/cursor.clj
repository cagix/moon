(ns cdq.entity.state.cursor
  (:require [cdq.entity.state.player-idle]
            [cdq.ui.windows.inventory :as inventory-window]
            [clojure.gdx.scenes.scene2d.ui.button :as button]
            [clojure.vis-ui.window :as window]))

(def function-map
  {:active-skill :cursors/sandclock
   :player-dead :cursors/black-x
   :player-idle (fn [player-eid ctx]
                  (let [[k params] (cdq.entity.state.player-idle/interaction-state ctx player-eid)]
                    (case k
                      :interaction-state/mouseover-actor
                      (let [actor params]
                        (let [inventory-slot (inventory-window/cell-with-item? actor)]
                          (cond
                           (and inventory-slot
                                (get-in (:entity/inventory @player-eid) inventory-slot)) :cursors/hand-before-grab
                           (window/title-bar? actor) :cursors/move-window
                           (button/is?        actor) :cursors/over-button
                           :else :cursors/default)))

                      :interaction-state/clickable-mouseover-eid
                      (let [{:keys [clicked-eid
                                    in-click-range?]} params]
                        (case (:type (:entity/clickable @clicked-eid))
                          :clickable/item (if in-click-range?
                                            :cursors/hand-before-grab
                                            :cursors/hand-before-grab-gray)
                          :clickable/player :cursors/bag))

                      :interaction-state.skill/usable
                      :cursors/use-skill

                      :interaction-state.skill/not-usable
                      :cursors/skill-not-usable

                      :interaction-state/no-skill-selected
                      :cursors/no-skill-selected)))
   :player-item-on-cursor :cursors/hand-grab
   :player-moving :cursors/walking
   :stunned :cursors/denied})
