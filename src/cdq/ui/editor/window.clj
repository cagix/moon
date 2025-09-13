(ns cdq.ui.editor.window
  (:require [cdq.schema :as schema]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.group :as group]))

(defn create
  [{:keys [actors rows]}]
  {:actor/type :actor.type/window
   :title "[SKY]Property[]"
   :actor/name "cdq.ui.editor.window"
   :modal? true
   :close-button? true
   :center? true
   :close-on-escape? true
   :group/actors actors
   :rows rows
   :cell-defaults {:pad 5}
   :pack? true})

(defn map-widget-property-values [table schemas]
  (into {}
        (for [widget (filter (comp vector? actor/user-object) (group/children table))
              :let [[k _] (actor/user-object widget)]]
          [k (schema/value (get schemas k) widget schemas)])))
