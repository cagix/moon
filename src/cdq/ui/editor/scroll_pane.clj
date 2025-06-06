(ns cdq.ui.editor.scroll-pane
  (:require [gdl.ui :as ui]))

(defn table-cell [viewport-height rows]
  (let [table (ui/table {:rows rows
                         :name "scroll-pane-table"
                         :cell-defaults {:pad 5}
                         :pack? true})]
    {:actor (ui/scroll-pane table)
     :width  (+ (.getWidth table) 50)
     :height (min (- viewport-height 50)
                  (.getHeight table))}))

(defn choose-window [viewport-height rows]
  (ui/window {:title "Choose"
              :modal? true
              :close-button? true
              :center? true
              :close-on-escape? true
              :rows [[(table-cell viewport-height rows)]]
              :pack? true}))
