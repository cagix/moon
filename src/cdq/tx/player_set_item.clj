(ns cdq.tx.player-set-item
  (:require [cdq.ctx :as ctx]
            [cdq.graphics :as graphics]
            [cdq.stage :as stage]))

(defn do!
  [{:keys [ctx/graphics
           ctx/stage]}
   cell item]
  (stage/set-item! stage cell
                   {:texture-region (graphics/texture-region graphics (:entity/image item))
                    :tooltip-text (fn [ctx]
                                    (ctx/info-text ctx item))})
  nil)
