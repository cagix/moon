(ns gdl.editor
  (:require [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.lwjgl :as lwjgl]
            [clojure.string :as str]
            [clojure.component :refer [defcomponent]]
            [clojure.utils :refer [truncate ->edn-str find-first sort-by-k-order]]
            [gdl.app :as app]
            [gdl.assets :as assets]
            [gdl.context :as ctx :refer [play-sound]]
            [gdl.db :as db]
            [gdl.malli :as m]
            [gdl.schema :as schema]
            [gdl.property :as property]
            [gdl.ui :refer [horizontal-separator-cell
                            vertical-separator-cell
                            ui-actor
                            image-button
                            text-button
                            *on-clicked-actor*
                            find-ancestor-window
                            pack-ancestor-window!
                            scroll-pane
                            image->widget
                            ui-stack
                            text-field
                            add-tooltip!]
             :as ui]
            [clojure.gdx.scene2d.actor :refer [user-object]]
            [clojure.gdx.scene2d.group :refer [children clear-children add-actor! find-actor]]
            [gdl.ui.table :refer [add-rows!]])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Table)
           (com.kotcrab.vis.ui.widget.tabbedpane Tab TabbedPane)))

(defn- info-text [property]
  (binding [*print-level* 3]
    (with-out-str
     (clojure.pprint/pprint property))))

