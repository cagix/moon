(ns cdq.editor.widget.map
  (:require [cdq.db :as db]
            [cdq.property :as property]
            [cdq.schemas :as schemas]
            [cdq.editor.overview-table]
            [cdq.editor.widget :as widget]
            [cdq.ui :as ui]
            [cdq.ui.group :as group]
            [cdq.ui.separator :as separator]
            [cdq.ui.table :as table]
            [cdq.ui.text-button :as text-button]
            [cdq.ui.window :as window]
            [cdq.utils :as utils]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.set :as set]))

(declare application-state-atom)

(defn- property-editor-window
  [{:keys [ctx/db
           ctx/ui-viewport]
    :as ctx}
   property]
  (let [schema (get (:schemas db) (property/type property))
        widget (widget/create schema nil property ctx)]
    {:actor/type :actor.type/property-editor
     :delete-fn (fn [_ctx]
                  (swap! application-state-atom update :ctx/db
                         db/delete!
                         (:property/id property)))
     :save? #(input/key-just-pressed? % :enter)
     :save-fn (fn [{:keys [ctx/db]}]
                (swap! application-state-atom update :ctx/db
                       db/update!
                       (widget/value schema nil widget (:schemas db))))
     :scrollpane-height (:viewport/height ui-viewport)
     :widget widget
     :window-opts {:title (str "[SKY]Property[]")
                   :id :property-editor-window
                   :modal? true
                   :close-button? true
                   :center? true
                   :close-on-escape? true
                   :cell-defaults {:pad 5}}}))


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

(defn- rebuild-editor-window!
  [{:keys [ctx/db
           ctx/stage]
    :as ctx}]
  (let [window (:property-editor-window stage)
        prop-value (window->property-value window (:schemas db))]
    (actor/remove! window)
    (stage/add! stage (property-editor-window ctx prop-value))))

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
                              (text-button/create "-"
                                                  (fn [_actor ctx]
                                                    (actor/remove! (find-kv-widget table k))
                                                    (rebuild-editor-window! ctx))))
                     :left? true}
                    {:actor {:actor/type :actor.type/label
                             :label/text (name k) ;(str "[GRAY]:" (namespace k) "[]/" (name k))
                             }}]]}
    :right? true}
   (separator/vertical)
   {:actor (let [widget (ui/construct? (widget/create (get schemas k) k v ctx))]
             (actor/set-user-object! widget [k v])
             widget)
    :left? true}])

(defn- open-add-component-window! [{:keys [ctx/db
                                           ctx/stage]}
                                   schema
                                   map-widget-table]
  (let [schemas (:schemas db)
        window (window/create {:title "Choose"
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
       [(text-button/create (name k)
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

(defn create [schema  _attribute m {:keys [ctx/db] :as ctx}]
  (let [table (table/create
               {:cell-defaults {:pad 5}
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
                [{:actor (text-button/create "Add component"
                                             (fn [_actor ctx]
                                               (open-add-component-window! ctx schema table)))
                  :colspan colspan}])]
             [(when opt?
                [(separator/horizontal colspan)])]
             component-rows))
    table))

(defn value [_  _attribute table schemas]
  (into {}
        (for [widget (filter (comp vector? actor/user-object) (group/children table))
              :let [[k _] (actor/user-object widget)]]
          [k (widget/value (get schemas k) k widget schemas)])))

; construct !
(defn open-editor-overview-window!
  [{:keys [ctx/stage]
    :as ctx}
   property-type]
  (let [window (window/create {:title "Edit"
                               :modal? true
                               :close-button? true
                               :center? true
                               :close-on-escape? true})
        on-clicked-id (fn [id
                           {:keys [ctx/db
                                   ctx/stage]
                            :as ctx}]
                        (stage/add! stage (property-editor-window ctx (db/get-raw db id))))]
    (table/add! window (cdq.editor.overview-table/create ctx
                                                         property-type
                                                         on-clicked-id))
    (.pack window)
    (stage/add! stage window)))
