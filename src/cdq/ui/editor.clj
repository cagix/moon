(ns cdq.ui.editor
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.schema :as schema]
            [cdq.property :as property]
            [cdq.malli :as m]
            [cdq.ui.editor.scroll-pane :as scroll-pane]
            [cdq.ui.editor.overview-table :as overview-table]
            [cdq.ui.editor.widget :as widget]
            [cdq.ui.error-window :as error-window]
            [cdq.utils :as utils]
            [clojure.set :as set]
            [gdl.input :as input]
            [gdl.ui :as ui]))

(defn- apply-context-fn [window f]
  #(try (f)
        (ui/remove! window)
        (catch Throwable t
          (utils/pretty-pst t)
          (ui/add! ctx/stage (error-window/create t)))))

; We are working with raw property data without edn->value and build
; otherwise at update! we would have to convert again from edn->value back to edn
; for example at images/relationships
(defn- editor-window [props]
  (let [schema (get ctx/schemas (property/type props))
        window (ui/window {:title (str "[SKY]Property[]")
                           :id :property-editor-window
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        widget (widget/create schema props)
        save!   (apply-context-fn window #(do
                                           (alter-var-root #'ctx/db db/update (widget/value schema widget))
                                           (db/save! ctx/db)))
        delete! (apply-context-fn window #(do
                                           (alter-var-root #'ctx/db db/delete (:property/id props))
                                           (db/save! ctx/db)))]
    (ui/add-rows! window [[(scroll-pane/table-cell [[{:actor widget :colspan 2}]
                                                    [{:actor (ui/text-button "Save [LIGHT_GRAY](ENTER)[]" save!)
                                                      :center? true}
                                                     {:actor (ui/text-button "Delete" delete!)
                                                      :center? true}]])]])
    (.addActor window (ui/actor {:act (fn [_this _delta]
                                        (when (input/key-just-pressed? :enter)
                                          (save!)))}))
    (.pack window)
    window))

(defn- get-editor-window []
  (:property-editor-window ctx/stage))

(defn- window->property-value []
 (let [window (get-editor-window)
       scroll-pane-table (ui/find-actor (:scroll-pane window) "scroll-pane-table")
       m-widget-cell (first (seq (ui/cells scroll-pane-table)))
       table (:map-widget scroll-pane-table)]
   (widget/value [:s/map] table)))

(defn- rebuild-editor-window []
  (let [prop-value (window->property-value)]
    (ui/remove! (get-editor-window))
    (ui/add! ctx/stage (editor-window prop-value))))

(defn- value-widget [[k v]]
  (let [widget (widget/create (get ctx/schemas k) v)]
    (ui/set-user-object! widget [k v])
    widget))

(def ^:private value-widget? (comp vector? ui/user-object))

(defn- find-kv-widget [table k]
  (utils/find-first (fn [actor]
                      (and (ui/user-object actor)
                           (= k ((ui/user-object actor) 0))))
                    (ui/children table)))

(defn- attribute-label [k schema table]
  (let [label (ui/label ;(str "[GRAY]:" (namespace k) "[]/" (name k))
                        (name k))
        delete-button (when (m/optional? k (schema/malli-form schema ctx/schemas))
                        (ui/text-button "-"
                                        (fn []
                                          (ui/remove! (find-kv-widget table k))
                                          (rebuild-editor-window))))]
    (ui/table {:cell-defaults {:pad 2}
               :rows [[{:actor delete-button :left? true}
                       label]]})))

(def ^:private component-row-cols 3)

(defn- component-row [[k v] schema table]
  [{:actor (attribute-label k schema table)
    :right? true}
   (ui/vertical-separator-cell)
   {:actor (value-widget [k v])
    :left? true}])

(defn- horiz-sep []
  [(ui/horizontal-separator-cell component-row-cols)])

(defn- k->default-value [k]
  (let [schema (get ctx/schemas k)]
    (cond
     (#{:s/one-to-one :s/one-to-many} (schema/type schema)) nil

     ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

     :else (m/generate (schema/malli-form schema ctx/schemas)
                       {:size 3}))))

(defn- choose-component-window [schema map-widget-table]
  (let [window (ui/window {:title "Choose"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        remaining-ks (sort (remove (set (keys (widget/value schema map-widget-table)))
                                   (m/map-keys (schema/malli-form schema ctx/schemas))))]
    (ui/add-rows!
     window
     (for [k remaining-ks]
       [(ui/text-button (name k)
                        (fn []
                          (.remove window)
                          (ui/add-rows! map-widget-table [(component-row
                                                           [k (k->default-value k)]
                                                           schema
                                                           map-widget-table)])
                          (rebuild-editor-window)))]))
    (.pack window)
    (ui/add! ctx/stage window)))

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

(defmethod widget/create :s/map [schema m]
  (let [table (ui/table {:cell-defaults {:pad 5}
                         :id :map-widget})
        component-rows (interpose-f horiz-sep
                          (map #(component-row % schema table)
                               (utils/sort-by-k-order property-k-sort-order
                                                      m)))
        colspan component-row-cols
        opt? (seq (set/difference (m/optional-keyset (schema/malli-form schema ctx/schemas))
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

(defmethod widget/value :s/map [_ table]
  (into {}
        (for [widget (filter value-widget? (ui/children table))
              :let [[k _] (ui/user-object widget)]]
          [k (widget/value (get ctx/schemas k) widget)])))

; FIXME overview table not refreshed after changes in properties

(defn- edit-property [id]
  (ui/add! ctx/stage (editor-window (db/get-raw ctx/db id))))

(defn open-editor-window! [property-type]
  (let [window (ui/window {:title "Edit"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true})]
    (ui/add! window (overview-table/create property-type edit-property))
    (.pack window)
    (ui/add! ctx/stage window)))
