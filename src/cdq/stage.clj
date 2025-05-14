(ns cdq.stage
  (:require [cdq.audio.sound :as sound]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.db.property :as property]
            [cdq.db.schema :as schema]
            [cdq.entity :as entity]
            [cdq.entity.inventory :as inventory]
            [cdq.entity.state :as state]
            [cdq.graphics :as graphics]
            [cdq.grid2d :as g2d]
            [cdq.info :as info]
            [cdq.schemas :as schemas]
            [cdq.utils :as utils]
            [cdq.val-max :as val-max]
            [clojure.edn :as edn]
            [clojure.string :as str])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx Gdx Input$Keys)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Color Texture OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor Group Stage Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Cell Table Image Button WidgetGroup Stack HorizontalGroup VerticalGroup Tree$Node Label Table Button ButtonGroup Image Widget Window)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable TextureRegionDrawable ClickListener Drawable ChangeListener)
           (com.badlogic.gdx.utils Align Scaling)
           (com.badlogic.gdx.math Vector2)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Separator Menu MenuBar MenuItem PopupMenu VisTable Tooltip VisImage VisTextButton VisCheckBox VisSelectBox VisImageButton VisTextField VisLabel VisScrollPane VisTree VisWindow)))

(defn- find-actor-with-id [^Group group id]
  (let [actors (.getChildren group)
        ids (keep Actor/.getUserObject actors)]
    (assert (or (empty? ids)
                (apply distinct? ids)) ; TODO could check @ add
            (str "Actor ids are not distinct: " (vec ids)))
    (first (filter #(= id (Actor/.getUserObject %)) actors))))

(defmacro ^:private proxy-ILookup
  "For actors inheriting from Group."
  [class args]
  `(proxy [~class clojure.lang.ILookup] ~args
     (valAt
       ([id#]
        (find-actor-with-id ~'this id#))
       ([id# not-found#]
        (or (find-actor-with-id ~'this id#) not-found#)))))

(defn- load-vis-ui! [{:keys [skin-scale]}]
  ; app crashes during startup before VisUI/dispose and we do cdq.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (VisUI/isLoaded)
    (VisUI/dispose))
  (VisUI/load (case skin-scale
                :x1 VisUI$SkinScale/X1
                :x2 VisUI$SkinScale/X2))
  (-> (VisUI/getSkin)
      (.getFont "default-font")
      .getData
      .markupEnabled
      (set! true))
  ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
  ;Controls whether to fade out tooltip when mouse was moved. (default false)
  ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0)))


(comment
 ; fill parent & pack is from Widget TODO ( not widget-group ?)
 com.badlogic.gdx.scenes.scene2d.ui.Widget
 ; about .pack :
 ; Generally this method should not be called in an actor's constructor because it calls Layout.layout(), which means a subclass would have layout() called before the subclass' constructor. Instead, in constructors simply set the actor's size to Layout.getPrefWidth() and Layout.getPrefHeight(). This allows the actor to have a size at construction time for more convenient use with groups that do not layout their children.
 )

(defn- set-widget-group-opts [^WidgetGroup widget-group {:keys [fill-parent? pack?]}]
  (.setFillParent widget-group (boolean fill-parent?)) ; <- actor? TODO
  (when pack?
    (.pack widget-group))
  widget-group)

(defn- set-cell-opts! [^Cell cell opts]
  (doseq [[option arg] opts]
    (case option
      :fill-x?    (.fillX     cell)
      :fill-y?    (.fillY     cell)
      :expand?    (.expand    cell)
      :expand-x?  (.expandX   cell)
      :expand-y?  (.expandY   cell)
      :bottom?    (.bottom    cell)
      :colspan    (.colspan   cell (int   arg))
      :pad        (.pad       cell (float arg))
      :pad-top    (.padTop    cell (float arg))
      :pad-bottom (.padBottom cell (float arg))
      :width      (.width     cell (float arg))
      :height     (.height    cell (float arg))
      :center?    (.center    cell)
      :right?     (.right     cell)
      :left?      (.left      cell))))

(defn- add-rows!
  "rows is a seq of seqs of columns.
  Elements are actors or nil (for just adding empty cells ) or a map of
  {:actor :expand? :bottom?  :colspan int :pad :pad-bottom}. Only :actor is required."
  [^Table table rows]
  (doseq [row rows]
    (doseq [props-or-actor row]
      (cond
       (map? props-or-actor) (-> (.add table ^Actor (:actor props-or-actor))
                                 (set-cell-opts! (dissoc props-or-actor :actor)))
       :else (.add table ^Actor props-or-actor)))
    (.row table))
  table)

(defn- set-table-opts! [^Table table {:keys [rows cell-defaults]}]
  (set-cell-opts! (.defaults table) cell-defaults)
  (add-rows! table rows))

(defn- set-actor-opts! [^Actor actor {:keys [id
                                             name
                                             visible?
                                             touchable
                                             center-position
                                             position] :as opts}]
  (when id
    (.setUserObject actor id))
  (when name
    (.setName actor name))
  (when (contains? opts :visible?)
    (.setVisible actor (boolean visible?)))
  (when-let [[x y] center-position]
    (.setPosition actor
                  (- x (/ (.getWidth  actor) 2))
                  (- y (/ (.getHeight actor) 2))))
  (when-let [[x y] position]
    (.setPosition actor x y))
  actor)

(defn- set-opts! [actor opts]
  (set-actor-opts! actor opts)
  (when (instance? Table actor)
    (set-table-opts! actor opts)) ; before widget-group-opts so pack is packing rows
  (when (instance? WidgetGroup actor)
    (set-widget-group-opts actor opts))
  actor)

(defn- ->group [{:keys [actors] :as opts}]
  (let [group (proxy-ILookup Group [])]
    (run! #(Group/.addActor group %) actors)
    (set-opts! group opts)))

(defn- horizontal-group ^HorizontalGroup [{:keys [space pad]}]
  (let [group (proxy-ILookup HorizontalGroup [])]
    (when space (.space group (float space)))
    (when pad   (.pad   group (float pad)))
    group))

(defn- vertical-group [actors]
  (let [group (proxy-ILookup VerticalGroup [])]
    (run! #(Group/.addActor group %) actors)
    group))

(defn- add-tooltip!
  "tooltip-text is a (fn [context]) or a string. If it is a function will be-recalculated every show.
  Returns the actor."
  [^Actor actor tooltip-text]
  (let [text? (string? tooltip-text)
        label (VisLabel. (if text? tooltip-text ""))
        tooltip (proxy [Tooltip] []
                  ; hooking into getWidth because at
                  ; https://github.com/kotcrab/vis-blob/master/ui/src/main/java/com/kotcrab/vis/ui/widget/Tooltip.java#L271
                  ; when tooltip position gets calculated we setText (which calls pack) before that
                  ; so that the size is correct for the newly calculated text.
                  (getWidth []
                    (let [^Tooltip this this]
                      (when-not text?
                        (.setText this (str (tooltip-text))))
                      (proxy-super getWidth))))]
    (.setAlignment label Align/center)
    (.setTarget  tooltip actor)
    (.setContent tooltip label))
  actor)

(defn- remove-tooltip! [^Actor actor]
  (Tooltip/removeTooltip actor))

(defn- button-group [{:keys [max-check-count min-check-count]}]
  (doto (ButtonGroup.)
    (.setMaxCheckCount max-check-count)
    (.setMinCheckCount min-check-count)))

(defn- check-box
  "on-clicked is a fn of one arg, taking the current isChecked state"
  [text on-clicked checked?]
  (let [^Button button (VisCheckBox. (str text))]
    (.setChecked button checked?)
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event ^Button actor]
                      (on-clicked (.isChecked actor)))))
    button))

(defn- select-box [{:keys [items selected]}]
  (doto (VisSelectBox.)
    (.setItems ^"[Lcom.badlogic.gdx.scenes.scene2d.Actor;" (into-array items))
    (.setSelected selected)))

(defn- ->table ^Table [opts]
  (-> (proxy-ILookup VisTable [])
      (set-opts! opts)))

(defn- ->window ^VisWindow [{:keys [title modal? close-button? center? close-on-escape?] :as opts}]
  (-> (let [window (doto (proxy-ILookup VisWindow [^String title true]) ; true = showWindowBorder
                     (.setModal (boolean modal?)))]
        (when close-button?    (.addCloseButton window))
        (when center?          (.centerWindow   window))
        (when close-on-escape? (.closeOnEscape  window))
        window)
      (set-opts! opts)))

(defn- ->label ^VisLabel [text]
  (VisLabel. ^CharSequence text))

(defn- text-field [text opts]
  (-> (VisTextField. (str text))
      (set-opts! opts)))

(defn- ->stack ^Stack [actors]
  (proxy-ILookup Stack [(into-array Actor actors)]))

(defmulti ^:private image* type)

(defmethod image* Drawable [^Drawable drawable]
  (VisImage. drawable))

(defmethod image* Texture [^Texture texture]
  (VisImage. (TextureRegion. texture)))

(defmethod image* TextureRegion [^TextureRegion tr]
  (VisImage. tr))

(defn- image-widget ; TODO widget also make, for fill parent
  "Takes either a texture-region or drawable. Opts are :scaling, :align and actor opts."
  [object {:keys [scaling align fill-parent?] :as opts}]
  (-> (let [^Image image (image* object)]
        (when (= :center align)
          (.setAlign image Align/center))
        (when (= :fill scaling)
          (.setScaling image Scaling/fill))
        (when fill-parent?
          (.setFillParent image true))
        image)
      (set-opts! opts)))

(defn- image->widget
  "Same opts as [[image-widget]]."
  [image opts]
  (image-widget (:texture-region image) opts))

(defn- scroll-pane [actor]
  (let [scroll-pane (VisScrollPane. actor)]
    (.setUserObject scroll-pane :scroll-pane)
    (.setFlickScroll scroll-pane false)
    (.setFadeScrollBars scroll-pane false)
    scroll-pane))

(declare ^:private ^:dynamic *on-clicked-actor*)

(defn- change-listener ^ChangeListener [on-clicked]
  (proxy [ChangeListener] []
    (changed [event actor]
      (binding [*on-clicked-actor* actor]
        (on-clicked)))))

(defn- text-button [text on-clicked]
  (let [button (VisTextButton. (str text))]
    (.addListener button (change-listener on-clicked))
    button))

(defn- image-button
  ([image on-clicked]
   (image-button image on-clicked {}))
  ([{:keys [^TextureRegion texture-region]} on-clicked {:keys [scale]}]
   (let [drawable (TextureRegionDrawable. texture-region)
         button (VisImageButton. ^Drawable drawable)]
     (when scale
       (let [[w h] [(.getRegionWidth  texture-region)
                    (.getRegionHeight texture-region)]]
         (BaseDrawable/.setMinSize drawable
                                   (float (* scale w))
                                   (float (* scale h)))))
     (.addListener button (change-listener on-clicked))
     button)))

(defn- tree-node ^Tree$Node [actor]
  (proxy [Tree$Node] [actor]))

(defn- add-actor! [^Stage stage actor]
  (.addActor stage actor))

(defmacro ^:private with-err-str
  "Evaluates exprs in a context in which *err* is bound to a fresh
  StringWriter.  Returns the string created by any nested printing
  calls."
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*err* s#]
       ~@body
       (str s#))))

(defn show-error-window! [stage throwable]
  (add-actor! stage
              (->window {:title "Error"
                         :rows [[(->label (binding [*print-level* 3]
                                            (with-err-str
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

(defmulti ^:private schema->widget widget-type)
(defmulti ^:private ->value        widget-type)

(defn- scroll-pane-cell [rows]
  (let [table (->table {:rows rows
                        :name "scroll-pane-table"
                        :cell-defaults {:pad 5}
                        :pack? true})]
    {:actor (scroll-pane table)
     :width  (+ (.getWidth table) 50)
     :height (min (- (:height (:ui-viewport ctx/graphics)) 50)
                  (.getHeight table))}))

(defn- scrollable-choose-window [rows]
  (->window {:title "Choose"
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
  (let [schema (schema/schema-of (:schemas ctx/schemas) (property/type props))
        window (->window {:title (str "[SKY]Property[]")
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
    (add-rows! window [[(scroll-pane-cell [[{:actor widget :colspan 2}]
                                           [{:actor (text-button "Save [LIGHT_GRAY](ENTER)[]" save!)
                                             :center? true}
                                            {:actor (text-button "Delete" delete!)
                                             :center? true}]])]])
    (.addActor window (proxy [Actor] []
                        (act [_delta]
                          (when (.isKeyJustPressed Gdx/input Input$Keys/ENTER)
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

(defmethod schema->widget :default [_ v]
  (->label (truncate (->edn-str v) 60)))

(defmethod ->value :default [_ widget]
  ((Actor/.getUserObject widget) 1))

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

(defn- play-button [sound-name]
  (text-button "play!" #(sound/play! sound-name)))

(declare columns)

(defn- sound-file->sound-name [sound-file]
  (-> sound-file
      (str/replace-first "sounds/" "")
      (str/replace ".wav" "")))

(defn- assets-of-type [asset-type]
  (filter #(= (AssetManager/.getAssetType ctx/assets %) asset-type)
          (AssetManager/.getAssetNames ctx/assets)))

(defn- choose-window [table]
  (let [rows (for [sound-name (map sound-file->sound-name (assets-of-type Sound))]
               [(text-button sound-name
                             (fn []
                               (Group/.clearChildren table)
                               (add-rows! table [(columns table sound-name)])
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
  (let [table (->table {:cell-defaults {:pad 5}})]
    (add-rows! table [(if sound-name
                        (columns table sound-name)
                        [(text-button "No sound" #(choose-window table))])])
    table))

(defn- property-widget [{:keys [property/id] :as props} clicked-id-fn extra-info-text scale]
  (let [on-clicked #(clicked-id-fn id)
        button (if-let [image (property->image props)]
                 (image-button image on-clicked {:scale scale})
                 (text-button (name id) on-clicked))
        top-widget (->label (or (and extra-info-text (extra-info-text props)) ""))
        stack (->stack [button top-widget])]
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
    (->table
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
    (add-rows!
     table
     [[(text-button "+"
                    (fn []
                      (let [window (->window {:title "Choose"
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
  (let [table (->table {:cell-defaults {:pad 5}})]
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
    (add-rows!
     table
     [[(when-not property-id
         (text-button "+"
                      (fn []
                        (let [window (->window {:title "Choose"
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
  (let [table (->table {:cell-defaults {:pad 5}})]
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
  (let [widget (schema->widget (schema/schema-of (:schemas ctx/schemas) k)
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
  (let [label (->label ;(str "[GRAY]:" (namespace k) "[]/" (name k))
                       (name k))
        delete-button (when (schema/optional-k? k schema (:schemas ctx/schemas))
                        (text-button "-"
                                     (fn []
                                       (Actor/.remove (find-kv-widget table k))
                                       (rebuild-editor-window))))]
    (->table {:cell-defaults {:pad 2}
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
  (let [schema (schema/schema-of (:schemas ctx/schemas) k)]
    (cond
     (#{:s/one-to-one :s/one-to-many} (schema/type schema)) nil

     ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

     :else (schema/generate schema {:size 3} (:schemas ctx/schemas)))))

(defn- choose-component-window [schema map-widget-table]
  (let [window (->window {:title "Choose"
                          :modal? true
                          :close-button? true
                          :center? true
                          :close-on-escape? true
                          :cell-defaults {:pad 5}})
        remaining-ks (sort (remove (set (keys (->value schema map-widget-table)))
                                   (schema/map-keys schema (:schemas ctx/schemas))))]
    (add-rows!
     window
     (for [k remaining-ks]
       [(text-button (name k)
                     (fn []
                       (.remove window)
                       (add-rows! map-widget-table [(component-row
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
  (let [table (->table {:cell-defaults {:pad 5}
                        :id :map-widget})
        component-rows (interpose-f horiz-sep
                          (map #(component-row % schema table)
                               (utils/sort-by-k-order property-k-sort-order
                                                      m)))
        colspan component-row-cols
        opt? (schema/optional-keys-left schema m (:schemas ctx/schemas))]
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
        (for [widget (filter value-widget? (Group/.getChildren table))
              :let [[k _] (Actor/.getUserObject widget)]]
          [k (->value (schema/schema-of (:schemas ctx/schemas) k) widget)])))

; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
#_(defn- texture-rows []
  (for [file (sort (assets-of-type Texture))]
    [(image-button (image file) (fn []))]
    #_[(text-button file (fn []))]))

(defmethod schema->widget :s/image [schema image]
  (image-button (schema/edn->value schema image)
                (fn on-clicked [])
                {:scale 2})
  #_(image-button image
                  #(add-actor! ctx/stage (scrollable-choose-window (texture-rows)))
                  {:dimensions [96 96]})) ; x2  , not hardcoded here

(defmethod schema->widget :s/animation [_ animation]
  (->table {:rows [(for [image (:frames animation)]
                     (image-button (schema/edn->value :s/image image)
                                   (fn on-clicked [])
                                   {:scale 2}))]
            :cell-defaults {:pad 1}}))

; FIXME overview table not refreshed after changes in properties

(defn- edit-property [id]
  (add-actor! ctx/stage (editor-window (db/get-raw ctx/db id))))

; TODO unused code below

(import '(com.kotcrab.vis.ui.widget.tabbedpane Tab TabbedPane TabbedPaneAdapter))

(defn- property-type-tabs []
  (for [property-type (sort (schemas/property-types ctx/schemas))]
    {:title (str/capitalize (name property-type))
     :content (overview-table property-type edit-property)}))

(defn- tab-widget [{:keys [title content savable? closable-by-user?]}]
  (proxy [Tab] [(boolean savable?) (boolean closable-by-user?)]
    (getTabTitle [] title)
    (getContentTable [] content)))

#_(defn tabs-table []
  (let [label-str "foobar"
        table (->table {:fill-parent? true})
        container (->table {})
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
    (.pad (.left (.add table (->label label-str))) (float 10))
    (doseq [tab-data (property-type-tabs)]
      (.add tabbed-pane (tab-widget tab-data)))
    table))

#_(defn- background-image [path]
    (image-widget (ctx/assets path)
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
  (let [window (->window {:title "Edit"
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
   (let [icon (image-widget icon {})
         label (->label "")
         sub-table (->table {:rows [[icon label]]})]
     (Group/.addActor table (set-label-text-actor label text-fn))
     (.expandX (.right (Table/.add table sub-table)))))
  ([table text-fn]
   (let [label (->label "")]
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
                                     (.addListener (change-listener (or on-click (fn [])))))))
    (MenuBar/.addMenu menu-bar app-menu)))

(defn- create-menu [{:keys [menus update-labels]}]
  (->table {:rows [[{:actor (let [menu-bar (MenuBar.)]
                              (run! #(add-menu! menu-bar %) menus)
                              (add-update-labels! menu-bar update-labels)
                              (MenuBar/.getTable menu-bar))
                     :expand-x? true
                     :fill-x? true
                     :colspan 1}]
                   [{:actor (doto (->label "")
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
  (let [drawable (TextureRegionDrawable. ^TextureRegion (:texture-region (slot->sprite slot)))]
    (BaseDrawable/.setMinSize drawable (float cell-size) (float cell-size))
    (TextureRegionDrawable/.tint drawable (Color. (float 1) (float 1) (float 1) (float 0.4)))))

(defn- ->cell [slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]]
    (doto (->stack [(draw-rect-actor)
                    (image-widget (slot->background slot) {:id :image})])
      (.setName "inventory-cell")
      (.setUserObject cell)
      (.addListener (proxy [ClickListener] []
                      (clicked [_event _x _y]
                        (state/clicked-inventory-cell (entity/state-obj @ctx/player-eid) cell)))))))

(defn- inventory-table []
  (->table {:id ::table
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
  (->window {:title "Inventory"
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
        drawable (TextureRegionDrawable. ^TextureRegion (:texture-region (:entity/image item)))]
    (BaseDrawable/.setMinSize drawable (float cell-size) (float cell-size))
    (Image/.setDrawable image-widget drawable)
    (add-tooltip! cell-widget #(info/text item))))

(defn remove-item! [stage cell]
  (let [cell-widget (get-cell-widget stage cell)
        image-widget (get cell-widget :image)]
    (Image/.setDrawable image-widget (slot->background (cell 0)))
    (remove-tooltip! cell-widget)))

(defn- action-bar []
  (->table {:rows [[{:actor (doto (horizontal-group {:pad 2 :space 2})
                              (Actor/.setUserObject ::horizontal-group)
                              (Group/.addActor (doto (proxy [Actor] [])
                                                 (Actor/.setName "button-group")
                                                 (Actor/.setUserObject (button-group {:max-check-count 1
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
        button (image-button image (fn []) {:scale 2})]
    (Actor/.setUserObject button id)
    (add-tooltip! button #(info/text skill)) ; (assoc ctx :effect/source (world/player)) FIXME
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
  (let [label (->label "")
        window (->window {:title "Info"
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
            :items (for [property-type (sort (schemas/property-types ctx/schemas))]
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
                    :update-fn (fn [] (.zoom ^OrthographicCamera (:camera (:world-viewport ctx/graphics))))
                    :icon (ctx/assets "images/zoom.png")}
                   {:label "FPS"
                    :update-fn (fn [] (.getFramesPerSecond Gdx/graphics))
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
  (Actor/.setUserObject (Group/.findActor (Stage/.getRoot stage) "player-message-actor")
                        (atom {:text text
                               :counter 0})))

(defn- create-actors []
  [(create-menu (dev-menu-config))
   (action-bar)
   (hp-mana-bar [(/ (:width (:ui-viewport ctx/graphics)) 2)
                 80 ; action-bar-icon-size
                 ])
   (->group {:id :windows
             :actors [(entity-info-window [(:width (:ui-viewport ctx/graphics)) 0])
                      (inventory-window [(:width  (:ui-viewport ctx/graphics))
                                         (:height (:ui-viewport ctx/graphics))])]})
   (player-state-actor)
   (player-message)])

(defn create! []
  (load-vis-ui! {:skin-scale :x1} #_(:vis-ui config))
  (let [stage (proxy [Stage ILookup] [(:ui-viewport ctx/graphics)
                                      (:batch       ctx/graphics)]
                (valAt
                  ([id]
                   (find-actor-with-id (Stage/.getRoot this) id))
                  ([id not-found]
                   (or (find-actor-with-id (Stage/.getRoot this) id)
                       not-found))))]
    (run! (partial add-actor! stage) (create-actors))
    (.setInputProcessor Gdx/input stage)
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
              (->window {:title title
                         :rows [[(->label text)]
                                [(text-button button-text
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
  (let [window-hotkeys {:inventory-window   Input$Keys/I
                        :entity-info-window Input$Keys/E}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (.isKeyJustPressed Gdx/input (get window-hotkeys window-id))]
      (toggle-visible! (get (:windows stage) window-id))))
  (when (.isKeyJustPressed Gdx/input Input$Keys/ESCAPE)
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
