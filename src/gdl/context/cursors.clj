(ns gdl.context.cursors
  (:require [clojure.gdx :as gdx]))

(defn dispose [[_ cursors]]
  (run! gdx/dispose (vals cursors)))
