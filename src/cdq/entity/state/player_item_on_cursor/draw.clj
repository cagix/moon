(ns cdq.entity.state.player-item-on-cursor.draw
  (:require [cdq.entity.state.player-item-on-cursor :as player-item-on-cursor]
            [cdq.graphics.textures :as textures]
            [clojure.gdx :as gdx]
            [cdq.ui :as ui]))

(defn txs
  [{:keys [item]}
   entity
   {:keys [ctx/graphics
           ctx/gdx
           ctx/stage]}]
  (when (player-item-on-cursor/world-item? (ui/mouseover-actor stage (gdx/mouse-position gdx)))
    [[:draw/texture-region
      (textures/texture-region graphics (:entity/image item))
      (player-item-on-cursor/item-place-position (:graphics/world-mouse-position graphics)
                                                 entity)
      {:center? true}]]))
