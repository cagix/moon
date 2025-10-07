(ns cdq.ui.editor.map-widget-table
  (:require [cdq.ui.editor.schema :as schema])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)))

(defn get-value [table schemas]
  (into {}
        (for [widget (filter (comp vector? Actor/.getUserObject) (Group/.getChildren table))
              :let [[k _] (Actor/.getUserObject widget)]]
          [k (schema/value (get schemas k) widget schemas)])))
