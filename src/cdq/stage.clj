(ns cdq.stage
  (:require [cdq.audio.sound :as sound]
            [cdq.assets :as assets]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.db.property :as property]
            [cdq.db.schema :as schema]
            [cdq.data.val-max :as val-max]
            [cdq.entity :as entity]
            [cdq.entity.inventory :as inventory]
            [cdq.entity.state :as state]
            [cdq.g.db :as g.db]
            [cdq.graphics :as graphics]
            [clojure.gdx.graphics.color :as color]
            [cdq.info :as info]
            [clojure.data.grid2d :as g2d]
            [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.input :as input]
            [cdq.stage.ui :refer [image-button
                                  text-button
                                  *on-clicked-actor*
                                  image->widget
                                  ui-stack
                                  text-field
                                  add-tooltip!]
             :as ui]
            [clojure.gdx.graphics :as gdx.graphics]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.string :as str]
            [clojure.utils :as utils])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.scenes.scene2d Actor Group Stage Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Label Table Button ButtonGroup Image Widget Window)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable TextureRegionDrawable ClickListener)
           (com.badlogic.gdx.math Vector2)
           (com.kotcrab.vis.ui.widget Separator Menu MenuBar MenuItem PopupMenu VisWindow)))

(defn add-actor! [^Stage stage actor]
  (.addActor stage actor))

(defn show-error-window! [stage throwable]
  (add-actor! stage
              (ui/window {:title "Error"
                          :rows [[(ui/label (binding [*print-level* 3]
                                              (utils/with-err-str
                                                (clojure.repl/pst throwable))))]]
                          :modal? true
                          :close-button? true
                          :close-on-escape? true
                          :center? true
                          :pack? true})))

(defn- find-ancestor-window ^Window [actor]
  (if-let [p (Actor/.getParent actor)]
    (if (instance? Window p)
      p
      (find-ancestor-window p))
    (throw (Error. (str "Actor has no parent window " actor)))))

(defn- pack-ancestor-window! [actor]
  (.pack (find-ancestor-window actor)))

(defn- horizontal-separator-cell [colspan]
  {:actor (Separator. "default")
   :pad-top 2
   :pad-bottom 2
   :colspan colspan
   :fill-x? true
   :expand-x? true})

(defn- vertical-separator-cell []
  {:actor (Separator. "vertical")
   :pad-top 2
   :pad-bottom 2
   :fill-y? true
   :expand-y? true})

(defn- property->image [{:keys [entity/image entity/animation]}]
  (or image
      (first (:frames animation))))

(defn- get-schemas []
  (:schemas ctx/db))

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
     :height (min (- (:height (:ui-viewport ctx/graphics)) 50)
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
          (show-error-window! ctx/stage t))))

