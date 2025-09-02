(ns cdq.ui.editor.widget.map
  (:require [cdq.editor]
            [cdq.schemas :as schemas]
            [cdq.ui.editor.widget :as widget]
            [cdq.utils :as utils]
            [clojure.set :as set]
            [cdq.ui.actor :as actor]
            [cdq.ui.group :as group]
            [cdq.ui.stage :as stage]
            [cdq.ui.table :as table]
            [cdq.gdx.ui :as ui]
            [cdq.ui.separator :as separator]))

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

(defn- window->property-value [property-editor-window schemas]
 (let [window property-editor-window
       scroll-pane-table (group/find-actor (:scroll-pane window) "scroll-pane-table")
       m-widget-cell (first (seq (table/cells scroll-pane-table)))
       table (:map-widget scroll-pane-table)]
   (widget/value [:s/map] nil table schemas)))

(defn- rebuild-editor-window! [{:keys [ctx/db
                                       ctx/stage] :as ctx}]
  (let [window (:property-editor-window stage)
        prop-value (window->property-value window (:schemas db))]
    (actor/remove! window)
    (cdq.editor/open-property-editor-window! ctx prop-value)))

(defn- find-kv-widget [table k]
  (utils/find-first (fn [actor]
                      (and (actor/user-object actor)
                           (= k ((actor/user-object actor) 0))))
                    (group/children table)))

(def ^:private component-row-cols 3)

(defn- component-row [ctx [k v] map-schema schemas table]
  [{:actor {:actor/type :actor.type/table
            :cell-defaults {:pad 2}
            :rows [[{:actor (when (schemas/optional-k? schemas map-schema k)
                              (ui/text-button "-"
                                              (fn [_actor ctx]
                                                (actor/remove! (find-kv-widget table k))
                                                (rebuild-editor-window! ctx))))
                     :left? true}
                    {:actor {:actor/type :actor.type/label
                             :label/text (name k) ;(str "[GRAY]:" (namespace k) "[]/" (name k))
                             }}]]}
    :right? true}
   (separator/vertical)
   {:actor (let [widget (cdq.ui.actor/construct? (widget/create (get schemas k) k v ctx))]
             (actor/set-user-object! widget [k v])
             widget)
    :left? true}])

(defn- open-add-component-window! [{:keys [ctx/db
                                           ctx/stage]}
                                   schema
                                   map-widget-table]
  (let [schemas (:schemas db)
        window (ui/window {:title "Choose"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        remaining-ks (sort (remove (set (keys (widget/value schema nil map-widget-table schemas)))
                                   (schemas/map-keys schemas schema)))]
    (table/add-rows!
     window
     (for [k remaining-ks]
       [(ui/text-button (name k)
                        (fn [_actor ctx]
                          (.remove window)
                          (table/add-rows! map-widget-table [(component-row ctx
                                                                            [k (schemas/k->default-value schemas k)]
                                                                            schema
                                                                            schemas
                                                                            map-widget-table)])
                          (rebuild-editor-window! ctx)))]))
    (.pack window)
    (stage/add! stage window)))

(defn- horiz-sep []
  [(separator/horizontal component-row-cols)])

(defn- interpose-f [f coll]
  (drop 1 (interleave (repeatedly f) coll)))

(defmethod widget/create :s/map [schema  _attribute m {:keys [ctx/db] :as ctx}]
  (let [table (ui/table {:cell-defaults {:pad 5}
                         :id :map-widget})
        component-rows (interpose-f horiz-sep
                                    (map (fn [[k v]]
                                           (component-row ctx
                                                          [k v]
                                                          schema
                                                          (:schemas db)
                                                          table))
                               (utils/sort-by-k-order property-k-sort-order
                                                      m)))
        colspan component-row-cols
        opt? (seq (set/difference (schemas/optional-keyset (:schemas db) schema)
                                  (set (keys m))))]
    (table/add-rows!
     table
     (concat [(when opt?
                [{:actor (ui/text-button "Add component"
                                         (fn [_actor ctx]
                                           (open-add-component-window! ctx schema table)))
                  :colspan colspan}])]
             [(when opt?
                [(separator/horizontal colspan)])]
             component-rows))
    table))

(defmethod widget/value :s/map [_  _attribute table schemas]
  (into {}
        (for [widget (filter (comp vector? actor/user-object) (group/children table))
              :let [[k _] (actor/user-object widget)]]
          [k (widget/value (get schemas k) k widget schemas)])))
