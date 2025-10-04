(ns gdl.scene2d.build.horizontal-group
  (:require [clojure.gdx.scenes.scene2d.ui.horizontal-group :as horizontal-group]
            [gdl.scene2d :as scene2d]
            [gdl.scene2d.group :as group]))

(defmethod scene2d/build :actor.type/horizontal-group [opts]
  (doto (horizontal-group/create opts)
    (group/set-opts! opts)))
