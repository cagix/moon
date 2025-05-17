(ns cdq.ui.editor
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.schema :as schema]
            [cdq.property :as property]
            [cdq.tx.sound :as tx.sound]
            [cdq.ui.editor.widget :as widget]
            [cdq.ui.error-window :as error-window]
            [cdq.utils :as utils]
            [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.string :as str]
            [gdl.assets :as assets]
            [gdl.input :as input]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor]
            [gdl.ui.stage :as stage]
            [gdl.malli :as m])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group
                                            Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Table)
           (com.kotcrab.vis.ui.widget VisTextField
                                      VisSelectBox
                                      VisCheckBox)))

(defn- info-text [property]
  (binding [*print-level* 3]
    (with-out-str
     (clojure.pprint/pprint property))))

(defn- scroll-pane-cell [rows]
  (let [table (ui/table {:rows rows
                         :name "scroll-pane-table"
                         :cell-defaults {:pad 5}
                         :pack? true})]
    {:actor (ui/scroll-pane table)
     :width  (+ (.getWidth table) 50)
     :height (min (- (:height ctx/ui-viewport) 50)
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
          (utils/pretty-pst t)
          (stage/add-actor! ctx/stage (error-window/create t)))))

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
    (ui/add-rows! window [[(scroll-pane-cell [[{:actor widget :colspan 2}]
                                              [{:actor (ui/text-button "Save [LIGHT_GRAY](ENTER)[]" save!)
                                                :center? true}
                                               {:actor (ui/text-button "Delete" delete!)
                                                :center? true}]])]])
    (.addActor window (proxy [Actor] []
                        (act [_delta]
                          (when (input/key-just-pressed? :enter)
                            (save!)))))
    (.pack window)
    window))

(defn- ->edn-str [v]
  (binding [*print-level* nil]
    (pr-str v)))

(defn- truncate [s limit]
  (if (> (count s) limit)
    (str (subs s 0 limit) "...")
    s))

(defmethod widget/create :default [_ v]
  (ui/label (truncate (->edn-str v) 60)))

(defmethod widget/value :default [_ widget]
  ((Actor/.getUserObject widget) 1))

(defmethod widget/create :widget/edn [schema v]
  (actor/add-tooltip! (ui/text-field (->edn-str v) {})
                      (str schema)))

(defmethod widget/value :widget/edn [_ widget]
  (edn/read-string (VisTextField/.getText widget)))

(defmethod widget/create :string [schema v]
  (actor/add-tooltip! (ui/text-field v {})
                      (str schema)))

(defmethod widget/value :string [_ widget]
  (VisTextField/.getText widget))

(defmethod widget/create :boolean [_ checked?]
  (assert (boolean? checked?))
  (ui/check-box "" (fn [_]) checked?))

(defmethod widget/value :boolean [_ widget]
  (VisCheckBox/.isChecked widget))

(defmethod widget/create :enum [schema v]
  (ui/select-box {:items (map ->edn-str (rest schema))
                  :selected (->edn-str v)}))

(defmethod widget/value :enum [_ widget]
  (edn/read-string (VisSelectBox/.getSelected widget)))

