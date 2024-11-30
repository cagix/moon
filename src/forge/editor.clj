(ns ^:no-doc forge.editor
  (:require [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.string :as str]
            [forge.db :as db]
            [forge.graphics :refer [gui-viewport-height]]
            [forge.info :as info]
            [forge.ui :as ui]
            [forge.utils :refer [index-of truncate ->edn-str]]
            [forge.screen :as screen]
            [forge.widgets.error-window :refer [error-window!]])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Touchable)
           (com.kotcrab.vis.ui.widget VisCheckBox VisTextField VisSelectBox)
           (com.kotcrab.vis.ui.widget.tabbedpane Tab TabbedPane TabbedPaneAdapter)))

(defn- map-keys [m-schema]
  (let [[_m _p & ks] m-schema]
    (for [[k m? _schema] ks]
      k)))

(defn- map-form-k->properties
  "Given a map schema gives a map of key to key properties (like :optional)."
  [m-schema]
  (let [[_m _p & ks] m-schema]
    (into {} (for [[k m? _schema] ks]
               [k (if (map? m?) m?)]))))

(defn- optional? [k map-schema]
  (:optional (k (map-form-k->properties map-schema))))

(defn- optional-keyset [m-schema]
  (set (filter #(optional? % m-schema) (map-keys m-schema))))

(comment
 (= (optional-keyset
     [:map {:closed true}
      [:foo]
      [:bar]
      [:baz {:optional true}]
      [:boz {:optional false}]
      [:asdf {:optional true}]])
    [:baz :asdf])

 )

(defn- optional-keys-left [m-schema m]
  (seq (set/difference (optional-keyset m-schema)
                       (set (keys m)))))

(defn- widget-type [schema _]
  (let [stype (db/schema-type schema)]
    (cond
     (#{:s/map-optional :s/components-ns} stype)
     :s/map

     (#{:s/number :s/nat-int :s/int :s/pos :s/pos-int :s/val-max} stype)
     :widget/edn

     :else stype)))

(defmulti create   widget-type)
(defmulti ->value  widget-type)

(defn- scroll-pane-cell [rows]
  (let [table (ui/table {:rows rows
                         :name "scroll-pane-table"
                         :cell-defaults {:pad 5}
                         :pack? true})]
    {:actor (ui/scroll-pane table)
     :width  (+ (.getWidth table) 50)
     :height (min (- gui-viewport-height 50)
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
          (error-window! t))))

; We are working with raw property data without edn->value and db/build
; otherwise at db/update! we would have to convert again from edn->value back to edn
; for example at images/relationships
(defn- editor-window [props]
  (let [schema (db/schema-of-property props)
        window (ui/window {:title (str "[SKY]Property[]")
                           :id :property-editor-window
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        widget (create schema props)
        save!   (apply-context-fn window #(db/update! (->value schema widget)))
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
    window))

(defmethod create :default [_ v]
  (ui/label (truncate (->edn-str v) 60)))

(defmethod ->value :default [_ widget]
  ((Actor/.getUserObject widget) 1))

(defmethod create :widget/edn [schema v]
  (ui/add-tooltip! (ui/text-field (->edn-str v) {})
                   (str schema)))

(defmethod ->value :widget/edn [_ widget]
  (edn/read-string (VisTextField/.getText widget)))

(defmethod create :string [schema v]
  (ui/add-tooltip! (ui/text-field v {})
                   (str schema)))

(defmethod ->value :string [_ widget]
  (VisTextField/.getText widget))

(defmethod create :boolean [_ checked?]
  (assert (boolean? checked?))
  (ui/check-box "" (fn [_]) checked?))

(defmethod ->value :boolean [_ widget]
  (VisCheckBox/.isChecked widget))

(defmethod create :enum [schema v]
  (ui/select-box {:items (map ->edn-str (rest schema))
                  :selected (->edn-str v)}))

(defmethod ->value :enum [_ widget]
  (edn/read-string (VisSelectBox/.getSelected widget)))

(defn- play-button [sound-file]
  (ui/text-button "play!" #(play-sound sound-file)))

(declare columns)

(defn- all-of-class
  "Returns all asset paths with the specific class."
  [class]
  (filter #(= (.getAssetType asset-manager %) class)
          (.getAssetNames asset-manager)))

(defn- choose-window [table]
  (let [rows (for [sound-file (all-of-class com.badlogic.gdx.audio.Sound)]
               [(ui/text-button (str/replace-first sound-file "sounds/" "")
                                (fn []
                                  (ui/clear-children! table)
                                  (ui/add-rows! table [(columns table sound-file)])
                                  (Actor/.remove (ui/find-ancestor-window ui/*on-clicked-actor*))
                                  (ui/pack-ancestor-window! table)
                                  (let [[k _] (Actor/.getUserObject table)]
                                    (Actor/.setUserObject table [k sound-file]))))
                (play-button sound-file)])]
    (add-actor (scrollable-choose-window rows))))

(defn- columns [table sound-file]
  [(ui/text-button (name sound-file) #(choose-window table))
   (play-button sound-file)])

(defmethod create :s/sound [_ sound-file]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (ui/add-rows! table [(if sound-file
                           (columns table sound-file)
                           [(ui/text-button "No sound" #(choose-window table))])])
    table))

(defn- property-widget [{:keys [property/id] :as props} clicked-id-fn extra-info-text scale]
  (let [on-clicked #(clicked-id-fn id)
        button (if-let [image (db/property->image props)]
                 (ui/image-button image on-clicked {:scale scale})
                 (ui/text-button (name id) on-clicked))
        top-widget (ui/label (or (and extra-info-text (extra-info-text props)) ""))
        stack (ui/stack [button top-widget])]
    (ui/add-tooltip! button #(info/text props))
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
                         :properties/worlds {:columns 10}})

(defn- overview-table [property-type clicked-id-fn]
  (let [{:keys [sort-by-fn
                extra-info-text
                columns
                image/scale]} (overview property-type)
        properties (db/build-all property-type)
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
                    (ui/clear-children! table)
                    (add-one-to-many-rows table property-type property-ids)
                    (ui/pack-ancestor-window! table))]
    (ui/add-rows!
     table
     [[(ui/text-button "+"
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
                           (add-actor window))))]
      (for [property-id property-ids]
        (let [property (db/build property-id)
              image-widget (ui/image->widget (db/property->image property)
                                             {:id property-id})]
          (ui/add-tooltip! image-widget #(info/text property))))
      (for [id property-ids]
        (ui/text-button "-" #(redo-rows (disj property-ids id))))])))

(defmethod create :s/one-to-many [[_ property-type] property-ids]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-many-rows table property-type property-ids)
    table))

(defmethod ->value :s/one-to-many [_ widget]
  (->> (ui/children widget)
       (keep Actor/.getUserObject)
       set))

(defn- add-one-to-one-rows [table property-type property-id]
  (let [redo-rows (fn [id]
                    (ui/clear-children! table)
                    (add-one-to-one-rows table property-type id)
                    (ui/pack-ancestor-window! table))]
    (ui/add-rows!
     table
     [[(when-not property-id
         (ui/text-button "+"
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
                             (add-actor window)))))]
      [(when property-id
         (let [property (db/build property-id)
               image-widget (ui/image->widget (db/property->image property) {:id property-id})]
           (ui/add-tooltip! image-widget #(info/text property))
           image-widget))]
      [(when property-id
         (ui/text-button "-" #(redo-rows nil)))]])))

(defmethod create :s/one-to-one [[_ property-type] property-id]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-one-rows table property-type property-id)
    table))

(defmethod ->value :s/one-to-one [_ widget]
  (->> (ui/children widget)
       (keep Actor/.getUserObject)
       first))

(defn- get-editor-window []
  (:property-editor-window (screen-stage)))

(defn- property-value []
 (let [window (get-editor-window)
       scroll-pane-table (.findActor (:scroll-pane window) "scroll-pane-table")
       m-widget-cell (first (seq (.getCells scroll-pane-table)))
       table (:map-widget scroll-pane-table)]
   (->value [:s/map] table)))

(defn- rebuild-editor-window []
  (let [prop-value (property-value)]
    (Actor/.remove (get-editor-window))
    (add-actor (editor-window prop-value))))

(defn- value-widget [[k v]]
  (let [widget (create (db/schema-of k) v)]
    (.setUserObject widget [k v])
    widget))

(def ^:private value-widget? (comp vector? Actor/.getUserObject))

(defn- find-kv-widget [table k]
  (forge.utils/find-first (fn [actor]
                           (and (Actor/.getUserObject actor)
                                (= k ((Actor/.getUserObject actor) 0))))
                         (ui/children table)))

(defn- attribute-label [k m-schema table]
  (let [label (ui/label ;(str "[GRAY]:" (namespace k) "[]/" (name k))
                        (name k))
        delete-button (when (optional? k m-schema)
                        (ui/text-button "-"
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
        malli-form (db/malli-form schema)
        remaining-ks (sort (remove (set (keys (->value schema map-widget-table)))
                                   (map-keys malli-form)))]
    (ui/add-rows!
     window
     (for [k remaining-ks]
       [(ui/text-button (name k)
                        (fn []
                          (Actor/.remove window)
                          (ui/add-rows! map-widget-table [(component-row
                                                           [k (db/k->default-value k)]
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

(defmethod create :s/map [schema m]
  (let [table (ui/table {:cell-defaults {:pad 5}
                         :id :map-widget})
        component-rows (interpose-f horiz-sep
                          (map #(component-row % (db/malli-form schema) table)
                               (sort-by component-order m)))
        colspan component-row-cols
        opt? (optional-keys-left (db/malli-form schema) m)]
    (ui/add-rows!
     table
     (concat [(when opt?
                [{:actor (ui/text-button "Add component" #(choose-component-window schema table))
                  :colspan colspan}])]
             [(when opt?
                [(ui/horizontal-separator-cell colspan)])]
             component-rows))
    table))

(defmethod ->value :s/map [_ table]
  (into {}
        (for [widget (filter value-widget? (ui/children table))
              :let [[k _] (Actor/.getUserObject widget)]]
          [k (->value (db/schema-of k) widget)])))

; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
#_(defn- texture-rows []
  (for [file (sort (all-of-class com.badlogic.gdx.graphics.Texture))]
    [(ui/image-button (image file) (fn []))]
    #_[(ui/text-button file (fn []))]))

(defmethod create :s/image [schema image]
  (ui/image-button (db/edn->value schema image)
                   (fn on-clicked [])
                   {:scale 2})
  #_(ui/image-button image
                     #(stage/add! (scrollable-choose-window (texture-rows)))
                     {:dimensions [96 96]})) ; x2  , not hardcoded here

(defmethod create :s/animation [_ animation]
  (ui/table {:rows [(for [image (:frames animation)]
                      (ui/image-button (db/edn->value :s/image image)
                                       (fn on-clicked [])
                                       {:scale 2}))]
             :cell-defaults {:pad 1}}))

; FIXME overview table not refreshed after changes in properties

(defn- edit-property [id]
  (add-actor (editor-window (db/get-raw id))))

(defn- property-type-tabs []
  (for [property-type (sort (db/property-types))]
    {:title (str/capitalize (name property-type))
     :content (overview-table property-type edit-property)}))

(defn- tab-widget [{:keys [title content savable? closable-by-user?]}]
  (proxy [Tab] [(boolean savable?) (boolean closable-by-user?)]
    (getTabTitle [] title)
    (getContentTable [] content)))

(defn- tabs-table [label]
  (let [table (ui/table {:fill-parent? true})
        container (ui/table {})
        tabbed-pane (TabbedPane.)]
    (.addListener tabbed-pane
                  (proxy [TabbedPaneAdapter] []
                    (switchedTab [^Tab tab]
                      (.clearChildren container)
                      (.fill (.expand (.add container (.getContentTable tab)))))))
    (.fillX (.expandX (.add table (.getTable tabbed-pane))))
    (.row table)
    (.fill (.expand (.add table container)))
    (.row table)
    (.pad (.left (.add table (ui/label label))) (float 10))
    (doseq [tab-data (property-type-tabs)]
      (.add tabbed-pane (tab-widget tab-data)))
    table))

(defn screen [background-image]
  {:actors [background-image
            (tabs-table "[LIGHT_GRAY]Left-Shift: Back to Main Menu[]")
            (ui/actor {:act (fn []
                              (when (key-just-pressed? :shift-left)
                                (change-screen :screens/main-menu)))})]
   :screen (reify screen/Screen
             (enter [_])
             (exit [_])
             (render [_])
             (destroy [_]))})
