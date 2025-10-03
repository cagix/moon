(ns cdq.entity.state.player-item-on-cursor.handle-input
  (:require [cdq.entity.state.player-item-on-cursor :as player-item-on-cursor]
            [cdq.input :as input]
            [cdq.ui :as ui]))

(defn txs
  [eid {:keys [ctx/input
               ctx/stage]}]
  (let [mouseover-actor (ui/mouseover-actor stage (input/mouse-position input))]
    (when (and (input/left-mouse-button-just-pressed? input)
               (player-item-on-cursor/world-item? mouseover-actor))
      [[:tx/event eid :drop-item]])))
