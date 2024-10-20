(ns editor.utils
  (:require [gdx.graphics :as g]
            [gdx.ui :as ui]))

(defn scroll-pane-cell [rows]
  (let [table (ui/table {:rows rows
                         :name "scroll-pane-table"
                         :cell-defaults {:pad 5}
                         :pack? true})]
    {:actor (ui/scroll-pane table)
     :width  (+ (.getWidth table) 50)
     :height (min (- (g/gui-viewport-height) 50)
                  (.getHeight table))}))

(defn scrollable-choose-window [rows]
  (ui/window {:title "Choose"
              :modal? true
              :close-button? true
              :center? true
              :close-on-escape? true
              :rows [[(scroll-pane-cell rows)]]
              :pack? true}))
