(ns cdq.ui.editor.map-widget-table
  (:require [cdq.ui.editor.schema :as schema]
            [clojure.gdx.scenes.scene2d.group :as group])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn get-value [table schemas]
  (into {}
        (for [widget (filter (comp vector? Actor/.getUserObject) (group/children table))
              :let [[k _] (Actor/.getUserObject widget)]]
          [k (schema/value (get schemas k) widget schemas)])))