; We are working with raw property data without edn->value and build
; otherwise at update! we would have to convert again from edn->value back to edn
; for example at images/relationships
(defn- editor-window [props]
  (let [schemas (get-schemas)
        schema (schema/schema-of schemas (property/type props))
        window (ui/window {:title (str "[SKY]Property[]")
                           :id :property-editor-window
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        widget (schema->widget schema props)
        save!   (apply-context-fn window #(do
                                           (alter-var-root #'ctx/db db/update (->value schema widget))
                                           (db/save! ctx/db)))
        delete! (apply-context-fn window #(do
                                           (alter-var-root #'ctx/db db/delete (:property/id props))
                                           (db/save! ctx/db)))]
    (ui/add-rows! window [[(scroll-pane-cell [[{:actor widget :colspan 2}]
                                              [{:actor (text-button "Save [LIGHT_GRAY](ENTER)[]" save!)
                                                :center? true}
                                               {:actor (text-button "Delete" delete!)
                                                :center? true}]])]])
    (.addActor window (proxy [Actor] []
                        (act [_delta]
                          (when (input/key-just-pressed? gdx/input :enter)
                            (save!)))))
    (.pack window)
    window))

(defmethod schema->widget :default [_ v]
  (ui/label (utils/truncate (utils/->edn-str v) 60)))

(defmethod ->value :default [_ widget]
  ((Actor/.getUserObject widget) 1))

(defmethod schema->widget :widget/edn [schema v]
  (add-tooltip! (text-field (utils/->edn-str v) {})
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
  (ui/select-box {:items (map utils/->edn-str (rest schema))
                  :selected (utils/->edn-str v)}))

(defmethod ->value :enum [_ widget]
  (edn/read-string (ui/selected widget)))

(defn- play-button [sound-name]
  (text-button "play!" #(sound/play! sound-name)))

(declare columns)

(defn- sound-file->sound-name [sound-file]
  (-> sound-file
      (str/replace-first "sounds/" "")
      (str/replace ".wav" "")))

(defn- choose-window [table]
  (let [rows (for [sound-name (map sound-file->sound-name (assets/all-of-type ctx/assets com.badlogic.gdx.audio.Sound))]
               [(text-button sound-name
                             (fn []
                               (Group/.clearChildren table)
                               (ui/add-rows! table [(columns table sound-name)])
                               (.remove (find-ancestor-window *on-clicked-actor*))
                               (pack-ancestor-window! table)
                               (let [[k _] (Actor/.getUserObject table)]
                                 (Actor/.setUserObject table [k sound-name]))))
                (play-button sound-name)])]
    (add-actor! ctx/stage (scrollable-choose-window rows))))

(defn- columns [table sound-name]
  [(text-button sound-name
                #(choose-window table))
   (play-button sound-name)])

(defmethod schema->widget :s/sound [_ sound-name]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (ui/add-rows! table [(if sound-name
                           (columns table sound-name)
                           [(text-button "No sound" #(choose-window table))])])
    table))

(defn- property-widget [{:keys [property/id] :as props} clicked-id-fn extra-info-text scale]
  (let [on-clicked #(clicked-id-fn id)
        button (if-let [image (property->image props)]
                 (image-button image on-clicked {:scale scale})
                 (text-button (name id) on-clicked))
        top-widget (ui/label (or (and extra-info-text (extra-info-text props)) ""))
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
                         :properties/worlds {:columns 10}
                         :properties/player-dead {:columns 1}
                         :properties/player-idle {:columns 1}
                         :properties/player-item-on-cursor {:columns 1}})

(defn overview-table [property-type clicked-id-fn]
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
                    (pack-ancestor-window! table))]
    (ui/add-rows!
     table
     [[(text-button "+"
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
                        (add-actor! ctx/stage window))))]
      (for [property-id property-ids]
        (let [property (db/build ctx/db property-id)
              image-widget (image->widget (property->image property)
                                          {:id property-id})]
          (add-tooltip! image-widget #(info-text property))))
      (for [id property-ids]
        (text-button "-" #(redo-rows (disj property-ids id))))])))

(defmethod schema->widget :s/one-to-many [[_ property-type] property-ids]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-many-rows table property-type property-ids)
    table))

(defmethod ->value :s/one-to-many [_ widget]
  (->> (Group/.getChildren widget)
       (keep Actor/.getUserObject)
       set))

(defn- add-one-to-one-rows [table property-type property-id]
  (let [redo-rows (fn [id]
                    (Group/.clearChildren table)
                    (add-one-to-one-rows table property-type id)
                    (pack-ancestor-window! table))]
    (ui/add-rows!
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
                                              (.remove window)
                                              (redo-rows id))]
                          (Table/.add table window ^Actor (overview-table property-type clicked-id-fn))
                          (.pack window)
                          (add-actor! ctx/stage window)))))]
      [(when property-id
         (let [property (db/build ctx/db property-id)
               image-widget (image->widget (property->image property)
                                           {:id property-id})]
           (add-tooltip! image-widget #(info-text property))
           image-widget))]
      [(when property-id
         (text-button "-" #(redo-rows nil)))]])))

(defmethod schema->widget :s/one-to-one [[_ property-type] property-id]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-one-rows table property-type property-id)
    table))

(defmethod ->value :s/one-to-one [_ widget]
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
   (->value [:s/map] table)))

(defn- rebuild-editor-window []
  (let [prop-value (window->property-value)]
    (Actor/.remove (get-editor-window))
    (add-actor! ctx/stage (editor-window prop-value))))

(defn- value-widget [[k v]]
  (let [widget (schema->widget (schema/schema-of (get-schemas) k)
                               v)]
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
        delete-button (when (schema/optional-k? k schema (get-schemas))
                        (text-button "-"
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
   (vertical-separator-cell)
   {:actor (value-widget [k v])
    :left? true}])

(defn- horiz-sep []
  [(horizontal-separator-cell component-row-cols)])

(defn- k->default-value [k]
  (let [schema (schema/schema-of (get-schemas) k)]
    (cond
     (#{:s/one-to-one :s/one-to-many} (schema/type schema)) nil

     ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

     :else (schema/generate schema {:size 3} (get-schemas)))))

(defn- choose-component-window [schema map-widget-table]
  (let [window (ui/window {:title "Choose"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        remaining-ks (sort (remove (set (keys (->value schema map-widget-table)))
                                   (schema/map-keys schema (get-schemas))))]
    (ui/add-rows!
     window
     (for [k remaining-ks]
       [(text-button (name k)
                     (fn []
                       (.remove window)
                       (ui/add-rows! map-widget-table [(component-row
                                                        [k (k->default-value k)]
                                                        schema
                                                        map-widget-table)])
                       (rebuild-editor-window)))]))
    (.pack window)
    (add-actor! ctx/stage window)))

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
                          (map #(component-row % schema table)
                               (utils/sort-by-k-order property-k-sort-order
                                                      m)))
        colspan component-row-cols
        opt? (schema/optional-keys-left schema m (get-schemas))]
    (ui/add-rows!
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
        (for [widget (filter value-widget? (Group/.getChildren table))
              :let [[k _] (Actor/.getUserObject widget)]]
          [k (->value (schema/schema-of (get-schemas) k) widget)])))

; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
#_(defn- texture-rows []
  (for [file (sort (assets/all-of-type ctx/assets com.badlogic.gdx.graphics.Texture))]
    [(image-button (image file) (fn []))]
    #_[(text-button file (fn []))]))

(defmethod schema->widget :s/image [schema image]
  (image-button (g.db/edn->value schema image ctx/db)
                (fn on-clicked [])
                {:scale 2})
  #_(image-button image
                  #(add-actor! ctx/stage (scrollable-choose-window (texture-rows)))
                  {:dimensions [96 96]})) ; x2  , not hardcoded here

(defmethod schema->widget :s/animation [_ animation]
  (ui/table {:rows [(for [image (:frames animation)]
                      (image-button (g.db/edn->value :s/image image ctx/db)
                                    (fn on-clicked [])
                                    {:scale 2}))]
             :cell-defaults {:pad 1}}))

; FIXME overview table not refreshed after changes in properties

(defn edit-property [id]
  (add-actor! ctx/stage (editor-window (db/get-raw ctx/db id))))

; TODO unused code below

(import '(com.kotcrab.vis.ui.widget.tabbedpane Tab TabbedPane TabbedPaneAdapter))

(defn- property-types [schemas]
  (filter #(= "properties" (namespace %))
          (keys schemas)))

(defn- property-type-tabs []
  (for [property-type (sort (property-types (get-schemas)))]
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
    (add-actor! ctx/stage actor)))

(defn- open-editor-window! [property-type]
  (let [window (ui/window {:title "Edit"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true})]
    (Table/.add window ^Actor (overview-table property-type edit-property))
    (.pack window)
    (add-actor! ctx/stage window)))

(defn- set-label-text-actor [label text-fn]
  (proxy [Actor] []
    (act [_delta]
      (Label/.setText label (str (text-fn))))))

(defn- add-upd-label!
  ([table text-fn icon]
   (let [icon (ui/image-widget icon {})
         label (ui/label "")
         sub-table (ui/table {:rows [[icon label]]})]
     (Group/.addActor table (set-label-text-actor label text-fn))
     (.expandX (.right (Table/.add table sub-table)))))
  ([table text-fn]
   (let [label (ui/label "")]
     (Group/.addActor table (set-label-text-actor label text-fn))
     (.expandX (.right (Table/.add table label))))))

(defn- add-update-labels! [menu-bar update-labels]
  (let [table (MenuBar/.getTable menu-bar)]
    (doseq [{:keys [label update-fn icon]} update-labels]
      (let [update-fn #(str label ": " (update-fn))]
        (if icon
          (add-upd-label! table update-fn icon)
          (add-upd-label! table update-fn))))))

(defn- add-menu! [menu-bar {:keys [label items]}]
  (let [app-menu (Menu. label)]
    (doseq [{:keys [label on-click]} items]
      (PopupMenu/.addItem app-menu (doto (MenuItem. label)
                                     (.addListener (ui/change-listener (or on-click (fn [])))))))
    (MenuBar/.addMenu menu-bar app-menu)))

(defn- create-menu [{:keys [menus update-labels]}]
  (ui/table {:rows [[{:actor (let [menu-bar (MenuBar.)]
                               (run! #(add-menu! menu-bar %) menus)
                               (add-update-labels! menu-bar update-labels)
                               (MenuBar/.getTable menu-bar))
                      :expand-x? true
                      :fill-x? true
                      :colspan 1}]
                    [{:actor (doto (ui/label "")
                               (Actor/.setTouchable Touchable/disabled))
                      :expand? true
                      :fill-x? true
                      :fill-y? true}]]
             :fill-parent? true}))

; Items are also smaller than 48x48 all of them
; so wasting space ...
; can maybe make a smaller textureatlas or something...

(def ^:private cell-size 48)
(def ^:private droppable-color   [0   0.6 0 0.8])
(def ^:private not-allowed-color [0.6 0   0 0.8])

(defn- draw-cell-rect! [g player-entity x y mouseover? cell]
  (graphics/draw-rectangle g x y cell-size cell-size :gray)
  (when (and mouseover?
             (= :player-item-on-cursor (entity/state-k player-entity)))
    (let [item (:entity/item-on-cursor player-entity)
          color (if (inventory/valid-slot? cell item)
                  droppable-color
                  not-allowed-color)]
      (graphics/draw-filled-rectangle g (inc x) (inc y) (- cell-size 2) (- cell-size 2) color))))

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-rect-actor []
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (let [g ctx/graphics
            ^Actor actor this]
        (draw-cell-rect! g
                         @ctx/player-eid
                         (.getX actor)
                         (.getY actor)
                         (let [[x y] (graphics/mouse-position g)
                               v (.stageToLocalCoordinates actor (Vector2. x y))]
                           (Actor/.hit actor (.x v) (.y v) true))
                         (Actor/.getUserObject (.getParent actor)))))))

(def ^:private slot->y-sprite-idx
  #:inventory.slot {:weapon   0
                    :shield   1
                    :rings    2
                    :necklace 3
                    :helm     4
                    :cloak    5
                    :chest    6
                    :leg      7
                    :glove    8
                    :boot     9
                    :bag      10}) ; transparent

(defn- slot->sprite-idx [slot]
  [21 (+ (slot->y-sprite-idx slot) 2)])

(defn- slot->sprite [slot]
  (graphics/from-sheet ctx/graphics
                       (graphics/sprite-sheet ctx/graphics (ctx/assets "images/items.png") 48 48)
                       (slot->sprite-idx slot)))

(defn- slot->background [slot]
  (let [drawable (-> (slot->sprite slot)
                     :texture-region
                     ui/texture-region-drawable)]
    (BaseDrawable/.setMinSize drawable (float cell-size) (float cell-size))
    (TextureRegionDrawable/.tint drawable (color/create 1 1 1 0.4))))

(defn- ->cell [slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]]
    (doto (ui/ui-stack [(draw-rect-actor)
                        (ui/image-widget (slot->background slot) {:id :image})])
      (.setName "inventory-cell")
      (.setUserObject cell)
      (.addListener (proxy [ClickListener] []
                      (clicked [_event _x _y]
                        (state/clicked-inventory-cell (entity/state-obj @ctx/player-eid) cell)))))))

(defn- inventory-table []
  (ui/table {:id ::table
             :rows (concat [[nil nil
                             (->cell :inventory.slot/helm)
                             (->cell :inventory.slot/necklace)]
                            [nil
                             (->cell :inventory.slot/weapon)
                             (->cell :inventory.slot/chest)
                             (->cell :inventory.slot/cloak)
                             (->cell :inventory.slot/shield)]
                            [nil nil
                             (->cell :inventory.slot/leg)]
                            [nil
                             (->cell :inventory.slot/glove)
                             (->cell :inventory.slot/rings :position [0 0])
                             (->cell :inventory.slot/rings :position [1 0])
                             (->cell :inventory.slot/boot)]]
                           (for [y (range (g2d/height (:inventory.slot/bag inventory/empty-inventory)))]
                             (for [x (range (g2d/width (:inventory.slot/bag inventory/empty-inventory)))]
                               (->cell :inventory.slot/bag :position [x y]))))}))

(defn- inventory-window [position]
  (ui/window {:title "Inventory"
              :id :inventory-window
              :visible? false
              :pack? true
              :position position
              :rows [[{:actor (inventory-table)
                       :pad 4}]]}))

(defn- get-cell-widget [stage cell]
  (get (::table (-> stage :windows :inventory-window)) cell))

(defn set-item! [stage cell item]
  (let [cell-widget (get-cell-widget stage cell)
        image-widget (get cell-widget :image)
        drawable (ui/texture-region-drawable (:texture-region (:entity/image item)))]
    (BaseDrawable/.setMinSize drawable (float cell-size) (float cell-size))
    (Image/.setDrawable image-widget drawable)
    (ui/add-tooltip! cell-widget #(info/text item))))

(defn remove-item! [stage cell]
  (let [cell-widget (get-cell-widget stage cell)
        image-widget (get cell-widget :image)]
    (Image/.setDrawable image-widget (slot->background (cell 0)))
    (ui/remove-tooltip! cell-widget)))

(defn- action-bar []
  (ui/table {:rows [[{:actor (doto (ui/horizontal-group {:pad 2 :space 2})
                               (Actor/.setUserObject ::horizontal-group)
                               (Group/.addActor (doto (proxy [Actor] [])
                                                  (Actor/.setName "button-group")
                                                  (Actor/.setUserObject (ui/button-group {:max-check-count 1
                                                                                          :min-check-count 0})))))
                      :expand? true
                      :bottom? true}]]
             :id ::action-bar-table
             :cell-defaults {:pad 2}
             :fill-parent? true}))

(defn- action-bar-data [stage]
  (let [group (::horizontal-group (::action-bar-table stage))]
    {:horizontal-group group
     :button-group (Actor/.getUserObject (Group/.findActor group "button-group"))}))

(defn selected-skill [stage]
  (when-let [skill-button (ButtonGroup/.getChecked (:button-group (action-bar-data stage)))]
    (Actor/.getUserObject skill-button)))

(defn add-skill! [stage {:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (action-bar-data stage)
        button (ui/image-button image (fn []) {:scale 2})]
    (Actor/.setUserObject button id)
    (ui/add-tooltip! button #(info/text skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (Group/.addActor horizontal-group button)
    (ButtonGroup/.add button-group ^Button button)
    nil))

(defn remove-skill! [stage {:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (action-bar-data stage)
        button (get horizontal-group id)]
    (Actor/.remove button)
    (ButtonGroup/.remove button-group ^Button button)
    nil))

(let [disallowed-keys [:entity/skills
                       #_:entity/fsm
                       :entity/faction
                       :active-skill]]
  (defn- ->label-text []
    ; items then have 2x pretty-name
    #_(.setText (.getTitleLabel window)
                (if-let [eid ctx/mouseover-eid]
                  (info/text [:property/pretty-name (:property/pretty-name @eid)])
                  "Entity Info"))
    (when-let [eid ctx/mouseover-eid]
      (info/text ; don't use select-keys as it loses Entity record type
                 (apply dissoc @eid disallowed-keys)))))

(defn- entity-info-window [position]
  (let [label (ui/label "")
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position position
                           :rows [[{:actor label :expand? true}]]})]
    ; do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (.addActor window (proxy [Actor] []
                        (act [_delta]
                          (.setText label (str (->label-text)))
                          (.pack window))))
    window))

(defn- render-infostr-on-bar [g infostr x y h]
  (graphics/draw-text g {:text infostr
                         :x (+ x 75)
                         :y (+ y 2)
                         :up? true}))

(defn- hp-mana-bar [[x y-mana]]
  (let [rahmen      (graphics/sprite ctx/graphics (ctx/assets "images/rahmen.png"))
        hpcontent   (graphics/sprite ctx/graphics (ctx/assets "images/hp.png"))
        manacontent (graphics/sprite ctx/graphics (ctx/assets "images/mana.png"))
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [g x y contentimage minmaxval name]
                            (graphics/draw-image g rahmen [x y])
                            (graphics/draw-image g (graphics/sub-sprite g
                                                                        contentimage
                                                                        [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                                 [x y])
                            (render-infostr-on-bar g (str (utils/readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (proxy [Actor] []
      (draw [_batch _parent-alpha]
        (let [player-entity @ctx/player-eid
              x (- x (/ rahmenw 2))
              g ctx/graphics]
          (render-hpmana-bar g x y-hp   hpcontent   (entity/hitpoints player-entity) "HP")
          (render-hpmana-bar g x y-mana manacontent (entity/mana      player-entity) "MP"))))))

(defn root [^Stage stage]
  (Stage/.getRoot stage))

;"Mouseover-Actor: "
#_(when-let [actor (mouse-on-actor? ctx/stage)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))

(defn- dev-menu-config []
  {:menus [{:label "World"
            :items (for [world-fn '[cdq.level.vampire/create
                                    cdq.level.uf-caves/create
                                    cdq.level.modules/create]]
                     {:label (str "Start " (namespace world-fn))
                      :on-click (fn [] (ctx/reset-game! world-fn))})}
           {:label "Help"
            :items [{:label "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"}]}
           {:label "Objects"
            :items (for [property-type (sort (filter #(= "properties" (namespace %))
                                                     (keys (:schemas ctx/db))))]
                     {:label (str/capitalize (name property-type))
                      :on-click (fn []
                                  (open-editor-window! property-type))})}]
   :update-labels [{:label "Mouseover-entity id"
                    :update-fn (fn []
                                 (when-let [entity (and ctx/mouseover-eid @ctx/mouseover-eid)]
                                   (:entity/id entity)))
                    :icon (ctx/assets "images/mouseover.png")}
                   {:label "elapsed-time"
                    :update-fn (fn [] (str (utils/readable-number ctx/elapsed-time) " seconds"))
                    :icon (ctx/assets "images/clock.png")}
                   {:label "paused?"
                    :update-fn (fn [] ctx/paused?)}
                   {:label "GUI"
                    :update-fn (fn [] (graphics/mouse-position ctx/graphics))}
                   {:label "World"
                    :update-fn (fn [] (mapv int (graphics/world-mouse-position ctx/graphics)))}
                   {:label "Zoom"
                    :update-fn (fn [] (camera/zoom (:camera (:world-viewport ctx/graphics))))
                    :icon (ctx/assets "images/zoom.png")}
                   {:label "FPS"
                    :update-fn (fn [] (gdx.graphics/frames-per-second gdx/graphics))
                    :icon (ctx/assets "images/fps.png")}]})

(defn- player-state-actor []
  (proxy [Actor] []
    (draw [_batch _parent-alpha]
      (state/draw-gui-view (entity/state-obj @ctx/player-eid)))))

(defn- player-message []
  (doto (proxy [Actor] []
          (draw [_batch _parent-alpha]
            (let [g ctx/graphics
                  state (Actor/.getUserObject this)]
              (when-let [text (:text @state)]
                (graphics/draw-text g {:x (/ (:width     (:ui-viewport g)) 2)
                                       :y (+ (/ (:height (:ui-viewport g)) 2) 200)
                                       :text text
                                       :scale 2.5
                                       :up? true}))))
          (act [delta]
            (let [state (Actor/.getUserObject this)]
              (when (:text @state)
                (swap! state update :counter + delta)
                (when (>= (:counter @state) 1.5)
                  (reset! state nil))))))
    (.setUserObject (atom nil))
    (.setName "player-message-actor")))

(defn show-message! [stage text]
  (Actor/.setUserObject (Group/.findActor (root stage) "player-message-actor")
                        (atom {:text text
                               :counter 0})))

(defn- create-actors []
  [(create-menu (dev-menu-config))
   (action-bar)
   (hp-mana-bar [(/ (:width (:ui-viewport ctx/graphics)) 2)
                 80 ; action-bar-icon-size
                 ])
   (ui/group {:id :windows
              :actors [(entity-info-window [(:width (:ui-viewport ctx/graphics)) 0])
                       (inventory-window [(:width  (:ui-viewport ctx/graphics))
                                          (:height (:ui-viewport ctx/graphics))])]})
   (player-state-actor)
   (player-message)])

(defn create! []
  (ui/load! {:skin-scale :x1} #_(:vis-ui config))
  (let [stage (proxy [Stage ILookup] [(:ui-viewport ctx/graphics)
                                      (:batch       ctx/graphics)]
                (valAt
                  ([id]
                   (ui/find-actor-with-id (root this) id))
                  ([id not-found]
                   (or (ui/find-actor-with-id (root this) id)
                       not-found))))]
    (run! (partial add-actor! stage) (create-actors))
    (input/set-processor! gdx/input stage)
    stage))

(defn draw! [^Stage stage]
  (.draw stage))

(defn act! [^Stage stage]
  (.act stage))

; (viewport/unproject-mouse-position (stage/viewport stage))
; => move ui-viewport inside stage?
; => viewport/unproject-mouse-position ? -> already exists!
; => stage/resize-viewport! need to add (for viewport)
(defn mouse-on-actor? [^Stage stage]
  (let [[x y] (graphics/mouse-position ctx/graphics)]
    (.hit stage x y true)))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal! [stage {:keys [title text button-text on-click]}]
  (assert (not (::modal stage)))
  (add-actor! stage
              (ui/window {:title title
                          :rows [[(ui/label text)]
                                 [(ui/text-button button-text
                                                  (fn []
                                                    (Actor/.remove (::modal stage))
                                                    (on-click)))]]
                          :id ::modal
                          :modal? true
                          :center-position [(/ (:width  (:ui-viewport ctx/graphics)) 2)
                                            (* (:height (:ui-viewport ctx/graphics)) (/ 3 4))]
                          :pack? true})))

(defn- toggle-visible! [^Actor actor]
  (.setVisible actor (not (.isVisible actor))))

(defn check-window-controls! [stage]
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (input/key-just-pressed? gdx/input (get window-hotkeys window-id))]
      (toggle-visible! (get (:windows stage) window-id))))
  (when (input/key-just-pressed? gdx/input :escape)
    (let [windows (Group/.getChildren (:windows stage))]
      (when (some Actor/.isVisible windows)
        (run! #(Actor/.setVisible % false) windows)))))

(defn inventory-visible? [stage]
  (-> stage :windows :inventory-window Actor/.isVisible))

(defn toggle-inventory-visible! [stage]
  (-> stage :windows :inventory-window toggle-visible!))

(defn inventory-cell-with-item? [actor]
  (and (Actor/.getParent actor)
       (= "inventory-cell" (Actor/.getName (Actor/.getParent actor)))
       (get-in (:entity/inventory @ctx/player-eid)
               (Actor/.getUserObject (Actor/.getParent actor)))))

(defn window-title-bar? ; TODO buggy FIXME
  "Returns true if the actor is a window title bar."
  [^Actor actor]
  (when (instance? Label actor)
    (when-let [p (.getParent actor)]
      (when-let [p (.getParent p)]
        (and (instance? VisWindow actor)
             (= (.getTitleLabel ^Window p) actor))))))

(defn- button-class? [actor]
  (some #(= Button %) (supers (class actor))))

(defn button?
  "Returns true if the actor or its parent is a button."
  [^Actor actor]
  (or (button-class? actor)
      (and (.getParent actor)
           (button-class? (.getParent actor)))))
