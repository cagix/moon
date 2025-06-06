(ns clojure.gdx.graphics.colors
  (:require [clojure.gdx.graphics.color :as color])
  (:import (com.badlogic.gdx.graphics Colors)))

(defn put! [name color]
  (Colors/put name (color/create color)))
