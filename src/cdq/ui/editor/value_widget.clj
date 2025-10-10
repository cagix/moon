(ns cdq.ui.editor.value-widget
  (:require [clojure.gdx.scene2d.actor :as actor]
            [cdq.ui.editor.schema :as schema]))

(defn build [ctx schema k v]
  (let [widget (schema/create schema v ctx)]
    ; FIXME assert no user object !
    (actor/set-user-object! widget [k v])
    widget))
