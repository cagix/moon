(ns clojure.scene2d.build.stack
  (:require [clojure.scene2d.widget-group :as widget-group]
            [clojure.scene2d :as scene2d])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Stack)))

(defmethod scene2d/build :actor.type/stack [opts]
  (doto (Stack.)
    (widget-group/set-opts! opts)))
