(ns clojure.scene2d.build.stack
  (:require [clojure.scene2d.widget-group :as widget-group])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Stack)))

(defn create [opts]
  (doto (Stack.)
    (widget-group/set-opts! opts)))
