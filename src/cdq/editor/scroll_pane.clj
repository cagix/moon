(ns cdq.editor.scroll-pane
  (:require [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.vis-ui.scroll-pane :as scroll-pane]
            [clojure.vis-ui.widget :as widget]))

(defn table-cell [viewport-height rows]
  (let [table (widget/table
               {:rows rows
                :name "scroll-pane-table"
                :cell-defaults {:pad 5}
                :pack? true})]
    {:actor (doto (scroll-pane/create table)
              (actor/set-user-object! :scroll-pane))
     :width  (+ (.getWidth table) 50)
     :height (min (- viewport-height 50)
                  (.getHeight table))}))

(defn choose-window [viewport-height rows]
  (widget/window {:title "Choose"
                  :modal? true
                  :close-button? true
                  :center? true
                  :close-on-escape? true
                  :rows [[(table-cell viewport-height rows)]]
                  :pack? true}))
