(ns cdq.tx.player-set-item
  (:require [cdq.image :as image]
            [cdq.info :as info]
            [cdq.stage :as stage]))

(defn do! [[_ cell item]
           {:keys [ctx/textures
                   ctx/stage]
            :as ctx}]
  (stage/set-item! stage cell
                   {:texture-region (image/texture-region (:entity/image item) textures)
                    :tooltip-text (fn [ctx]
                                    (info/generate (:ctx/info ctx) item ctx))})
  nil)