(defn- play-button [sound-name]
  (ui/text-button "play!" #(tx.sound/do! sound-name)))

(declare columns)

(defn- sound-file->sound-name [sound-file]
  (-> sound-file
      (str/replace-first "sounds/" "")
      (str/replace ".wav" "")))

(defn- choose-window [table]
  (let [rows (for [sound-name (map sound-file->sound-name (assets/all-of-type ctx/assets :sound))]
               [(ui/text-button sound-name
                                (fn []
                                  (Group/.clearChildren table)
                                  (ui/add-rows! table [(columns table sound-name)])
                                  (.remove (ui/find-ancestor-window ui/*on-clicked-actor*))
                                  (ui/pack-ancestor-window! table)
                                  (let [[k _] (Actor/.getUserObject table)]
                                    (Actor/.setUserObject table [k sound-name]))))
                (play-button sound-name)])]
    (stage/add-actor! ctx/stage (scrollable-choose-window rows))))

(defn- columns [table sound-name]
  [(ui/text-button sound-name
                   #(choose-window table))
   (play-button sound-name)])

(defmethod widget/create :s/sound [_ sound-name]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (ui/add-rows! table [(if sound-name
                           (columns table sound-name)
                           [(ui/text-button "No sound" #(choose-window table))])])
    table))

(defn- property-widget [{:keys [property/id] :as props} clicked-id-fn extra-info-text scale]
  (let [on-clicked #(clicked-id-fn id)
        button (if-let [image (property/image props)]
                 (ui/image-button image on-clicked {:scale scale})
                 (ui/text-button (name id) on-clicked))
        top-widget (ui/label (or (and extra-info-text (extra-info-text props)) ""))
        stack (ui/stack [button top-widget])]
    (actor/add-tooltip! button #(info-text props))
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

(defn- overview-table [property-type clicked-id-fn]
  (assert (contains? overview property-type)
          (pr-str property-type))
  (let [{:keys [sort-by-fn
                extra-info-text
                columns
                image/scale]} (overview property-type)
        properties (db/build-all ctx/db property-type)
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
                    (Group/.clearChildren table)
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
                                               (.remove window)
                                               (redo-rows (conj property-ids id)))]
                           (Table/.add window ^Actor (overview-table property-type clicked-id-fn))
                           (.pack window)
                           (stage/add-actor! ctx/stage window))))]
      (for [property-id property-ids]
        (let [property (db/build ctx/db property-id)
              image-widget (ui/image->widget (property/image property)
                                             {:id property-id})]
          (actor/add-tooltip! image-widget #(info-text property))))
      (for [id property-ids]
        (ui/text-button "-" #(redo-rows (disj property-ids id))))])))

(defmethod widget/create :s/one-to-many [[_ property-type] property-ids]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-many-rows table property-type property-ids)
    table))

(defmethod widget/value :s/one-to-many [_ widget]
  (->> (Group/.getChildren widget)
       (keep Actor/.getUserObject)
       set))

(defn- add-one-to-one-rows [table property-type property-id]
  (let [redo-rows (fn [id]
                    (Group/.clearChildren table)
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
                                                 (.remove window)
                                                 (redo-rows id))]
                             (Table/.add table window ^Actor (overview-table property-type clicked-id-fn))
                             (.pack window)
                             (stage/add-actor! ctx/stage window)))))]
      [(when property-id
         (let [property (db/build ctx/db property-id)
               image-widget (ui/image->widget (property/image property)
                                              {:id property-id})]
           (actor/add-tooltip! image-widget #(info-text property))
           image-widget))]
      [(when property-id
         (ui/text-button "-" #(redo-rows nil)))]])))

(defmethod widget/create :s/one-to-one [[_ property-type] property-id]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-one-rows table property-type property-id)
    table))

(defmethod widget/value :s/one-to-one [_ widget]
  (->> (Group/.getChildren widget)
       (keep Actor/.getUserObject)
       first))

(defn- get-editor-window []
  (:property-editor-window ctx/stage))

(defn- window->property-value []
 (let [window (get-editor-window)
       scroll-pane-table (Group/.findActor (:scroll-pane window) "scroll-pane-table")
       m-widget-cell (first (seq (Table/.getCells scroll-pane-table)))
       table (:map-widget scroll-pane-table)]
   (widget/value [:s/map] table)))

(defn- rebuild-editor-window []
  (let [prop-value (window->property-value)]
    (Actor/.remove (get-editor-window))
    (stage/add-actor! ctx/stage (editor-window prop-value))))

(defn- value-widget [[k v]]
  (let [widget (widget/create (get ctx/schemas k) v)]
    (Actor/.setUserObject widget [k v])
    widget))

(def ^:private value-widget? (comp vector? Actor/.getUserObject))

(defn- find-kv-widget [table k]
  (utils/find-first (fn [actor]
                      (and (Actor/.getUserObject actor)
                           (= k ((Actor/.getUserObject actor) 0))))
                    (Group/.getChildren table)))

(defn- attribute-label [k schema table]
  (let [label (ui/label ;(str "[GRAY]:" (namespace k) "[]/" (name k))
                        (name k))
        delete-button (when (m/optional? k (schema/malli-form schema ctx/schemas))
                        (ui/text-button "-"
                                        (fn []
                                          (Actor/.remove (find-kv-widget table k))
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
    (stage/add-actor! ctx/stage window)))

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
        (for [widget (filter value-widget? (Group/.getChildren table))
              :let [[k _] (Actor/.getUserObject widget)]]
          [k (widget/value (get ctx/schemas k) widget)])))

; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
#_(defn- texture-rows []
  (for [file (sort (assets/all-of-type ctx/assets :texture))]
    [(ui/image-button (image file) (fn []))]
    #_[(ui/text-button file (fn []))]))

(defmethod widget/create :s/image [schema image]
  (ui/image-button (schema/edn->value schema image)
                   (fn on-clicked [])
                   {:scale 2})
  #_(ui/image-button image
                     #(stage/add-actor! ctx/stage (scrollable-choose-window (texture-rows)))
                     {:dimensions [96 96]})) ; x2  , not hardcoded here

(defmethod widget/create :s/animation [_ animation]
  (ui/table {:rows [(for [image (:frames animation)]
                      (ui/image-button (schema/edn->value :s/image image)
                                       (fn on-clicked [])
                                       {:scale 2}))]
             :cell-defaults {:pad 1}}))

; FIXME overview table not refreshed after changes in properties

(defn- edit-property [id]
  (stage/add-actor! ctx/stage (editor-window (db/get-raw ctx/db id))))

; TODO unused code below

(import '(com.kotcrab.vis.ui.widget.tabbedpane Tab TabbedPane TabbedPaneAdapter))

(defn- property-type-tabs []
  (for [property-type (sort (filter #(= "properties" (namespace %)) (keys ctx/schemas)))]
    {:title (str/capitalize (name property-type))
     :content (overview-table property-type edit-property)}))

(defn- tab-widget [{:keys [title content savable? closable-by-user?]}]
  (proxy [Tab] [(boolean savable?) (boolean closable-by-user?)]
    (getTabTitle [] title)
    (getContentTable [] content)))

#_(defn tabs-table []
  (let [label-str "foobar"
        table (ui/table {:fill-parent? true})
        container (ui/table {})
        tabbed-pane (TabbedPane.)]
    (.addListener tabbed-pane
                  (proxy [TabbedPaneAdapter] []
                    (switchedTab [^Tab tab]
                      (Group/.getChildren container)
                      (.fill (.expand (.add container (.getContentTable tab)))))))
    (.fillX (.expandX (.add table (.getTable tabbed-pane))))
    (.row table)
    (.fill (.expand (.add table container)))
    (.row table)
    (.pad (.left (.add table (ui/label label-str))) (float 10))
    (doseq [tab-data (property-type-tabs)]
      (.add tabbed-pane (tab-widget tab-data)))
    table))

#_(defn- background-image [path]
    (ui/image-widget (ctx/assets path)
                     {:fill-parent? true
                      :scaling :fill
                      :align :center}))

#_(defn create []
  ; TODO cannot find asset when starting from 'moon' ...
  ; because assets are searhed and loaded differently ...
  (doseq [actor [(background-image "images/moon_background.png")
                 (tabs-table       "custom label text here")]]
    (stage/add-actor! ctx/stage actor)))

(defn open-editor-window! [property-type]
  (let [window (ui/window {:title "Edit"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true})]
    (Table/.add window ^Actor (overview-table property-type edit-property))
    (.pack window)
    (stage/add-actor! ctx/stage window)))
