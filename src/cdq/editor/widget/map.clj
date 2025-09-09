(ns cdq.editor.widget.map
  (:require [cdq.editor-window :as editor-window]
            [cdq.schemas :as schemas]
            [cdq.editor.widget :as editor-widget]
            [cdq.utils :as utils]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui.table :as table]
            [clojure.set :as set]
            [clojure.vis-ui.separator :as separator]
            [clojure.vis-ui.widget :as widget]))

(defn- k->label-text [k]
  (name k) ;(str "[GRAY]:" (namespace k) "[]/" (name k))
  )

(defn- remove-component-button [k table]
  (widget/text-button "-"
                      (fn [_actor ctx]
                        (actor/remove! (utils/find-first (fn [actor]
                                                           (and (actor/user-object actor)
                                                                (= k ((actor/user-object actor) 0))))
                                                         (group/children table)))
                        (editor-window/rebuild! ctx))))

(defn- label-cell
  [{:keys [display-remove-component-button? k table label-text]}]
  {:actor {:actor/type :actor.type/table
           :cell-defaults {:pad 2}
           :rows [[{:actor (when display-remove-component-button?
                             (remove-component-button k table))
                    :left? true}
                   {:actor {:actor/type :actor.type/label
                            :label/text label-text}}]]}
   :right? true})

(defn- vertical-separator-cell []
  {:actor (separator/vertical)
   :pad-top 2
   :pad-bottom 2
   :fill-y? true
   :expand-y? true})

(defn- component-row [editor-widget k map-schema schemas table]
  [(label-cell {:display-remove-component-button? (schemas/optional-k? schemas map-schema k)
                :k k
                :table table
                :label-text (k->label-text k)})
   (vertical-separator-cell)
   {:actor editor-widget
    :left? true}])

(defn- open-add-component-window! [{:keys [ctx/db
                                           ctx/stage]}
                                   schema
                                   map-widget-table]
  (let [schemas (:schemas db)
        window (widget/window {:title "Choose"
                               :modal? true
                               :close-button? true
                               :center? true
                               :close-on-escape? true
                               :cell-defaults {:pad 5}})
        remaining-ks (sort (remove (set (keys (editor-widget/value schema nil map-widget-table schemas)))
                                   (schemas/map-keys schemas schema)))]
    (table/add-rows!
     window
     (for [k remaining-ks]
       [(widget/text-button (name k)
                            (fn [_actor ctx]
                              (.remove window)
                              (table/add-rows! map-widget-table [(component-row (editor-widget/build ctx
                                                                                                     (get schemas k)
                                                                                                     k
                                                                                                     (schemas/k->default-value schemas k))
                                                                                k
                                                                                schema
                                                                                schemas
                                                                                map-widget-table)])
                              (editor-window/rebuild! ctx)))]))
    (.pack window)
    (stage/add! stage window)))

(defn- horiz-sep [colspan]
  (fn []
    [{:actor (separator/horizontal)
      :pad-top 2
      :pad-bottom 2
      :colspan colspan
      :fill-x? true
      :expand-x? true}]))

(defn- interpose-f [f coll]
  (drop 1 (interleave (repeatedly f) coll)))

(defn create [schema  _attribute m {:keys [ctx/db
                                           ctx/config] :as ctx}]
  (let [k-sort-order (:property-k-sort-order (:cdq.editor.widget.map config))
        table (widget/table
               {:cell-defaults {:pad 5}
                :id :map-widget})
        colspan 3
        component-rows (interpose-f (horiz-sep colspan)
                                    (map (fn [[k v]]
                                           (component-row (editor-widget/build ctx (get (:schemas db) k) k v)
                                                          k
                                                          schema
                                                          (:schemas db)
                                                          table))
                                         (utils/sort-by-k-order k-sort-order m)))
        opt? (seq (set/difference (schemas/optional-keyset (:schemas db) schema)
                                  (set (keys m))))]
    (table/add-rows!
     table
     (concat [(when opt?
                [{:actor (widget/text-button "Add component"
                                             (fn [_actor ctx]
                                               (open-add-component-window! ctx schema table)))
                  :colspan colspan}])]
             [(when opt?
                [{:actor (separator/horizontal)
                  :pad-top 2
                  :pad-bottom 2
                  :colspan colspan
                  :fill-x? true
                  :expand-x? true}])]
             component-rows))
    table))

(defn value [_  _attribute table schemas]
  (into {}
        (for [widget (filter (comp vector? actor/user-object) (group/children table))
              :let [[k _] (actor/user-object widget)]]
          [k (editor-widget/value (get schemas k) k widget schemas)])))
