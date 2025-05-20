(ns cdq.ui.editor.widget.map
  (:require [cdq.ctx :as ctx]
            [cdq.schema :as schema]
            [cdq.malli :as m]
            [cdq.ui.editor]
            [cdq.ui.editor.widget :as widget]
            [cdq.utils :as utils]
            [clojure.set :as set]
            [gdl.ui :as ui]))

(def ^:private property-k-sort-order
  [:property/id
   :property/pretty-name
   :entity/image
   :entity/animation
   :entity/species
   :creature/level
   :entity/body
   :item/slot
   :projectile/speed
   :projectile/max-range
   :projectile/piercing?
   :skill/action-time-modifier-key
   :skill/action-time
   :skill/start-action-sound
   :skill/cost
   :skill/cooldown])

(defn- window->property-value [property-editor-window]
 (let [window property-editor-window
       scroll-pane-table (ui/find-actor (:scroll-pane window) "scroll-pane-table")
       m-widget-cell (first (seq (ui/cells scroll-pane-table)))
       table (:map-widget scroll-pane-table)]
   (widget/value [:s/map] table)))

(defn- rebuild-editor-window! [stage]
  (let [window (:property-editor-window stage)
        prop-value (window->property-value window)]
    (ui/remove! window)
    (ui/add! stage (cdq.ui.editor/editor-window prop-value))))

(defn- find-kv-widget [table k]
  (utils/find-first (fn [actor]
                      (and (ui/user-object actor)
                           (= k ((ui/user-object actor) 0))))
                    (ui/children table)))

(def ^:private component-row-cols 3)

(defn- component-row [[k v] map-schema schemas table]
  [{:actor (ui/table {:cell-defaults {:pad 2}
                      :rows [[{:actor (when (m/optional? k (schema/malli-form map-schema schemas))
                                        (ui/text-button "-"
                                                        (fn []
                                                          (ui/remove! (find-kv-widget table k))
                                                          (rebuild-editor-window! ctx/stage))))
                               :left? true}
                              (ui/label ;(str "[GRAY]:" (namespace k) "[]/" (name k))
                                        (name k))]]})
    :right? true}
   (ui/vertical-separator-cell)
   {:actor (let [widget (widget/create (get schemas k) v)]
             (ui/set-user-object! widget [k v])
             widget)
    :left? true}])

(defn- k->default-value [schemas k]
  (let [schema (get schemas k)]
    (cond
     (#{:s/one-to-one :s/one-to-many} (schema/type schema)) nil

     ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

     :else (m/generate (schema/malli-form schema schemas)
                       {:size 3}))))

(defn- choose-component-window [schema map-widget-table]
  (let [window (ui/window {:title "Choose"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        remaining-ks (sort (remove (set (keys (widget/value schema map-widget-table)))
                                   (m/map-keys (schema/malli-form schema (:schemas ctx/db)))))]
    (ui/add-rows!
     window
     (for [k remaining-ks]
       [(ui/text-button (name k)
                        (fn []
                          (.remove window)
                          (ui/add-rows! map-widget-table [(component-row [k (k->default-value (:schemas ctx/db) k)]
                                                                         schema
                                                                         (:schemas ctx/db)
                                                                         map-widget-table)])
                          (rebuild-editor-window! ctx/stage)))]))
    (.pack window)
    (ui/add! ctx/stage window)))

(defn- horiz-sep []
  [(ui/horizontal-separator-cell component-row-cols)])

(defn- interpose-f [f coll]
  (drop 1 (interleave (repeatedly f) coll)))

(defmethod widget/create :s/map [schema m]
  (let [table (ui/table {:cell-defaults {:pad 5}
                         :id :map-widget})
        component-rows (interpose-f horiz-sep
                                    (map (fn [[k v]]
                                           (component-row [k v]
                                                          schema
                                                          (:schemas ctx/db)
                                                          table))
                               (utils/sort-by-k-order property-k-sort-order
                                                      m)))
        colspan component-row-cols
        opt? (seq (set/difference (m/optional-keyset (schema/malli-form schema (:schemas ctx/db)))
                                  (set (keys m))))]
    (ui/add-rows!
     table
     (concat [(when opt?
                [{:actor (ui/text-button "Add component" #(choose-component-window schema table))
                  :colspan colspan}])]
             [(when opt?
                [(ui/horizontal-separator-cell colspan)])]
             component-rows))
    table))

(def ^:private value-widget? (comp vector? ui/user-object))

(defmethod widget/value :s/map [_ table]
  (into {}
        (for [widget (filter value-widget? (ui/children table))
              :let [[k _] (ui/user-object widget)]]
          [k (widget/value (get (:schemas ctx/db) k) widget)])))
