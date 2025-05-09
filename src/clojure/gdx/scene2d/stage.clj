(ns clojure.gdx.scene2d.stage
  (:import (com.badlogic.gdx.scenes.scene2d Stage)))

(defn root [^Stage stage]
  (Stage/.getRoot stage))
