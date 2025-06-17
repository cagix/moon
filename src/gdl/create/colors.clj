(ns gdl.create.colors
  (:require [gdx.graphics.colors :as colors]))

(defn do! [ctx colors]
  (colors/put! colors)
  ctx)
