(ns cdq.dev.data-view
  (:require [gdl.ui.stage :as stage]
            [gdx.ui :as ui]))

(defn- k->label-str [k]
  (str "[LIGHT_GRAY]:"
       (when-let [ns (namespace k)] (str ns "/"))
       "[][WHITE]"
       (name k)
       "[]"))

(defn- v->text [v]
  (cond
   (or (keyword? v)
       (number? v)
       (boolean? v)
       (string? v))
   (str "[GOLD]" v "[]")

   :else
   (str (class v))))

(declare table-view-window)

; TODO isn't there a clojure data browser thingy library

(defn- v->actor [v]
  (if (map? v)
    (ui/text-button "Map"
                    (fn [_actor {:keys [ctx/stage]}]
                      (stage/add! stage (table-view-window {:title "title"
                                                            :data v
                                                            :width 500
                                                            :height 500
                                                            }))
                      )
                    )
    {:actor/type :actor.type/label
     :label/text (v->text v)}))

(defn table-view-window [{:keys [title data width height]}]
  {:pre [(map? data)]}
  (let [scroll-pane-table (ui/table {:rows (for [[k v] (sort-by key data)]
                                             [{:actor {:actor/type :actor.type/label
                                                       :label/text (k->label-str k)}}
                                              {:actor (v->actor v)}])})
        scroll-pane-cell (let [;viewport (:ui-viewport ctx/graphics)
                               table (ui/table {:rows [[scroll-pane-table]]
                                                :cell-defaults {:pad 1}
                                                :pack? true})
                               scroll-pane (ui/scroll-pane table)]
                           {:actor scroll-pane
                            :width  width ; (- (:viewport/width viewport) 100) ; (+ 100 (/ (:viewport/width viewport) 2))
                            :height height ; (- (:viewport/height viewport) 200) ; (- (:viewport/height viewport) 50) #_(min (- (:height viewport) 50) (height table))
                            })]
    (ui/window {:title title
                :close-button? true
                :close-on-escape? true
                :center? true
                :rows [[scroll-pane-cell]]
                :pack? true})))
