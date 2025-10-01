(ns cdq.entity.state.player-item-on-cursor.draw
  (:require [cdq.entity.state.player-item-on-cursor :as player-item-on-cursor]
            [cdq.graphics.textures :as textures]
            [cdq.input :as input]
            [cdq.stage :as stage]))

(defn txs
  [{:keys [item]}
   entity
   {:keys [ctx/graphics
           ctx/input
           ctx/stage]}]
  (when (player-item-on-cursor/world-item? (stage/mouseover-actor stage
                                                                  (input/mouse-position input)))
    [[:draw/texture-region
      (textures/texture-region graphics (:entity/image item))
      (player-item-on-cursor/item-place-position (:graphics/world-mouse-position graphics)
                                                 entity)
      {:center? true}]]))
