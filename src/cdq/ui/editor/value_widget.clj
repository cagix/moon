(ns cdq.ui.editor.value-widget
  (:require [cdq.db.schema :as schema]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.actor :as actor]))

(defn build [ctx schema k v]
  (let [widget (schema/create schema v ctx)
        widget (if (instance? com.badlogic.gdx.scenes.scene2d.Actor widget)
                 widget
                 (scene2d/build widget))]
    ; FIXME assert no user object !
    (actor/set-user-object! widget [k v])
    widget))
