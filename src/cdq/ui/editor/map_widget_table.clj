(ns cdq.ui.editor.map-widget-table
  (:require [cdq.ui.editor.schema :as schema]
            [clojure.gdx.scene2d.actor :as actor])
  (:import (com.badlogic.gdx.scenes.scene2d Group)))

(defn get-value [table schemas]
  (into {}
        (for [widget (filter (comp vector? actor/user-object) (Group/.getChildren table))
              :let [[k _] (actor/user-object widget)]]
          [k (schema/value (get schemas k) widget schemas)])))
