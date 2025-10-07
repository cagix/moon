(ns cdq.ui.editor.value-widget
  (:require [cdq.ui.editor.schema :as schema]
            [clojure.scene2d :as scene2d])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn build [ctx schema k v]
  (let [widget (schema/create schema v ctx)
        widget (if (instance? com.badlogic.gdx.scenes.scene2d.Actor widget)
                 widget
                 (scene2d/build widget))]
    ; FIXME assert no user object !
    (Actor/.setUserObject widget [k v])
    widget))
