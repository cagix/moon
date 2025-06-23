(ns cdq.dev.data-view
  (:require [gdx.ui :as ui]))

(defn- k->label-str [k]
  (str "[LIGHT_GRAY]:"
       (when-let [ns (namespace k)] (str ns "/"))
       "[][WHITE]"
       (name k)
       "[]"))

(defn- generate-table [m]
  (ui/table {:rows (for [[k v] (sort-by key m)]
                     [{:actor {:actor/type :actor.type/label
                               :label/text (k->label-str k)}}
                      {:actor {:actor/type :actor.type/label
                               :label/text (str (class v))}}])}))

(defn- scroll-pane-cell [rows width height]
  (let [;viewport (:ui-viewport ctx/graphics)
        table (ui/table {:rows rows
                         :cell-defaults {:pad 1}
                         :pack? true})
        scroll-pane (ui/scroll-pane table)]
    {:actor scroll-pane
     :width  width ; (- (:viewport/width viewport) 100) ; (+ 100 (/ (:viewport/width viewport) 2))
     :height height ; (- (:viewport/height viewport) 200) ; (- (:viewport/height viewport) 50) #_(min (- (:height viewport) 50) (height table))
     }))

(defn- scroll-pane-window [title content width height]
  (ui/window {:title title
              :close-button? true
              :close-on-escape? true
              :center? true
              :rows [[(scroll-pane-cell [[content]]
                                        width
                                        height)]]
              :pack? true}))

(defn table-view-window [title m width height]
  {:pre [(map? m)]}
  (scroll-pane-window title
                      (generate-table m)
                      width
                      height))
