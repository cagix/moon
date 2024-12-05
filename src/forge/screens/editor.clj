(ns ^:no-doc forge.screens.editor
  (:require [clojure.edn :as edn]
            [forge.core :refer :all])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Table)
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
  (seq (set-difference (optional-keyset m-schema)
                       (set (keys m)))))

(defn- widget-type [schema _]
  (let [stype (schema-type schema)]
    (cond
     (#{:s/map-optional :s/components-ns} stype)
     :s/map

     (#{:s/number :s/nat-int :s/int :s/pos :s/pos-int :s/val-max} stype)
     :widget/edn

     :else stype)))

(defmulti schema->widget widget-type)
(defmulti ->value        widget-type)

(defn- scroll-pane-cell [rows]
  (let [table (ui-table {:rows rows
                         :name "scroll-pane-table"
                         :cell-defaults {:pad 5}
                         :pack? true})]
    {:actor (scroll-pane table)
     :width  (+ (.getWidth table) 50)
     :height (min (- gui-viewport-height 50)
                  (.getHeight table))}))

(defn- scrollable-choose-window [rows]
  (ui-window {:title "Choose"
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

; We are working with raw property data without edn->value and build
; otherwise at update! we would have to convert again from edn->value back to edn
; for example at images/relationships
(defn- editor-window [props]
  (let [schema (schema-of-property props)
        window (ui-window {:title (str "[SKY]Property[]")
                           :id :property-editor-window
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        widget (schema->widget schema props)
        save!   (apply-context-fn window #(db-update! (->value schema widget)))
        delete! (apply-context-fn window #(db-delete! (:property/id props)))]
    (add-rows! window [[(scroll-pane-cell [[{:actor widget :colspan 2}]
                                           [{:actor (text-button "Save [LIGHT_GRAY](ENTER)[]" save!)
                                             :center? true}
                                            {:actor (text-button "Delete" delete!)
                                             :center? true}]])]])
    (add-actor! window (ui-actor {:act (fn []
                                         (when (key-just-pressed? :enter)
                                           (save!)))}))
    (.pack window)
    window))

(defmethod schema->widget :default [_ v]
  (label (truncate (->edn-str v) 60)))

(defmethod ->value :default [_ widget]
  ((user-object widget) 1))

(defmethod schema->widget :widget/edn [schema v]
  (add-tooltip! (text-field (->edn-str v) {})
                (str schema)))

(defmethod ->value :widget/edn [_ widget]
  (edn/read-string (VisTextField/.getText widget)))

(defmethod schema->widget :string [schema v]
  (add-tooltip! (text-field v {})
                (str schema)))

(defmethod ->value :string [_ widget]
  (VisTextField/.getText widget))

(defmethod schema->widget :boolean [_ checked?]
  (assert (boolean? checked?))
  (check-box "" (fn [_]) checked?))

(defmethod ->value :boolean [_ widget]
  (VisCheckBox/.isChecked widget))

(defmethod schema->widget :enum [schema v]
  (select-box {:items (map ->edn-str (rest schema))
               :selected (->edn-str v)}))

(defmethod ->value :enum [_ widget]
  (edn/read-string (VisSelectBox/.getSelected widget)))

(defn- play-button [sound-file]
  (text-button "play!" #(play-sound sound-file)))

(declare columns)

(defn- all-of-class
  "Returns all asset paths with the specific class."
  [class]
  (filter #(= (.getAssetType asset-manager %) class)
          (.getAssetNames asset-manager)))

(defn- choose-window [table]
  (let [rows (for [sound-file (all-of-class com.badlogic.gdx.audio.Sound)]
               [(text-button (str-replace-first sound-file "sounds/" "")
                             (fn []
                               (clear-children table)
                               (add-rows! table [(columns table sound-file)])
                               (Actor/.remove (find-ancestor-window *on-clicked-actor*))
                               (pack-ancestor-window! table)
                               (let [[k _] (user-object table)]
                                 (Actor/.setUserObject table [k sound-file]))))
                (play-button sound-file)])]
    (add-actor (scrollable-choose-window rows))))

(defn- columns [table sound-file]
  [(text-button (name sound-file) #(choose-window table))
   (play-button sound-file)])

(defmethod schema->widget :s/sound [_ sound-file]
  (let [table (ui-table {:cell-defaults {:pad 5}})]
    (add-rows! table [(if sound-file
                        (columns table sound-file)
                        [(text-button "No sound" #(choose-window table))])])
    table))

(defn- property-widget [{:keys [property/id] :as props} clicked-id-fn extra-info-text scale]
  (let [on-clicked #(clicked-id-fn id)
        button (if-let [image (property->image props)]
                 (image-button image on-clicked {:scale scale})
                 (text-button (name id) on-clicked))
        top-widget (label (or (and extra-info-text (extra-info-text props)) ""))
        stack (ui-stack [button top-widget])]
    (add-tooltip! button #(info-text props))
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

(defn- overview-table ^Actor [property-type clicked-id-fn]
  (let [{:keys [sort-by-fn
                extra-info-text
                columns
                image/scale]} (overview property-type)
        properties (build-all property-type)
        properties (if sort-by-fn
                     (sort-by sort-by-fn properties)
                     properties)]
    (ui-table
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
                      (let [window (ui-window {:title "Choose"
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
        (let [property (build property-id)
              image-widget (image->widget (property->image property)
                                          {:id property-id})]
          (add-tooltip! image-widget #(info-text property))))
      (for [id property-ids]
        (text-button "-" #(redo-rows (disj property-ids id))))])))

(defmethod schema->widget :s/one-to-many [[_ property-type] property-ids]
  (let [table (ui-table {:cell-defaults {:pad 5}})]
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
                        (let [window (ui-window {:title "Choose"
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
         (let [property (build property-id)
               image-widget (image->widget (property->image property) {:id property-id})]
           (add-tooltip! image-widget #(info-text property))
           image-widget))]
      [(when property-id
         (text-button "-" #(redo-rows nil)))]])))

(defmethod schema->widget :s/one-to-one [[_ property-type] property-id]
  (let [table (ui-table {:cell-defaults {:pad 5}})]
    (add-one-to-one-rows table property-type property-id)
    table))

(defmethod ->value :s/one-to-one [_ widget]
  (->> (children widget)
       (keep user-object)
       first))

(defn- get-editor-window []
  (:property-editor-window (screen-stage)))

(defn- window->property-value []
 (let [window (get-editor-window)
       scroll-pane-table (find-actor (:scroll-pane window) "scroll-pane-table")
       m-widget-cell (first (seq (Table/.getCells scroll-pane-table)))
       table (:map-widget scroll-pane-table)]
   (->value [:s/map] table)))

(defn- rebuild-editor-window []
  (let [prop-value (window->property-value)]
    (Actor/.remove (get-editor-window))
    (add-actor (editor-window prop-value))))

(defn- value-widget [[k v]]
  (let [widget (schema->widget (schema-of k) v)]
    (Actor/.setUserObject widget [k v])
    widget))

(def ^:private value-widget? (comp vector? user-object))

(defn- find-kv-widget [table k]
  (find-first (fn [actor]
                (and (user-object actor)
                     (= k ((user-object actor) 0))))
              (children table)))

(defn- attribute-label [k m-schema table]
  (let [label (label ;(str "[GRAY]:" (namespace k) "[]/" (name k))
                     (name k))
        delete-button (when (optional? k m-schema)
                        (text-button "-"
                                     (fn []
                                       (Actor/.remove (find-kv-widget table k))
                                       (rebuild-editor-window))))]
    (ui-table {:cell-defaults {:pad 2}
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

(defn- choose-component-window [schema map-widget-table]
  (let [window (ui-window {:title "Choose"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        malli-form (malli-form schema)
        remaining-ks (sort (remove (set (keys (->value schema map-widget-table)))
                                   (map-keys malli-form)))]
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

(defmethod schema->widget :s/map [schema m]
  (let [table (ui-table {:cell-defaults {:pad 5}
                         :id :map-widget})
        component-rows (interpose-f horiz-sep
                          (map #(component-row % (malli-form schema) table)
                               (sort-by component-order m)))
        colspan component-row-cols
        opt? (optional-keys-left (malli-form schema) m)]
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
          [k (->value (schema-of k) widget)])))

; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
#_(defn- texture-rows []
  (for [file (sort (all-of-class com.badlogic.gdx.graphics.Texture))]
    [(image-button (image file) (fn []))]
    #_[(text-button file (fn []))]))

(defmethod schema->widget :s/image [schema image]
  (image-button (edn->value schema image)
                (fn on-clicked [])
                {:scale 2})
  #_(image-button image
                  #(stage/add! (scrollable-choose-window (texture-rows)))
                  {:dimensions [96 96]})) ; x2  , not hardcoded here

(defmethod schema->widget :s/animation [_ animation]
  (ui-table {:rows [(for [image (:frames animation)]
                      (image-button (edn->value :s/image image)
                                    (fn on-clicked [])
                                    {:scale 2}))]
             :cell-defaults {:pad 1}}))

; FIXME overview table not refreshed after changes in properties

(defn- edit-property [id]
  (add-actor (editor-window (get-raw id))))

(defn- property-type-tabs []
  (for [property-type (sort (property-types))]
    {:title (str-capitalize (name property-type))
     :content (overview-table property-type edit-property)}))

(defn- tab-widget [{:keys [title content savable? closable-by-user?]}]
  (proxy [Tab] [(boolean savable?) (boolean closable-by-user?)]
    (getTabTitle [] title)
    (getContentTable [] content)))

(defn- tabs-table [label-str]
  (let [table (ui-table {:fill-parent? true})
        container (ui-table {})
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
    (.pad (.left (.add table (label label-str))) (float 10))
    (doseq [tab-data (property-type-tabs)]
      (.add tabbed-pane (tab-widget tab-data)))
    table))

(defn create []
  {:actors [(background-image)
            (tabs-table "[LIGHT_GRAY]Left-Shift: Back to Main Menu[]")
            (ui-actor {:act (fn []
                              (when (key-just-pressed? :shift-left)
                                (change-screen :screens/main-menu)))})]
   :screen (reify Screen
             (screen-enter [_])
             (screen-exit [_])
             (screen-render [_])
             (screen-destroy [_]))})
