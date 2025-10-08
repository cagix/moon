(ns cdq.entity.state.player-item-on-cursor.handle-input
  (:require [cdq.entity.state.player-item-on-cursor :as player-item-on-cursor]
            [cdq.input :as input]
            [cdq.ui :as ui]
            [clojure.gdx :as gdx])
  (:import (com.badlogic.gdx Input$Buttons)))

(defn txs
  [eid {:keys [ctx/gdx
               ctx/input
               ctx/stage]}]
  (let [mouseover-actor (ui/mouseover-actor stage (input/mouse-position input))]
    (when (and (gdx/button-just-pressed? gdx Input$Buttons/LEFT)
               (player-item-on-cursor/world-item? mouseover-actor))
      [[:tx/event eid :drop-item]])))
