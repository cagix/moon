(ns ^:no-doc moon.schema.map
  (:require [moon.schema :as schema]
            [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [gdl.utils :refer [index-of]]
            [malli.generator :as mg]
            [moon.app :refer [stage add-actor]]
            [moon.editor.malli :as malli]
            [moon.editor.property :as widgets.property]
            [moon.editor.scrollpane :refer [scroll-pane-cell]]))

(defn- attribute-form
  "Can define keys as just keywords or with schema-props like [:foo {:optional true}]."
  [ks]
  (for [k ks
        :let [k? (keyword? k)
              schema-props (if k? nil (k 1))
              k (if k? k (k 0))]]
    (do
     (assert (keyword? k))
     (assert (or (nil? schema-props) (map? schema-props)) (pr-str ks))
     [k schema-props (schema/form-of k)])))

(defn- map-form [ks]
  (apply vector :map {:closed true} (attribute-form ks)))

(defmethod schema/form :s/map [[_ ks]]
  (map-form ks))

(defmethod schema/form :s/map-optional [[_ ks]]
  (map-form (map (fn [k] [k {:optional true}]) ks)))

(defn- namespaced-ks [ns-name-k]
  (filter #(= (name ns-name-k) (namespace %))
          (keys schema/schemas)))

(defmethod schema/form :s/components-ns [[_ ns-name-k]]
  (schema/form [:s/map-optional (namespaced-ks ns-name-k)]))

(defn- editor-window []
  (:property-editor-window (stage)))

(defn- property-value []
 (let [window (editor-window)
       scroll-pane-table (.findActor (:scroll-pane window) "scroll-pane-table")
       m-widget-cell (first (seq (.getCells scroll-pane-table)))
       table (:map-widget scroll-pane-table)]
   (schema/widget-value [:s/map] table)))

(defn- rebuild-editor-window []
  (let [prop-value (property-value)]
    (a/remove! (editor-window))
    (add-actor (widgets.property/editor-window prop-value))))

(defn- k->default-value [k]
  (let [schema (schema/of k)]
    (cond
     (#{:s/one-to-one :s/one-to-many} (schema/type schema)) nil

     ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

     :else (mg/generate (schema/form schema) {:size 3}))))

(defn- value-widget [[k v]]
  (let [widget (schema/widget (schema/of k) v)]
    (.setUserObject widget [k v])
    widget))

(def ^:private value-widget? (comp vector? a/id))

(defn- find-kv-widget [table k]
  (gdl.utils/find-first (fn [actor]
                           (and (a/id actor)
                                (= k ((a/id actor) 0))))
                         (ui/children table)))

(defn- attribute-label [k m-schema table]
  (let [label (ui/label ;(str "[GRAY]:" (namespace k) "[]/" (name k))
                        (name k))
        delete-button (when (malli/optional? k m-schema)
                        (ui/text-button "-"
                                        (fn []
                                          (a/remove! (find-kv-widget table k))
                                          (rebuild-editor-window))))]
    (ui/table {:cell-defaults {:pad 2}
               :rows [[{:actor delete-button :left? true}
                       label]]})))

(def ^:private component-row-cols 3)

(defn- component-row [[k v] m-schema table]
  [{:actor (attribute-label k m-schema table)
    :right? true}
   (ui/vertical-separator-cell)
   {:actor (value-widget [k v])
    :left? true}])

(defn- horiz-sep []
  [(ui/horizontal-separator-cell component-row-cols)])

(defn- choose-component-window [schema map-widget-table]
  (let [window (ui/window {:title "Choose"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        malli-form (schema/form schema)
        remaining-ks (sort (remove (set (keys (schema/widget-value schema map-widget-table)))
                                   (malli/map-keys malli-form)))]
    (ui/add-rows!
     window
     (for [k remaining-ks]
       [(ui/text-button (name k)
                        (fn []
                          (a/remove! window)
                          (ui/add-rows! map-widget-table [(component-row
                                                           [k (k->default-value k)]
                                                           malli-form
                                                           map-widget-table)])
                          (rebuild-editor-window)))]))
    (.pack window)
    (add-actor window)))

(defn- interpose-f [f coll]
  (drop 1 (interleave (repeatedly f) coll)))

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

(defn- component-order [[k _v]]
  (or (index-of k property-k-sort-order) 99))

(defmethod schema/widget :s/map [schema m]
  (let [table (ui/table {:cell-defaults {:pad 5}
                         :id :map-widget})
        component-rows (interpose-f horiz-sep
                          (map #(component-row % (schema/form schema) table)
                               (sort-by component-order m)))
        colspan component-row-cols
        opt? (malli/optional-keys-left (schema/form schema) m)]
    (ui/add-rows!
     table
     (concat [(when opt?
                [{:actor (ui/text-button "Add component" #(choose-component-window schema table))
                  :colspan colspan}])]
             [(when opt?
                [(ui/horizontal-separator-cell colspan)])]
             component-rows))
    table))

(defmethod schema/widget-value :s/map [_ table]
  (into {}
        (for [widget (filter value-widget? (ui/children table))
              :let [[k _] (a/id widget)]]
          [k (schema/widget-value (schema/of k) widget)])))
