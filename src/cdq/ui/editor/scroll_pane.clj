(ns cdq.ui.editor.scroll-pane
  (:require [cdq.ctx :as ctx]
            [gdl.ui :as ui]))

(defn table-cell [rows]
  (let [table (ui/table {:rows rows
                         :name "scroll-pane-table"
                         :cell-defaults {:pad 5}
                         :pack? true})]
    {:actor (ui/scroll-pane table)
     :width  (+ (.getWidth table) 50)
     :height (min (- (:height ctx/ui-viewport) 50)
                  (.getHeight table))}))

(defn choose-window [rows]
  (ui/window {:title "Choose"
              :modal? true
              :close-button? true
              :center? true
              :close-on-escape? true
              :rows [[(table-cell rows)]]
              :pack? true}))
