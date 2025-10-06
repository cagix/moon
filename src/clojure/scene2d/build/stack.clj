(ns clojure.scene2d.build.stack
  (:require [clojure.scene2d.widget-group :as widget-group]
            [com.badlogic.gdx.scenes.scene2d.ui.stack :as stack]
            [clojure.scene2d :as scene2d]))

(defmethod scene2d/build :actor.type/stack [opts]
  (doto (stack/create)
    (widget-group/set-opts! opts)))
