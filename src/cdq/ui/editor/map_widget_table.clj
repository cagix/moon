(ns cdq.ui.editor.map-widget-table
  (:require [cdq.ui.editor.schema :as schema]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]))

(defn get-value [table schemas]
  (into {}
        (for [widget (filter (comp vector? actor/user-object) (group/children table))
              :let [[k _] (actor/user-object widget)]]
          [k (schema/value (get schemas k) widget schemas)])))
