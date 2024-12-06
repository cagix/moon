(ns forge.core
  (:require [clojure.gdx.graphics :as g :refer [delta-time]]
            [clojure.gdx.graphics.camera :as cam]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.math.shapes :as shape]
            [clojure.gdx.math.vector2 :as v]
            [clojure.gdx.scene2d.actor :refer [user-object]]
            [clojure.gdx.scene2d.group :refer [find-actor-with-id
                                               add-actor!
                                               find-actor]]
            [clojure.gdx.scene2d.utils :as scene2d.utils]
            [clojure.gdx.tiled :as tiled]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [clojure.string :as str]
            [clojure.vis-ui :as vis]
            [data.grid2d :as g2d]
            [forge.app.asset-manager :refer [play-sound]]
            [forge.app.cursors :refer [set-cursor]]
            [forge.app.db :as db :refer [malli-form
                                         edn->value]]
            [forge.app.gui-viewport :refer [gui-viewport-width
                                            gui-viewport-height]]
            [forge.app.shape-drawer :as sd]
            [forge.app.world-viewport :refer [world-viewport-width
                                              world-viewport-height
                                              world-camera]]
            [forge.component :refer [info-text]]
            [forge.effect :refer [effects-applicable?]]
            [forge.graphics :refer [draw-text
                                    edn->image
                                    ->image]]
            [forge.screens.stage :refer [screen-stage
                                         add-actor]]
            [forge.system :refer [defsystem]]
            [forge.utils :refer [bind-root
                                 safe-get
                                 pretty-pst
                                 tile->middle
                                 ->tile
                                 sort-by-order]]
            [forge.val-max :as val-max]
            [forge.world :refer [->v
                                 render-z-order
                                 spawn-creature]]
            [forge.world.content-grid :as content-grid]
            [forge.world.explored-tile-corners]
            [forge.world.entity-ids :as entity-ids]
            [forge.world.grid :as grid]
            [forge.world.raycaster :refer [ray-blocked?]]
            [forge.world.time :refer [timer
                                      reset-timer]]
            [forge.world.tiled-map :refer [world-tiled-map]]
            [forge.world.player :refer [player-eid]]
            [malli.core :as m]
            [reduce-fsm :as fsm])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Touchable Stage)
           (com.badlogic.gdx.scenes.scene2d.ui Cell Widget Image Label Button Table WidgetGroup Stack ButtonGroup HorizontalGroup VerticalGroup Window Tree$Node)
           (com.badlogic.gdx.scenes.scene2d.utils ChangeListener)
           (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils Align Scaling)
           (com.kotcrab.vis.ui.widget VisWindow VisTable)))

(defn k->pretty-name [k]
  (str/capitalize (name k)))

(defsystem e-tick [_ eid])
(defmethod e-tick :default [_ eid])

(defsystem render-below [_ entity])
(defmethod render-below :default [_ entity])

(defsystem render-default [_ entity])
(defmethod render-default :default [_ entity])

(defsystem render-above [_ entity])
(defmethod render-above :default [_ entity])

(defsystem render-info [_ entity])
(defmethod render-info :default [_ entity])

