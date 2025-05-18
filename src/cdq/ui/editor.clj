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
            [clojure.string :as str]
            [gdl.assets :as assets]
            [gdl.input :as input]
            [gdl.ui :as ui]
            [gdl.ui.stage :as stage])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui Table)))

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
    (ui/add-rows! window [[(scroll-pane/table-cell [[{:actor widget :colspan 2}]
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

(defn- get-editor-window []
  (:property-editor-window ctx/stage))

(defn- window->property-value []
 (let [window (get-editor-window)
       scroll-pane-table (ui/find-actor (:scroll-pane window) "scroll-pane-table")
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
                    (ui/children table)))

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
        (for [widget (filter value-widget? (ui/children table))
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
                     #(stage/add-actor! ctx/stage (scroll-pane/choose-window (texture-rows)))
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
     :content (overview-table/create property-type edit-property)}))

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
                      (ui/children container)
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
    (Table/.add window ^Actor (overview-table/create property-type edit-property))
    (.pack window)
    (stage/add-actor! ctx/stage window)))
