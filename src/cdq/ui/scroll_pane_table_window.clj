(ns cdq.ui.scroll-pane-table-window
  (:require [cdq.ui.table :as table]
            [cdq.ui.window :as window]
            [clojure.vis-ui.scroll-pane :as scroll-pane]))

(defn create [{:keys [title rows width height]}]
  (let [scroll-pane-table (table/create
                           {:rows (for [{:keys [label actor]} rows]
                                    [{:actor {:actor/type :actor.type/label
                                              :label/text label}}
                                     {:actor actor}])})
        scroll-pane-cell (let [;viewport (:ctx/ui-viewport ctx)
                               table (table/create
                                      {:rows [[scroll-pane-table]]
                                       :cell-defaults {:pad 1}
                                       :pack? true})]
                           {:actor (scroll-pane/create table)
                            :width width ; (- (:viewport/width viewport) 100) ; (+ 100 (/ (:viewport/width viewport) 2))
                            :height height ; (- (:viewport/height viewport) 200) ; (- (:viewport/height viewport) 50) #_(min (- (:height viewport) 50) (height table))
                            })]
    (window/create {:title title
                    :close-button? true
                    :close-on-escape? true
                    :center? true
                    :rows [[scroll-pane-cell]]
                    :pack? true})))
