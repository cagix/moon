(ns cdq.gdx-app.resize
  (:require [cdq.gdx.graphics :as graphics]))

(defn do!
  [{:keys [ctx/graphics]} width height]
  (graphics/update-viewports! graphics width height))
