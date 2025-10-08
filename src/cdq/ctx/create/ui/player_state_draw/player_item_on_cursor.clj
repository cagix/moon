(ns cdq.ctx.create.ui.player-state-draw.player-item-on-cursor
  (:require [cdq.entity.state.player-item-on-cursor :as player-item-on-cursor]
            [cdq.graphics.textures :as textures]
            [cdq.ui :as ui]
            [clojure.gdx :as gdx]))

(defn draws
  [eid
   {:keys [ctx/gdx
           ctx/graphics
           ctx/stage]}]
  (when (not (player-item-on-cursor/world-item? (ui/mouseover-actor stage (gdx/mouse-position gdx))))
    [[:draw/texture-region
      (textures/texture-region graphics (:entity/image (:entity/item-on-cursor @eid)))
      (:graphics/ui-mouse-position graphics)
      {:center? true}]]))
