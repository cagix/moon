(ns cdq.tx.player-set-item
  (:require [cdq.gdx.graphics :as graphics]
            [cdq.info :as info]
            [cdq.stage :as stage]))

(defn do!
  [{:keys [ctx/stage]
    :as ctx}
   cell item]
  (stage/set-item! stage cell
                   {:texture-region (graphics/texture-region ctx (:entity/image item))
                    :tooltip-text (fn [ctx]
                                    (info/generate (:ctx/info ctx) item ctx))})
  nil)
