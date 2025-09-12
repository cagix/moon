(ns cdq.render.clear-screen
  (:require [cdq.gdx.graphics :as graphics]
            [clojure.gdx.graphics.color :as color]))

(defn do!
  [{:keys [ctx/graphics]}]
  (graphics/clear! graphics color/black))
