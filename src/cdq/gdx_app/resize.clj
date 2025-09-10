(ns cdq.gdx-app.resize
  (:require [cdq.gdx.graphics :as graphics]))

(defn do!
  [ctx width height]
  (graphics/update-viewports! ctx width height))
