(ns cdq.tx.player-set-item
  (:require [cdq.gdx.graphics :as graphics]
            [cdq.info :as info]
            [cdq.ctx.stage :as stage]))

(defn do!
  [{:keys [ctx/graphics
           ctx/stage]}
   cell item]
  (stage/set-item! stage cell
                   {:texture-region (graphics/texture-region graphics (:entity/image item))
                    :tooltip-text (fn [ctx]
                                    (info/generate (:ctx/info ctx) item ctx))})
  nil)
