(ns gdx.graphics.colors
  (:require [gdx.graphics.color :as color])
  (:import (com.badlogic.gdx.graphics Colors)))

(defn put! [colors]
  (doseq [[name color-params] colors]
    (Colors/put name (color/->obj color-params))))