(defmacro with-err-str
  "Evaluates exprs in a context in which *err* is bound to a fresh
  StringWriter.  Returns the string created by any nested printing
  calls."
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*err* s#]
       ~@body
       (str s#))))

(defn ops-add    [ops other-ops] (merge-with + ops other-ops))
(defn ops-remove [ops other-ops] (merge-with - ops other-ops))

(defsystem op-apply [_ base-value])

(defmethod op-apply :op/inc [[_ value] base-value]
  (+ base-value value))

(defmethod op-apply :op/mult [[_ value] base-value]
  (* base-value (inc (/ value 100))))

(defsystem op-order)

(defmethod op-order :op/inc [_]
  0)

(defmethod op-order :op/mult [_]
  1)

(defn ops-apply [ops value]
  (reduce (fn [value op]
            (op-apply op value))
          value
          (sort-by op-order ops)))

(defsystem state-enter)
(defmethod state-enter :default [_])

(defsystem state-exit)
(defmethod state-exit :default [_])

(defsystem state-cursor)
(defmethod state-cursor :default [_])

(defsystem pause-game?)
(defmethod pause-game? :default [_])

(defsystem manual-tick)
(defmethod manual-tick :default [_])

(defsystem clicked-inventory-cell [_ cell])
(defmethod clicked-inventory-cell :default [_ cell])

(defsystem clicked-skillmenu-skill [_ skill])
(defmethod clicked-skillmenu-skill :default [_ skill])

(defsystem draw-gui-view [_])
(defmethod draw-gui-view :default [_])

(defn mods-add    [mods other-mods] (merge-with ops-add    mods other-mods))
(defn mods-remove [mods other-mods] (merge-with ops-remove mods other-mods))

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [eid]
  (try
   (doseq [k (keys @eid)]
     (try (when-let [v (k @eid)]
            (e-tick [k v] eid))
          (catch Throwable t
            (throw (ex-info "e-tick" {:k k} t)))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(defn tick-entities [entities]
  (run! tick-entity entities))

(defmethod malli-form :s/val-max [_] (m/form val-max/schema))

(defmethod malli-form :s/number  [_] number?)
(defmethod malli-form :s/nat-int [_] nat-int?)
(defmethod malli-form :s/int     [_] int?)
(defmethod malli-form :s/pos     [_] pos?)
(defmethod malli-form :s/pos-int [_] pos-int?)

(defmethod malli-form :s/sound [_] :string)

(defmethod malli-form :s/image [_]
  [:map {:closed true}
   [:file :string]
   [:sub-image-bounds {:optional true} [:vector {:size 4} nat-int?]]])

(defmethod malli-form :s/animation [_]
  [:map {:closed true}
   [:frames :some] ; FIXME actually images
   [:frame-duration pos?]
   [:looping? :boolean]])

(defn- type->id-namespace [property-type]
  (keyword (name property-type)))

(defmethod malli-form :s/one-to-one [[_ property-type]]
  [:qualified-keyword {:namespace (type->id-namespace property-type)}])

(defmethod malli-form :s/one-to-many [[_ property-type]]
  [:set [:qualified-keyword {:namespace (type->id-namespace property-type)}]])

(defn- attribute-form
  "Can define keys as just keywords or with schema-props like [:foo {:optional true}]."
  [ks]
  (for [k ks
        :let [k? (keyword? k)
              schema-props (if k? nil (k 1))
              k (if k? k (k 0))]]
    (do
     (assert (keyword? k))
     (assert (or (nil? schema-props) (map? schema-props)) (pr-str ks))
     [k schema-props (db/malli-form (db/schema-of k))])))

(defn- map-form [ks]
  (apply vector :map {:closed true} (attribute-form ks)))

(println "defmethod malli-form :s/map")
(defmethod malli-form :s/map [[_ ks]]
  (map-form ks))

(defmethod malli-form :s/map-optional [[_ ks]]
  (map-form (map (fn [k] [k {:optional true}]) ks)))

(defn- namespaced-ks [ns-name-k]
  (filter #(= (name ns-name-k) (namespace %))
          (keys db/schemas)))

(defmethod malli-form :s/components-ns [[_ ns-name-k]]
  (malli-form [:s/map-optional (namespaced-ks ns-name-k)]))

(defmethod edn->value :s/one-to-many [_ property-ids]
  (set (map db/build property-ids)))

(defmethod edn->value :s/one-to-one [_ property-id]
  (db/build property-id))

(defn property->image [{:keys [entity/image entity/animation]}]
  (or image
      (first (:frames animation))))

(require '[malli.generator :as mg])

(defn k->default-value [k]
  (let [schema (db/schema-of k)]
    (cond
     (#{:s/one-to-one :s/one-to-many} (db/schema-type schema)) nil

     ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

     :else (mg/generate (malli-form schema) {:size 3}))))

(defmethod edn->value :s/image [_ edn]
  (edn->image edn))

(defn toggle-visible! [^Actor actor]
  (.setVisible actor (not (.isVisible actor))))

(defn- set-center [^Actor actor x y]
  (.setPosition actor
                (- x (/ (.getWidth  actor) 2))
                (- y (/ (.getHeight actor) 2))))

(defn actor-hit [^Actor actor [x y]]
  (let [v (.stageToLocalCoordinates actor (Vector2. x y))]
    (.hit actor (.x v) (.y v) true)))

(defn- set-cell-opts [^Cell cell opts]
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

(defn add-rows!
  "rows is a seq of seqs of columns.
  Elements are actors or nil (for just adding empty cells ) or a map of
  {:actor :expand? :bottom?  :colspan int :pad :pad-bottom}. Only :actor is required."
  [^Table table rows]
  (doseq [row rows]
    (doseq [props-or-actor row]
      (cond
       (map? props-or-actor) (-> (.add table ^Actor (:actor props-or-actor))
                                 (set-cell-opts (dissoc props-or-actor :actor)))
       :else (.add table ^Actor props-or-actor)))
    (.row table))
  table)

(defn- set-table-opts [^Table table {:keys [rows cell-defaults]}]
  (set-cell-opts (.defaults table) cell-defaults)
  (add-rows! table rows))

(defn horizontal-separator-cell [colspan]
  {:actor (vis/separator :default)
   :pad-top 2
   :pad-bottom 2
   :colspan colspan
   :fill-x? true
   :expand-x? true})

(defn vertical-separator-cell []
  {:actor (vis/separator :vertical)
   :pad-top 2
   :pad-bottom 2
   :fill-y? true
   :expand-y? true})

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

(defn- set-actor-opts [^Actor a {:keys [id name visible? touchable center-position position] :as opts}]
  (when id                          (.setUserObject        a id))
  (when name                        (.setName      a name))
  (when (contains? opts :visible?)  (.setVisible   a (boolean visible?)))
  (when touchable                   (.setTouchable a (case touchable
                                                       :children-only Touchable/childrenOnly
                                                       :disabled      Touchable/disabled
                                                       :enabled       Touchable/enabled)))
  (when-let [[x y] center-position] (set-center    a x y))
  (when-let [[x y] position]        (.setPosition  a x y))
  a)

(defn- set-opts [actor opts]
  (set-actor-opts actor opts)
  (when (instance? Table actor)
    (set-table-opts actor opts)) ; before widget-group-opts so pack is packing rows
  (when (instance? WidgetGroup actor)
    (set-widget-group-opts actor opts))
  actor)

(defmacro ^:private proxy-ILookup
  "For actors inheriting from Group."
  [class args]
  `(proxy [~class clojure.lang.ILookup] ~args
     (valAt
       ([id#]
        (find-actor-with-id ~'this id#))
       ([id# not-found#]
        (or (find-actor-with-id ~'this id#) not-found#)))))

(defn ui-group [{:keys [actors] :as opts}]
  (let [group (proxy-ILookup com.badlogic.gdx.scenes.scene2d.Group [])]
    (run! #(add-actor! group %) actors)
    (set-opts group opts)))

(defn horizontal-group ^HorizontalGroup [{:keys [space pad]}]
  (let [group (proxy-ILookup HorizontalGroup [])]
    (when space (.space group (float space)))
    (when pad   (.pad   group (float pad)))
    group))

(defn vertical-group [actors]
  (let [group (proxy-ILookup VerticalGroup [])]
    (run! #(add-actor! group %) actors)
    group))

(defn add-tooltip!
  "tooltip-text is a (fn []) or a string. If it is a function will be-recalculated every show.
  Returns the actor."
  [actor tooltip-text]
  (vis/add-tooltip! actor tooltip-text))

(defn remove-tooltip! [actor]
  (vis/remove-tooltip! actor))

(defn button-group [{:keys [max-check-count min-check-count]}]
  (let [bg (ButtonGroup.)]
    (.setMaxCheckCount bg max-check-count)
    (.setMinCheckCount bg min-check-count)
    bg))

(defn check-box
  "on-clicked is a fn of one arg, taking the current isChecked state"
  [text on-clicked checked?]
  (let [^Button button (vis/check-box text)]
    (.setChecked button checked?)
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event ^Button actor]
                      (on-clicked (.isChecked actor)))))
    button))

(defn select-box [{:keys [items selected] :as opts}]
  (vis/select-box opts))

(defn ui-table ^Table [opts]
  (-> (proxy-ILookup VisTable [])
      (set-opts opts)))

(defn ui-window ^VisWindow [{:keys [title modal? close-button? center? close-on-escape?] :as opts}]
  (-> (let [window (doto (proxy-ILookup VisWindow [^String title true]) ; true = showWindowBorder
                     (.setModal (boolean modal?)))]
        (when close-button?    (.addCloseButton window))
        (when center?          (.centerWindow   window))
        (when close-on-escape? (.closeOnEscape  window))
        window)
      (set-opts opts)))

(defn label [text]
  (vis/label text))

(defn text-field [text opts]
  (-> (vis/text-field text)
      (set-opts opts)))

(defn ui-stack ^Stack [actors]
  (proxy-ILookup Stack [(into-array Actor actors)]))

(defn image-widget ; TODO widget also make, for fill parent
  "Takes either a texture-region or drawable. Opts are :scaling, :align and actor opts."
  [object {:keys [scaling align fill-parent?] :as opts}]
  (-> (let [^Image image (vis/image object)]
        (when (= :center align)
          (.setAlign image Align/center))
        (when (= :fill scaling)
          (.setScaling image Scaling/fill))
        (when fill-parent?
          (.setFillParent image true))
        image)
      (set-opts opts)))

(defn image->widget
  "Same opts as [[image-widget]]."
  [image opts]
  (image-widget (:texture-region image) opts))

(def texture-region-drawable scene2d.utils/texture-region-drawable)

(defn scroll-pane [actor]
  (let [scroll-pane (vis/scroll-pane actor)]
    (Actor/.setUserObject scroll-pane :scroll-pane)
    (.setFlickScroll scroll-pane false)
    (.setFadeScrollBars scroll-pane false)
    scroll-pane))

(defn- button-class? [actor]
  (some #(= Button %) (supers (class actor))))

(defn button?
  "Returns true if the actor or its parent is a button."
  [^Actor actor]
  (or (button-class? actor)
      (and (.getParent actor)
           (button-class? (.getParent actor)))))

(defn window-title-bar?
  "Returns true if the actor is a window title bar."
  [^Actor actor]
  (when (instance? Label actor)
    (when-let [p (.getParent actor)]
      (when-let [p (.getParent p)]
        (and (vis/window? p)
             (= (.getTitleLabel ^Window p) actor))))))

(defn find-ancestor-window ^Window [^Actor actor]
  (if-let [p (.getParent actor)]
    (if (instance? Window p)
      p
      (find-ancestor-window p))
    (throw (Error. (str "Actor has no parent window " actor)))))

(defn pack-ancestor-window! [^Actor actor]
  (.pack (find-ancestor-window actor)))

(declare ^:dynamic *on-clicked-actor*)

(defn change-listener ^ChangeListener [on-clicked]
  (proxy [ChangeListener] []
    (changed [event actor]
      (binding [*on-clicked-actor* actor]
        (on-clicked)))))

(defn text-button [text on-clicked]
  (let [button (vis/text-button text)]
    (.addListener button (change-listener on-clicked))
    button))

(defn image-button
  ([image on-clicked]
   (image-button image on-clicked {}))
  ([{:keys [texture-region]} on-clicked {:keys [scale]}]
   (let [drawable (texture-region-drawable texture-region)
         button (vis/image-button drawable)]
     (when scale
       (let [[w h] [(g/region-width  texture-region)
                    (g/region-height texture-region)]]
         (scene2d.utils/set-min-size! drawable
                                      (* scale w)
                                      (* scale h))))
     (.addListener button (change-listener on-clicked))
     button)))

(defn ui-actor ^Actor [{:keys [draw act]}]
  (proxy [Actor] []
    (draw [_batch _parent-alpha]
      (when draw (draw)))
    (act [_delta]
      (when act (act)))))

(defn ui-widget [draw!]
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (draw! this))))

(defn set-drawable! [^Image image drawable]
  (.setDrawable image drawable))

(defn t-node ^Tree$Node [actor]
  (proxy [Tree$Node] [actor]))

(defn background-image []
  (image->widget (->image "images/moon_background.png")
                 {:fill-parent? true
                  :scaling :fill
                  :align :center}))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal [{:keys [title text button-text on-click]}]
  (assert (not (::modal (screen-stage))))
  (add-actor
   (ui-window {:title title
               :rows [[(label text)]
                      [(text-button button-text
                                    (fn []
                                      (Actor/.remove (::modal (screen-stage)))
                                      (on-click)))]]
               :id ::modal
               :modal? true
               :center-position [(/ gui-viewport-width 2)
                                 (* gui-viewport-height (/ 3 4))]
               :pack? true})))

(defn error-window! [throwable]
  (pretty-pst throwable)
  (add-actor
   (ui-window {:title "Error"
               :rows [[(label (binding [*print-level* 3]
                                (with-err-str
                                  (clojure.repl/pst throwable))))]]
               :modal? true
               :close-button? true
               :close-on-escape? true
               :center? true
               :pack? true})))

(def ^:private player-message-duration-seconds 1.5)

(def ^:private message-to-player nil)

(defn- draw-player-message []
  (when-let [{:keys [message]} message-to-player]
    (draw-text {:x (/ gui-viewport-width 2)
                :y (+ (/ gui-viewport-height 2) 200)
                :text message
                :scale 2.5
                :up? true})))

(defn- check-remove-message []
  (when-let [{:keys [counter]} message-to-player]
    (alter-var-root #'message-to-player update :counter + (delta-time))
    (when (>= counter player-message-duration-seconds)
      (bind-root message-to-player nil))))

(defn player-message-actor []
  (ui-actor {:draw draw-player-message
             :act check-remove-message}))

(defn player-message-show [message]
  (bind-root message-to-player {:message message :counter 0}))

(defn cells-and-items [inventory slot]
  (for [[position item] (slot inventory)]
    [[slot position] item]))

(defn valid-slot? [[slot _] item]
  (or (= :inventory.slot/bag slot)
      (= (:item/slot item) slot)))

(defn stackable? [item-a item-b]
  (and (:count item-a)
       (:count item-b) ; this is not required but can be asserted, all of one name should have count if others have count
       (= (:property/id item-a) (:property/id item-b))))

(defmulti generate-level* (fn [world] (:world/generator world)))

(defn generate-level [world-props]
  (assoc (generate-level* world-props)
         :world/player-creature
         (:world/player-creature world-props)))

(def mouseover-eid nil)

(defn mouseover-entity []
  (and mouseover-eid
       @mouseover-eid))

(defn world-clear [] ; responsibility of screen? we are not creating the tiled-map here ...
  (when (bound? #'world-tiled-map)
    (dispose world-tiled-map)))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    (sd/rectangle x y (:width entity) (:height entity) color)))

(defn- render-entity! [system entity]
  (try
   (when show-body-bounds
     (draw-body-rect entity (if (:collides? entity) color/white :gray)))
   (run! #(system % entity) entity)
   (catch Throwable t
     (draw-body-rect entity :red)
     (pretty-pst t))))

(def ^:private ^:dbg-flag spawn-enemies? true)

(defn- spawn-enemies [tiled-map]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (spawn-creature (update props :position tile->middle))))

(defn world-init [{:keys [tiled-map start-position]}]
  (forge.world.tiled-map/init             tiled-map)
  (forge.world.explored-tile-corners/init tiled-map)
  (forge.world.grid/init                  tiled-map)
  (forge.world.entity-ids/init            tiled-map)
  (forge.world.content-grid/init          tiled-map)
  (forge.world.raycaster/init             tiled-map)
  (forge.world.time/init                  tiled-map)
  (forge.world.player/init           start-position)
  (when spawn-enemies?
    (spawn-enemies tiled-map)))

(defn active-entities []
  (content-grid/active-entities @player-eid))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [entity]
  (let [[x y] (:position entity)
        x (float x)
        y (float y)
        [cx cy] (cam/position (world-camera))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float world-viewport-width)  2)))
     (<= ydist (inc (/ (float world-viewport-height) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn line-of-sight? [source target]
  (and (or (not (:entity/player? source))
           (on-screen? target))
       (not (and los-checks?
                 (ray-blocked? (:position source) (:position target))))))

(defn render-entities
  "Draws entities in the correct z-order and in the order of render-systems for each z-order."
  [entities]
  (let [player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              render-z-order)
            system [render-below
                    render-default
                    render-above
                    render-info]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? player entity))]
      (render-entity! system entity))))

(defn e-direction [entity other-entity]
  (v/direction (:position entity) (:position other-entity)))

(defn e-collides? [entity other-entity]
  (shape/overlaps? entity other-entity))

(defn e-tile [entity]
  (->tile (:position entity)))

(defn e-enemy [{:keys [entity/faction]}]
  (case faction
    :evil :good
    :good :evil))

(defn e-state-k [entity]
  (-> entity :entity/fsm :state))

(defn e-state-obj [entity]
  (let [k (e-state-k entity)]
    [k (k entity)]))

(defn- send-event! [eid event params]
  (when-let [fsm (:entity/fsm @eid)]
    (let [old-state-k (:state fsm)
          new-fsm (fsm/fsm-event fsm event)
          new-state-k (:state new-fsm)]
      (when-not (= old-state-k new-state-k)
        (let [old-state-obj (e-state-obj @eid)
              new-state-obj [new-state-k (->v (if params
                                                [new-state-k eid params]
                                                [new-state-k eid]))]]
          (when (:entity/player? @eid)
            (when-let [cursor (state-cursor new-state-obj)]
              (set-cursor cursor)))
          (swap! eid #(-> %
                          (assoc :entity/fsm new-fsm
                                 new-state-k (new-state-obj 1))
                          (dissoc old-state-k)))
          (state-exit old-state-obj)
          (state-enter new-state-obj))))))

(defn send-event
  ([eid event]
   (send-event! eid event nil))
  ([eid event params]
   (send-event! eid event params)))

(defn add-mods    [entity mods] (update entity :entity/modifiers mods-add    mods))
(defn remove-mods [entity mods] (update entity :entity/modifiers mods-remove mods))

(defn mod-value [base-value {:keys [entity/modifiers]} modifier-k]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (ops-apply (modifier-k modifiers)
             base-value))

(defn e-stat [entity k]
  (when-let [base-value (k entity)]
    (mod-value base-value
               entity
               (keyword "modifier" (name k)))))

(defn- ->pos-int [val-max]
  (mapv #(-> % int (max 0)) val-max))

(defn- apply-max-modifier [val-max entity modifier-k]
  {:pre  [(m/validate val-max/schema val-max)]
   :post [(m/validate val-max/schema val-max)]}
  (let [val-max (update val-max 1 mod-value entity modifier-k)
        [v mx] (->pos-int val-max)]
    [(min v mx) mx]))

(defn- apply-min-modifier [val-max entity modifier-k]
  {:pre  [(m/validate val-max/schema val-max)]
   :post [(m/validate val-max/schema val-max)]}
  (let [val-max (update val-max 0 mod-value entity modifier-k)
        [v mx] (->pos-int val-max)]
    [v (max v mx)]))

(defn hitpoints
  "Returns the hitpoints val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-hp is capped by max-hp."
  [entity]
  (-> entity
      :entity/hp
      (apply-max-modifier entity :modifier/hp-max)))

(defn e-mana
  "Returns the mana val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-mana is capped by max-mana."
  [entity]
  (-> entity
      :entity/mana
      (apply-max-modifier entity :modifier/mana-max)))

(defn pay-mana-cost [entity cost]
  (let [mana-val ((e-mana entity) 0)]
    (assert (<= cost mana-val))
    (assoc-in entity [:entity/mana 0] (- mana-val cost))))

(defn add-text-effect [entity text]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter reset-timer))
           {:text text
            :counter (timer 0.4)})))

(defn- mana-value [entity]
  (if (:entity/mana entity)
    ((e-mana entity) 0)
    0))

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (mana-value entity))))

(defn skill-usable-state
  [entity
   {:keys [skill/cooling-down? skill/effects] :as skill}
   effect-ctx]
  (cond
   cooling-down?
   :cooldown

   (not-enough-mana? entity skill)
   :not-enough-mana

   (not (effects-applicable? effect-ctx effects))
   :invalid-params

   :else
   :usable))

(defn has-skill? [{:keys [entity/skills]} {:keys [property/id]}]
  (contains? skills id))

(defn damage-mods
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (apply-min-modifier source :modifier/damage-deal-min)
                (apply-max-modifier source :modifier/damage-deal-max))))

  ([source target damage]
   (update (damage-mods source damage)
           :damage/min-max
           apply-max-modifier
           target
           :modifier/damage-receive-max)))

(defn- action-bar-button-group []
  (let [actor (ui-actor {})]
    (.setName actor "action-bar/button-group")
    (Actor/.setUserObject actor (button-group {:max-check-count 1 :min-check-count 0}))
    actor))

(defn- group->button-group [group]
  (user-object (find-actor group "action-bar/button-group")))

(defn- get-action-bar []
  (let [group (::action-bar (:action-bar-table (screen-stage)))]
    {:horizontal-group group
     :button-group (group->button-group group)}))

(defn actionbar-add-skill [{:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar)
        button (image-button image (fn []) {:scale 2})]
    (Actor/.setUserObject button id)
    (add-tooltip! button #(info-text skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (add-actor! horizontal-group button)
    (ButtonGroup/.add button-group button)
    nil))

(defn actionbar-remove-skill [{:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar)
        ^Button button (get horizontal-group id)]
    (.remove button)
    (ButtonGroup/.remove button-group button)
    nil))

(defn actionbar-create []
  (let [group (horizontal-group {:pad 2 :space 2})]
    (.setUserObject group ::action-bar)
    (add-actor! group (action-bar-button-group))
    group))

(defn actionbar-selected-skill []
  (when-let [skill-button (ButtonGroup/.getChecked (:button-group (get-action-bar)))]
    (user-object skill-button)))

(defn add-skill [entity {:keys [property/id] :as skill}]
  {:pre [(not (has-skill? entity skill))]}
  (when (:entity/player? entity)
    (actionbar-add-skill skill))
  (assoc-in entity [:entity/skills id] skill))

(defn remove-skill [entity {:keys [property/id] :as skill}]
  {:pre [(has-skill? entity skill)]}
  (when (:entity/player? entity)
    (actionbar-remove-skill skill))
  (update entity :entity/skills dissoc id))
