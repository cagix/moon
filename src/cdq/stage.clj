(ns cdq.stage
  (:require [cdq.ctx :as ctx]
            [cdq.data.val-max :as val-max]
            [cdq.entity :as entity]
            [cdq.entity.inventory :as inventory]
            [cdq.entity.state :as state]
            [cdq.graphics :as graphics]
            [clojure.gdx.graphics.color :as color]
            [cdq.info :as info]
            [clojure.data.grid2d :as g2d]
            [clojure.gdx :as gdx]
            [clojure.gdx.input :as input]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :as group]
            [clojure.gdx.scene2d.ui :as ui]
            [clojure.gdx.scene2d.ui.menu :as ui.menu]
            [clojure.gdx.graphics :as gdx.graphics]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.string :as str]
            [clojure.utils :as utils])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.scenes.scene2d Stage)
           (com.badlogic.gdx.scenes.scene2d.ui Button ButtonGroup Image Widget)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable TextureRegionDrawable ClickListener)))

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
      (let [g ctx/graphics]
        (draw-cell-rect! g
                         @ctx/player-eid
                         (actor/x this)
                         (actor/y this)
                         (actor/hit this (graphics/mouse-position g))
                         (actor/user-object (actor/parent this)))))))

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
                               (actor/set-user-object! ::horizontal-group)
                               (group/add-actor! (doto (actor/create {})
                                                   (actor/set-name! "button-group")
                                                   (actor/set-user-object! (ui/button-group {:max-check-count 1
                                                                                             :min-check-count 0})))))
                      :expand? true
                      :bottom? true}]]
             :id ::action-bar-table
             :cell-defaults {:pad 2}
             :fill-parent? true}))

(defn- action-bar-data [stage]
  (let [group (::horizontal-group (::action-bar-table stage))]
    {:horizontal-group group
     :button-group (actor/user-object (group/find-actor group "button-group"))}))

(defn selected-skill [stage]
  (when-let [skill-button (ButtonGroup/.getChecked (:button-group (action-bar-data stage)))]
    (actor/user-object skill-button)))

(defn add-skill! [stage {:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (action-bar-data stage)
        button (ui/image-button image (fn []) {:scale 2})]
    (actor/set-user-object! button id)
    (ui/add-tooltip! button #(info/text skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (group/add-actor! horizontal-group button)
    (ButtonGroup/.add button-group ^Button button)
    nil))

(defn remove-skill! [stage {:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (action-bar-data stage)
        button (get horizontal-group id)]
    (actor/remove! button)
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
    (.addActor window (actor/create {:act (fn [_this]
                                            (.setText label (str (->label-text)))
                                            (.pack window))}))
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
    (actor/create {:draw (fn [_this]
                           (let [player-entity @ctx/player-eid
                                 x (- x (/ rahmenw 2))
                                 g ctx/graphics]
                             (render-hpmana-bar g x y-hp   hpcontent   (entity/hitpoints player-entity) "HP")
                             (render-hpmana-bar g x y-mana manacontent (entity/mana      player-entity) "MP")))})))

(defn root [^Stage stage]
  (Stage/.getRoot stage))

(defn add-actor! [^Stage stage actor]
  (.addActor stage actor))

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
                                  ((requiring-resolve 'cdq.editor/open-main-window!) property-type))})}]
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
  (actor/create {:draw (fn [_this] (state/draw-gui-view (entity/state-obj @ctx/player-eid)))}))

(defn- player-message []
  (doto (actor/create {:draw (fn [this]
                               (let [g ctx/graphics
                                     state (actor/user-object this)]
                                 (when-let [text (:text @state)]
                                   (graphics/draw-text g {:x (/ (:width     (:ui-viewport g)) 2)
                                                          :y (+ (/ (:height (:ui-viewport g)) 2) 200)
                                                          :text text
                                                          :scale 2.5
                                                          :up? true}))))
                       :act (fn [this]
                              (let [state (actor/user-object this)]
                                (when (:text @state)
                                  (swap! state update :counter + (gdx.graphics/delta-time gdx/graphics))
                                  (when (>= (:counter @state) 1.5)
                                    (reset! state nil)))))})
    (actor/set-user-object! (atom nil))
    (actor/set-name! "player-message-actor")))

(defn show-message! [stage text]
  (actor/set-user-object! (group/find-actor (root stage) "player-message-actor")
                          (atom {:text text
                                 :counter 0})))

(defn- create-actors []
  [(ui.menu/create (dev-menu-config))
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
                   (group/find-actor-with-id (root this) id))
                  ([id not-found]
                   (or (group/find-actor-with-id (root this) id)
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
                                                    (actor/remove! (::modal stage))
                                                    (on-click)))]]
                          :id ::modal
                          :modal? true
                          :center-position [(/ (:width  (:ui-viewport ctx/graphics)) 2)
                                            (* (:height (:ui-viewport ctx/graphics)) (/ 3 4))]
                          :pack? true})))

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

(defn check-window-controls! [stage]
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (input/key-just-pressed? gdx/input (get window-hotkeys window-id))]
      (actor/toggle-visible! (get (:windows stage) window-id))))
  (when (input/key-just-pressed? gdx/input :escape)
    (let [windows (group/children (:windows stage))]
      (when (some actor/visible? windows)
        (run! #(actor/set-visible! % false) windows)))))

(defn inventory-visible? [stage]
  (-> stage :windows :inventory-window actor/visible?))

(defn toggle-inventory-visible! [stage]
  (-> stage :windows :inventory-window actor/toggle-visible!))
