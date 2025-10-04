(ns gdl.scene2d.build.group
  (:require [com.badlogic.gdx.scenes.scene2d.group :as group]
            [clojure.scene2d.group]
            [gdl.scene2d :as scene2d]))

(defmethod scene2d/build :actor.type/group [opts]
  (doto (group/create)
    (clojure.scene2d.group/set-opts! opts)))
