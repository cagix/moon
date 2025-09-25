(ns cdq.ui.widget
  (:require [gdl.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]))

(defn scroll-pane-cell [viewport-height rows]
  (let [table (scene2d/build
               {:actor/type :actor.type/table
                :rows rows
                :actor/name "scroll-pane-table"
                :cell-defaults {:pad 5}
                :pack? true})]
    {:actor {:actor/type :actor.type/scroll-pane
             :actor/name "cdq.ui.widget.scroll-pane-table"
             :scroll-pane/actor table}
     :width  (+ (actor/get-width table) 50)
     :height (min (- viewport-height 50)
                  (actor/get-height table))}))

(defmethod scene2d/build :actor.type/scroll-pane-window
  [{:keys [viewport-height rows]}]
  (scene2d/build
   {:actor/type :actor.type/window
    :title "Choose"
    :modal? true
    :close-button? true
    :center? true
    :close-on-escape? true
    :rows [[(scroll-pane-cell viewport-height rows)]]
    :pack? true}))

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

(declare data-viewer)

(defn- v->actor [v]
  (if (map? v)
    (scene2d/build {:actor/type :actor.type/text-button
                    :text "Map"
                    :on-clicked (fn [_actor {:keys [ctx/stage]}]
                                  (stage/add! stage (data-viewer {:title "title"
                                                                  :data v
                                                                  :width 500
                                                                  :height 500})))})
    {:actor/type :actor.type/label
     :label/text (v->text v)}))

(defn data-viewer
  [{:keys [title
           data
           width
           height]}]
  {:pre [(map? data)]}
  (let [rows (for [[k v] (sort-by key data)]
               {:label (k->label-str k)
                :actor (v->actor v)})
        scroll-pane-table (scene2d/build
                           {:actor/type :actor.type/table
                            :rows (for [{:keys [label actor]} rows]
                                    [{:actor {:actor/type :actor.type/label
                                              :label/text label}}
                                     {:actor actor}])})
        scroll-pane-cell (let [;viewport (:graphics/ui-viewport ctx)
                               table (scene2d/build
                                      {:actor/type :actor.type/table
                                       :rows [[scroll-pane-table]]
                                       :cell-defaults {:pad 1}
                                       :pack? true})]
                           {:actor {:actor/type :actor.type/scroll-pane
                                    :actor/name "dbg scroll pane"
                                    :scroll-pane/actor table}
                            :width width ; (- (:viewport/width viewport) 100) ; (+ 100 (/ (:viewport/width viewport) 2))
                            :height height ; (- (:viewport/height viewport) 200) ; (- (:viewport/height viewport) 50) #_(min (- (:height viewport) 50) (height table))
                            })]
    (scene2d/build {:actor/type :actor.type/window
                    :title title
                    :close-button? true
                    :close-on-escape? true
                    :center? true
                    :rows [[scroll-pane-cell]]
                    :pack? true})))

(defmethod scene2d/build :actor.type/data-viewer [opts]
  (data-viewer opts))
