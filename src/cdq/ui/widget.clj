(ns cdq.ui.widget
  (:require [cdq.utils :refer [with-err-str]]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui] ; load defmethods
            [clojure.gdx.scenes.scene2d.ui.table :as table]
            [clojure.repl]
            [clojure.vis-ui.scroll-pane :as scroll-pane]
            [clojure.vis-ui.widget :as widget]))

(defn scroll-pane-cell [viewport-height rows]
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

(defn scroll-pane-window [viewport-height rows]
  (widget/window {:title "Choose"
                  :modal? true
                  :close-button? true
                  :center? true
                  :close-on-escape? true
                  :rows [[(scroll-pane-cell viewport-height rows)]]
                  :pack? true}))

(defn error-window [throwable]
  (widget/window
   {:title "Error"
    :rows [[{:actor {:actor/type :actor.type/label
                     :label/text (binding [*print-level* 3]
                                   (with-err-str
                                     (clojure.repl/pst throwable)))}}]]
    :modal? true
    :close-button? true
    :close-on-escape? true
    :center? true
    :pack? true}))

(defn property-editor-window
  [{:keys [check-enter-to-save
           clicked-save-fn
           clicked-delete-fn
           save?
           scrollpane-height
           widget]}]
  (doto (widget/window {:title (str "[SKY]Property[]")
                        :id :property-editor-window
                        :modal? true
                        :close-button? true
                        :center? true
                        :close-on-escape? true
                        :rows [[(scroll-pane-cell scrollpane-height
                                                  [[{:actor widget :colspan 2}]
                                                   [{:actor (widget/text-button "Save [LIGHT_GRAY](ENTER)[]" clicked-save-fn)
                                                     :center? true}
                                                    {:actor (widget/text-button "Delete" clicked-delete-fn)
                                                     :center? true}]])]]
                        :actors [{:actor/type :actor.type/actor
                                  :act (fn [actor _delta {:keys [ctx/input] :as ctx}]
                                         (when (save? input)
                                           (clicked-save-fn actor ctx)))}]
                        :cell-defaults {:pad 5}})
    (.pack)))

(defmethod actor/build :actor.type/property-editor [opts]
  (property-editor-window opts))

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
    (widget/text-button "Map"
                        (fn [_actor {:keys [ctx/stage]}]
                          (stage/add! stage (data-viewer {:title "title"
                                                          :data v
                                                          :width 500
                                                          :height 500}))))
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
        scroll-pane-table (widget/table
                           {:rows (for [{:keys [label actor]} rows]
                                    [{:actor {:actor/type :actor.type/label
                                              :label/text label}}
                                     {:actor actor}])})
        scroll-pane-cell (let [;viewport (:ctx/ui-viewport ctx)
                               table (widget/table
                                      {:rows [[scroll-pane-table]]
                                       :cell-defaults {:pad 1}
                                       :pack? true})]
                           {:actor (scroll-pane/create table)
                            :width width ; (- (:viewport/width viewport) 100) ; (+ 100 (/ (:viewport/width viewport) 2))
                            :height height ; (- (:viewport/height viewport) 200) ; (- (:viewport/height viewport) 50) #_(min (- (:height viewport) 50) (height table))
                            })]
    (widget/window {:title title
                    :close-button? true
                    :close-on-escape? true
                    :center? true
                    :rows [[scroll-pane-cell]]
                    :pack? true})))

(defmethod actor/build :actor.type/data-viewer [opts]
  (data-viewer opts))
