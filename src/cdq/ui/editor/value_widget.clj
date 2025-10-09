(ns cdq.ui.editor.value-widget
  (:require [cdq.ui.editor.schema :as schema])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn build [ctx schema k v]
  (let [widget (schema/create schema v ctx)]
    ; FIXME assert no user object !
    (Actor/.setUserObject widget [k v])
    widget))
