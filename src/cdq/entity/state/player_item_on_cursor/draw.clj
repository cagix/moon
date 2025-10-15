(ns cdq.entity.state.player-item-on-cursor.draw
  (:require [cdq.entity.state.player-item-on-cursor :as player-item-on-cursor]
            [cdq.graphics :as graphics]
            [cdq.input :as input]
            [cdq.ui :as ui]))

(defn txs
  [{:keys [item]}
   entity
   {:keys [ctx/graphics
           ctx/input
           ctx/stage]}]
  (when (player-item-on-cursor/world-item? (ui/mouseover-actor stage (input/mouse-position input)))
    [[:draw/texture-region
      (graphics/texture-region graphics (:entity/image item))
      (player-item-on-cursor/item-place-position (:graphics/world-mouse-position graphics)
                                                 entity)
      {:center? true}]]))
