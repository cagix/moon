(ns cdq.ui.widget
  (:require [clojure.scene2d :as scene2d]
            [clojure.scene2d.vis-ui.window :as window]
            [clojure.scene2d.build.table :as table]
            [clojure.scene2d.vis-ui.text-button :as text-button]
            [clojure.vis-ui.label :as label])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn scroll-pane-cell [viewport-height rows]
  (let [table (table/create
               {:rows rows
                :actor/name "scroll-pane-table"
                :cell-defaults {:pad 5}
                :pack? true})]
    {:actor {:actor/type :actor.type/scroll-pane
             :actor/name "cdq.ui.widget.scroll-pane-table"
             :scroll-pane/actor table}
     :width  (+ (Actor/.getWidth table) 50)
     :height (min (- viewport-height 50)
                  (Actor/.getHeight table))}))

(defmethod scene2d/build :actor.type/scroll-pane-window
  [{:keys [viewport-height rows]}]
  (window/create
   {:title "Choose"
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
    (text-button/create
     {:text "Map"
      :on-clicked (fn [actor _ctx]
                    (.addActor (Actor/.getStage actor)
                               (data-viewer {:title "title"
                                             :data v
                                             :width 500
                                             :height 500})))})
    (label/create (v->text v))))

(defn data-viewer
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
                                    [{:actor (label/create label)}
                                     {:actor actor}])})
        scroll-pane-cell (let [table (table/create
                                      {:rows [[scroll-pane-table]]
                                       :cell-defaults {:pad 1}
                                       :pack? true})]
                           {:actor {:actor/type :actor.type/scroll-pane
                                    :actor/name "dbg scroll pane"
                                    :scroll-pane/actor table}
                            :width width ; (- (viewport/world-width viewport) 100) ; (+ 100 (/ (viewport/world-width viewport) 2))
                            :height height ; (- (viewport/world-height viewport) 200) ; (- (viewport/world-height viewport) 50) #_(min (- (:height viewport) 50) (height table))
                            })]
    (window/create {:title title
                    :close-button? true
                    :close-on-escape? true
                    :center? true
                    :rows [[scroll-pane-cell]]
                    :pack? true})))

(defmethod scene2d/build :actor.type/data-viewer [opts]
  (data-viewer opts))
