(ns cdq.create.colors
  (:require [clojure.gdx.graphics.colors :as colors]))

(defn do!
  [_ctx params]
  (colors/put! params)
  nil)
