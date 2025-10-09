(ns cdq.game.render.update-mouse
  (:require [cdq.graphics.ui-viewport :as ui-viewport]
            [cdq.graphics.world-viewport :as world-viewport]
            [clojure.gdx :as gdx]))

(defn step
  [{:keys [ctx/gdx
           ctx/graphics]
    :as ctx}]
  (let [mp (gdx/mouse-position gdx)]
    (-> ctx
        (assoc-in [:ctx/graphics :graphics/world-mouse-position] (world-viewport/unproject graphics mp))
        (assoc-in [:ctx/graphics :graphics/ui-mouse-position] (ui-viewport/unproject graphics mp)))))
