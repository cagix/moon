(ns cdq.ui.data-viewer
  (:require [cdq.ui.stage :as stage]
            [cdq.ui.table :as table]
            [cdq.ui.text-button :as text-button]
            [cdq.ui.window :as window]
            [clojure.vis-ui.scroll-pane :as scroll-pane]))

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

(declare create)

(defn- v->actor [v]
  (if (map? v)
    (text-button/create "Map"
                        (fn [_actor {:keys [ctx/stage]}]
                          (stage/add! stage (create {:title "title"
                                                     :data v
                                                     :width 500
                                                     :height 500}))))
    {:actor/type :actor.type/label
     :label/text (v->text v)}))

(defn create
  [{:keys [title
           data
           width
           height]}]
  {:pre [(map? data)]}
  (let [rows (for [[k v] (sort-by key data)]
               {:label (k->label-str k)
                :actor (v->actor v)})
        scroll-pane-table (table/create
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
