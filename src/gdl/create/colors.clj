(ns gdl.create.colors
  (:require [clojure.gdx.graphics.colors :as colors]))

(defn do! [ctx colors]
  (colors/put! colors)
  ctx)