(defn- widget-type [schema _]
  (let [stype (schema/type schema)]
    (cond
     (#{:s/map-optional :s/components-ns} stype)
     :s/map

     (#{:s/number :s/nat-int :s/int :s/pos :s/pos-int :s/val-max} stype)
     :widget/edn

     :else stype)))

(defmulti schema->widget widget-type)
(defmulti ->value        widget-type)

(defn- scroll-pane-cell [rows]
  (let [table (ui/table {:rows rows
                         :name "scroll-pane-table"
                         :cell-defaults {:pad 5}
                         :pack? true})]
    {:actor (ui/scroll-pane table)
     :width  (+ (.getWidth table) 50)
     :height (min (- (:height (:gdl.context/viewport @app/state)) 50)
                  (.getHeight table))}))

(defn- scrollable-choose-window [rows]
  (ui/window {:title "Choose"
              :modal? true
              :close-button? true
              :center? true
              :close-on-escape? true
              :rows [[(scroll-pane-cell rows)]]
              :pack? true}))

(defn- apply-context-fn [window f]
  #(try (f)
        (Actor/.remove window)
        (catch Throwable t
          (ctx/error-window @app/state t))))

(defn- async-write-to-file! [{:keys [gdl.context/db]}]
  (db/async-write-to-file! db))

(defn update! [property]
  (swap! app/state update :gdl.context/db db/update property)
  (async-write-to-file! @app/state))

(defn delete! [property-id]
  (swap! app/state update :gdl.context/db db/delete property-id)
  (async-write-to-file! @app/state))

(defn- get-db
  ([]
   (get-db @app/state))
  ([context]
   (:gdl.context/db context)))

(defn- malli-form [schema]
  (schema/malli-form schema (:db/schemas (get-db))))

; We are working with raw property data without edn->value and build
; otherwise at update! we would have to convert again from edn->value back to edn
; for example at images/relationships
(defn- editor-window [props]
  (let [schema (db/schema-of (get-db) (property/type props))
        window (ui/window {:title (str "[SKY]Property[]")
                           :id :property-editor-window
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        widget (schema->widget schema props)
        save!   (apply-context-fn window #(update! (->value schema widget)))
        delete! (apply-context-fn window #(delete! (:property/id props)))]
    (add-rows! window [[(scroll-pane-cell [[{:actor widget :colspan 2}]
                                           [{:actor (text-button "Save [LIGHT_GRAY](ENTER)[]" save!)
                                             :center? true}
                                            {:actor (text-button "Delete" delete!)
                                             :center? true}]])]])
    (add-actor! window (ui-actor {:act (fn [context]
                                         (when (gdx/key-just-pressed? context :enter)
                                           (save!)))}))
    (.pack window)
    window))

(defmethod schema->widget :default [_ v]
  (ui/label (truncate (->edn-str v) 60)))

(defmethod ->value :default [_ widget]
  ((user-object widget) 1))

(defmethod schema->widget :widget/edn [schema v]
  (add-tooltip! (text-field (->edn-str v) {})
                (str schema)))

(defmethod ->value :widget/edn [_ widget]
  (edn/read-string (ui/text-field->text widget)))

(defmethod schema->widget :string [schema v]
  (add-tooltip! (text-field v {})
                (str schema)))

(defmethod ->value :string [_ widget]
  (ui/text-field->text widget))

(defmethod schema->widget :boolean [_ checked?]
  (assert (boolean? checked?))
  (ui/check-box "" (fn [_]) checked?))

(defmethod ->value :boolean [_ widget]
  (ui/checked? widget))

(defmethod schema->widget :enum [schema v]
  (ui/select-box {:items (map ->edn-str (rest schema))
                  :selected (->edn-str v)}))

(defmethod ->value :enum [_ widget]
  (edn/read-string (ui/selected widget)))

(defn- all-of-type [asset-type]
  (assets/all-of-type (:gdl.context/assets @app/state)
                      asset-type))

(defn- play-button [sound-name]
  (text-button "play!" #(play-sound @app/state sound-name)))

(declare columns)

(defn- sound-file->sound-name [sound-file]
  (-> sound-file
      (str/replace-first "sounds/" "")
      (str/replace ".wav" "")))

(defn- choose-window [table]
  (let [rows (for [sound-name (map sound-file->sound-name (all-of-type :sound))]
               [(text-button sound-name
                             (fn []
                               (clear-children table)
                               (add-rows! table [(columns table sound-name)])
                               (Actor/.remove (find-ancestor-window *on-clicked-actor*))
                               (pack-ancestor-window! table)
                               (let [[k _] (user-object table)]
                                 (Actor/.setUserObject table [k sound-name]))))
                (play-button sound-name)])]
    (ctx/add-actor @app/state (scrollable-choose-window rows))))

(defn- columns [table sound-name]
  [(text-button sound-name
                #(choose-window table))
   (play-button sound-name)])

(defmethod schema->widget :s/sound [_ sound-name]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-rows! table [(if sound-name
                        (columns table sound-name)
                        [(text-button "No sound" #(choose-window table))])])
    table))

(defn- property-widget [{:keys [property/id] :as props} clicked-id-fn extra-info-text scale]
  (let [on-clicked #(clicked-id-fn id)
        button (if-let [image (property/->image props)]
                 (image-button image on-clicked {:scale scale})
                 (text-button (name id) on-clicked))
        top-widget (ui/label (or (and extra-info-text (extra-info-text props)) ""))
        stack (ui-stack [button top-widget])]
    (add-tooltip! button (fn [_context] (info-text props)))
    (Actor/.setTouchable top-widget Touchable/disabled)
    stack))

(def ^:private overview {:properties/audiovisuals {:columns 10
                                                   :image/scale 2}
                         :properties/creatures {:columns 15
                                                :image/scale 1.5
                                                :sort-by-fn #(vector (:creature/level %)
                                                                     (name (:entity/species %))
                                                                     (name (:property/id %)))
                                                :extra-info-text #(str (:creature/level %))}
                         :properties/items {:columns 20
                                            :image/scale 1.1
                                            :sort-by-fn #(vector (if-let [slot (:item/slot %)]
                                                                   (name slot)
                                                                   "")
                                                                 (name (:property/id %)))}
                         :properties/projectiles {:columns 16
                                                  :image/scale 2}
                         :properties/skills {:columns 16
                                             :image/scale 2}
                         :properties/worlds {:columns 10}
                         :properties/player-dead {:columns 1}
                         :properties/player-idle {:columns 1}
                         :properties/player-item-on-cursor {:columns 1}})

(defn- overview-table ^Actor [context property-type clicked-id-fn]
  (assert (contains? overview property-type)
          (pr-str property-type))
  (let [{:keys [sort-by-fn
                extra-info-text
                columns
                image/scale]} (overview property-type)
        properties (db/build-all (get-db context) property-type context)
        properties (if sort-by-fn
                     (sort-by sort-by-fn properties)
                     properties)]
    (ui/table
     {:cell-defaults {:pad 5}
      :rows (for [properties (partition-all columns properties)]
              (for [property properties]
                (try (property-widget property clicked-id-fn extra-info-text scale)
                     (catch Throwable t
                       (throw (ex-info "" {:property property} t))))))})))

(defn- add-one-to-many-rows [table property-type property-ids]
  (let [redo-rows (fn [property-ids]
                    (clear-children table)
                    (add-one-to-many-rows table property-type property-ids)
                    (pack-ancestor-window! table))]
    (add-rows!
     table
     [[(text-button "+"
                    (fn []
                      (let [window (ui/window {:title "Choose"
                                               :modal? true
                                               :close-button? true
                                               :center? true
                                               :close-on-escape? true})
                            clicked-id-fn (fn [id]
                                            (Actor/.remove window)
                                            (redo-rows (conj property-ids id)))]
                        (.add window (overview-table property-type clicked-id-fn))
                        (.pack window)
                        (ctx/add-actor @app/state window))))]
      (for [property-id property-ids]
        (let [property (db/build (get-db) property-id)
              image-widget (image->widget (property/->image property)
                                          {:id property-id})]
          (add-tooltip! image-widget (fn [_context] (info-text property)))))
      (for [id property-ids]
        (text-button "-" #(redo-rows (disj property-ids id))))])))

(defmethod schema->widget :s/one-to-many [[_ property-type] property-ids]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-many-rows table property-type property-ids)
    table))

(defmethod ->value :s/one-to-many [_ widget]
  (->> (children widget)
       (keep user-object)
       set))

(defn- add-one-to-one-rows [table property-type property-id]
  (let [redo-rows (fn [id]
                    (clear-children table)
                    (add-one-to-one-rows table property-type id)
                    (pack-ancestor-window! table))]
    (add-rows!
     table
     [[(when-not property-id
         (text-button "+"
                      (fn []
                        (let [window (ui/window {:title "Choose"
                                                 :modal? true
                                                 :close-button? true
                                                 :center? true
                                                 :close-on-escape? true})
                              clicked-id-fn (fn [id]
                                              (Actor/.remove window)
                                              (redo-rows id))]
                          (.add window (overview-table property-type clicked-id-fn))
                          (.pack window)
                          (ctx/add-actor @app/state window)))))]
      [(when property-id
         (let [property (db/build (get-db) property-id)
               image-widget (image->widget (property/->image property)
                                           {:id property-id})]
           (add-tooltip! image-widget (fn [_context] (info-text property)))
           image-widget))]
      [(when property-id
         (text-button "-" #(redo-rows nil)))]])))

(defmethod schema->widget :s/one-to-one [[_ property-type] property-id]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-one-rows table property-type property-id)
    table))

(defmethod ->value :s/one-to-one [_ widget]
  (->> (children widget)
       (keep user-object)
       first))

(defn- get-editor-window []
  (:property-editor-window (ctx/stage @app/state)))

(defn- window->property-value []
 (let [window (get-editor-window)
       scroll-pane-table (find-actor (:scroll-pane window) "scroll-pane-table")
       m-widget-cell (first (seq (Table/.getCells scroll-pane-table)))
       table (:map-widget scroll-pane-table)]
   (->value [:s/map] table)))

(defn- rebuild-editor-window []
  (let [prop-value (window->property-value)]
    (Actor/.remove (get-editor-window))
    (ctx/add-actor @app/state (editor-window prop-value))))

(defn- value-widget [[k v]]
  (let [widget (schema->widget (db/schema-of (get-db) k) v)]
    (Actor/.setUserObject widget [k v])
    widget))

(def ^:private value-widget? (comp vector? user-object))

(defn- find-kv-widget [table k]
  (find-first (fn [actor]
                (and (user-object actor)
                     (= k ((user-object actor) 0))))
              (children table)))

(defn- attribute-label [k m-schema table]
  (let [label (ui/label ;(str "[GRAY]:" (namespace k) "[]/" (name k))
                        (name k))
        delete-button (when (m/optional? k m-schema)
                        (text-button "-"
                                     (fn []
                                       (Actor/.remove (find-kv-widget table k))
                                       (rebuild-editor-window))))]
    (ui/table {:cell-defaults {:pad 2}
               :rows [[{:actor delete-button :left? true}
                       label]]})))

(def ^:private component-row-cols 3)

(defn- component-row [[k v] m-schema table]
  [{:actor (attribute-label k m-schema table)
    :right? true}
   (vertical-separator-cell)
   {:actor (value-widget [k v])
    :left? true}])

(defn- horiz-sep []
  [(horizontal-separator-cell component-row-cols)])

(defn- k->default-value [k]
  (let [schema (db/schema-of (get-db) k)]
    (cond
     (#{:s/one-to-one :s/one-to-many} (schema/type schema)) nil

     ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

     :else (m/generate (malli-form schema) {:size 3}))))

(defn- choose-component-window [schema map-widget-table]
  (let [window (ui/window {:title "Choose"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        malli-form (malli-form schema)
        remaining-ks (sort (remove (set (keys (->value schema map-widget-table)))
                                   (m/map-keys malli-form)))]
    (add-rows!
     window
     (for [k remaining-ks]
       [(text-button (name k)
                     (fn []
                       (Actor/.remove window)
                       (add-rows! map-widget-table [(component-row
                                                     [k (k->default-value k)]
                                                     malli-form
                                                     map-widget-table)])
                       (rebuild-editor-window)))]))
    (.pack window)
    (ctx/add-actor @app/state window)))

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

(defmethod schema->widget :s/map [schema m]
  (let [table (ui/table {:cell-defaults {:pad 5}
                         :id :map-widget})
        component-rows (interpose-f horiz-sep
                          (map #(component-row % (malli-form schema) table)
                               (sort-by-k-order property-k-sort-order
                                                m)))
        colspan component-row-cols
        opt? (m/optional-keys-left (malli-form schema) m)]
    (add-rows!
     table
     (concat [(when opt?
                [{:actor (text-button "Add component" #(choose-component-window schema table))
                  :colspan colspan}])]
             [(when opt?
                [(horizontal-separator-cell colspan)])]
             component-rows))
    table))

(defmethod ->value :s/map [_ table]
  (into {}
        (for [widget (filter value-widget? (children table))
              :let [[k _] (user-object widget)]]
          [k (->value (db/schema-of (get-db) k) widget)])))

; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
#_(defn- texture-rows []
  (for [file (sort (all-of-type :texture))]
    [(image-button (image file) (fn []))]
    #_[(text-button file (fn []))]))

(defmethod schema->widget :s/image [schema image]
  (image-button (db/edn->value schema image (get-db) @app/state)
                (fn on-clicked [])
                {:scale 2})
  #_(image-button image
                  #(ctx/add-actor @app/state (scrollable-choose-window (texture-rows)))
                  {:dimensions [96 96]})) ; x2  , not hardcoded here

(defmethod schema->widget :s/animation [_ animation]
  (ui/table {:rows [(for [image (:frames animation)]
                      (image-button (db/edn->value :s/image image (get-db) @app/state)
                                    (fn on-clicked [])
                                    {:scale 2}))]
             :cell-defaults {:pad 1}}))

; FIXME overview table not refreshed after changes in properties

(defn- edit-property [id]
  (ctx/add-actor @app/state (editor-window (db/get-raw (get-db) id))))

(defn- property-type-tabs [context]
  (for [property-type (sort (db/property-types (get-db context)))]
    {:title (str/capitalize (name property-type))
     :content (overview-table context
                              property-type
                              edit-property)}))

(defn- tab-widget [{:keys [title content savable? closable-by-user?]}]
  (proxy [Tab] [(boolean savable?) (boolean closable-by-user?)]
    (getTabTitle [] title)
    (getContentTable [] content)))

(defn- tabs-table [context label-str]
  (let [table (ui/table {:fill-parent? true})
        container (ui/table {})
        tabbed-pane (TabbedPane.)]
    (.addListener tabbed-pane
                  (proxy [com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter] []
                    (switchedTab [^com.kotcrab.vis.ui.widget.tabbedpane.Tab tab]
                      (.clearChildren container)
                      (.fill (.expand (.add container (.getContentTable tab)))))))
    (.fillX (.expandX (.add table (.getTable tabbed-pane))))
    (.row table)
    (.fill (.expand (.add table container)))
    (.row table)
    (.pad (.left (.add table (ui/label label-str))) (float 10))
    (doseq [tab-data (property-type-tabs context)]
      (.add tabbed-pane (tab-widget tab-data)))
    table))

(defn- background-image [{:keys [gdl.context/assets]} path]
  (ui/image-widget (assets path)
                   {:fill-parent? true
                    :scaling :fill
                    :align :center}))

(defcomponent ::stage-actors
  (app/create [_ context]
    (doseq [actor [(background-image context "images/moon_background.png")
                   (tabs-table       context "custom label text here")]]
      (ctx/add-actor context actor))))

(defn- render-stage [context]
  (let [stage (ctx/stage context)]
    (ui/act  stage context)
    (ui/draw stage context))
  context)

(def ^:private components
  [[:gdl.context/unit-scale 1]
   [:gdl.context/batch]
   [:gdl.context/db {:schema "schema.edn"
                     :properties "properties.edn"}]
   [:gdl.context/assets "resources/"]
   [:gdl.context/viewport {:width 1440 :height 900}]

   ;; just because of sprite edn->value of db requires world-unit-scale
   [:gdl.context/world-unit-scale 1]
   [:gdl.context/world-viewport {:width 1440 :height 900}]
   ;;

   [:gdl.context/ui :skin-scale/x1]
   [:gdl.context/stage]
   [::stage-actors]])

(defn -main []
  (app/start {:title "Editor"
              :fps 60
              :width 1440
              :height 900
              :taskbar-icon "moon.png"}
             components
             render-stage))
