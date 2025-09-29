(ns gdl.scene2d.ui.stack
  (:require [gdl.scene2d :as scene2d]
            [gdl.scene2d.ui.widget-group :as widget-group])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Stack)))

(defmethod scene2d/build :actor.type/stack [opts]
  (doto (Stack.)
    (widget-group/set-opts! opts)))
