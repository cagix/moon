(ns ^:no-doc moon.editor.visui
  (:require [gdl.input :refer [key-just-pressed?]]
            [moon.component :as component]
            [moon.db :as db]
            [moon.info :as info]
            [moon.property :as property]
            [moon.schema :as schema]
            [moon.editor.common :refer [component-order]]
            [moon.editor.malli :as malli]
            [moon.editor.utils :refer [scroll-pane-cell]]
            [moon.editor.widget :as widget]
            [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [moon.ui.error-window :refer [error-window!]]
            [moon.stage :as stage]
            [malli.core :as m]
            [malli.generator :as mg]))

; We are working with raw property data without edn->value and db/build
; otherwise at db/update! we would have to convert again from edn->value back to edn
; for example at images/relationships

; FIXME overview table not refreshed after changes in properties

(defn- apply-context-fn [window f]
  #(try (f)
        (a/remove! window)
        (catch Throwable t
          (error-window! t))))

(defn props->editor-window [props]
  (let [schema (schema/of (property/type props))
        window (ui/window {:title (str "[SKY]Property[]")
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        widget (widget/create schema props)
        save!   (apply-context-fn window #(db/update! (widget/value schema widget)))
        delete! (apply-context-fn window #(db/delete! (:property/id props)))]
    (ui/add-rows! window [[(scroll-pane-cell [[{:actor widget :colspan 2}]
                                              [{:actor (ui/text-button "Save [LIGHT_GRAY](ENTER)[]" save!)
                                                :center? true}
                                               {:actor (ui/text-button "Delete" delete!)
                                                :center? true}]])]])
    (ui/add-actor! window (ui/actor {:act (fn []
                                            (when (key-just-pressed? :enter)
                                              (save!)))}))
    (.pack window)
    (def editor-window window)
    window))

(defn- property-value []
 (let [window editor-window
       scroll-pane-table (.findActor (:scroll-pane window) "scroll-pane-table")
       m-widget-cell (first (seq (.getCells scroll-pane-table)))
       table (:map-widget scroll-pane-table)]
   (widget/value [:s/map] table)))

(defn- rebuild-editor-window []
  (let [prop-value (property-value)]
    (a/remove! editor-window)
    (stage/add! (props->editor-window prop-value))))

(defn property-editor-window [id]
  (props->editor-window (db/get-raw id)))

(defn- k->default-value [k]
  (let [schema (schema/of k)]
    (cond
     (#{:s/one-to-one :s/one-to-many} (schema/type schema)) nil

     ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

     :else (mg/generate (schema/form schema) {:size 3}))))

(defn- value-widget [[k v]]
  (let [widget (widget/create (schema/of k) v)]
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
    (when-let [doc (:editor/doc (component/meta k))]
      (ui/add-tooltip! label doc))
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
        remaining-ks (sort (remove (set (keys (widget/value schema map-widget-table)))
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
    (stage/add! window)))

(defn- interpose-f [f coll]
  (drop 1 (interleave (repeatedly f) coll)))

(defmethod widget/create :s/map [schema m]
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

(defmethod widget/value :s/map [_ table]
  (into {}
        (for [widget (filter value-widget? (ui/children table))
              :let [[k _] (a/id widget)]]
          [k (widget/value (schema/of k) widget)])))
