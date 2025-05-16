(ns cdq.impl.stage
  (:require [cdq.ctx :as ctx]
            [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.graphics :as graphics]
            [cdq.grid2d :as g2d]
            [cdq.info :as info]
            [cdq.inventory :as inventory]
            [cdq.stage :refer [add-actor!] :as stage]
            [cdq.state :as state]
            [cdq.ui]
            [cdq.utils :as utils]
            [cdq.val-max :as val-max]
            [clojure.string :as str]
            [gdl.graphics]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.viewport :as viewport]
            [gdl.input :as input]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor
                                            Group
                                            Stage
                                            Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Table
                                               Image
                                               Label
                                               Button
                                               ButtonGroup
                                               Widget
                                               Window)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable
                                                  TextureRegionDrawable
                                                  ClickListener
                                                  Drawable)
           (com.badlogic.gdx.math Vector2)
           (com.kotcrab.vis.ui.widget Menu MenuBar MenuItem PopupMenu VisWindow)))

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

(defn- draw-cell-rect! [player-entity x y mouseover? cell]
  (draw/rectangle x y cell-size cell-size :gray)
  (when (and mouseover?
             (= :player-item-on-cursor (entity/state-k player-entity)))
    (let [item (:entity/item-on-cursor player-entity)
          color (if (inventory/valid-slot? cell item)
                  droppable-color
                  not-allowed-color)]
      (draw/filled-rectangle (inc x) (inc y) (- cell-size 2) (- cell-size 2) color))))

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-rect-actor []
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (let [^Actor actor this]
        (draw-cell-rect! @ctx/player-eid
                         (.getX actor)
                         (.getY actor)
                         (let [[x y] (viewport/mouse-position ctx/ui-viewport)
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
  (graphics/from-sheet (graphics/sprite-sheet (ctx/assets "images/items.png") 48 48)
                       (slot->sprite-idx slot)))

(defn- slot->background [slot]
  (let [drawable (TextureRegionDrawable. ^TextureRegion (:texture-region (slot->sprite slot)))]
    (BaseDrawable/.setMinSize drawable (float cell-size) (float cell-size))
    (TextureRegionDrawable/.tint drawable (gdl.graphics/color 1 1 1 0.4))))

(defn- ->cell [slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]]
    (doto (ui/stack [(draw-rect-actor)
                     (ui/image-widget (slot->background slot) {:id :image})])
      (.setName "inventory-cell")
      (.setUserObject cell)
      (.addListener (proxy [ClickListener] []
                      (clicked [_event _x _y]
                        (-> @ctx/player-eid
                            entity/state-obj
                            (state/clicked-inventory-cell cell)
                            utils/handle-txs!)))))))

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
        drawable (TextureRegionDrawable. ^TextureRegion (:texture-region (:entity/image item)))]
    (BaseDrawable/.setMinSize drawable (float cell-size) (float cell-size))
    (Image/.setDrawable image-widget drawable)
    (actor/add-tooltip! cell-widget #(info/text item))))

(defn remove-item! [stage cell]
  (let [cell-widget (get-cell-widget stage cell)
        image-widget (get cell-widget :image)]
    (Image/.setDrawable image-widget (slot->background (cell 0)))
    (actor/remove-tooltip! cell-widget)))

(defn- button-group [{:keys [max-check-count min-check-count]}]
  (doto (ButtonGroup.)
    (.setMaxCheckCount max-check-count)
    (.setMinCheckCount min-check-count)))

(defn- action-bar []
  (ui/table {:rows [[{:actor (doto (ui/horizontal-group {:pad 2 :space 2})
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
        button (ui/image-button image (fn []) {:scale 2})]
    (Actor/.setUserObject button id)
    (actor/add-tooltip! button #(info/text skill)) ; (assoc ctx :effect/source (world/player)) FIXME
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

(defn- render-infostr-on-bar [infostr x y h]
  (draw/text {:text infostr
              :x (+ x 75)
              :y (+ y 2)
              :up? true}))

(defn- hp-mana-bar [[x y-mana]]
  (let [rahmen      (graphics/sprite (ctx/assets "images/rahmen.png"))
        hpcontent   (graphics/sprite (ctx/assets "images/hp.png"))
        manacontent (graphics/sprite (ctx/assets "images/mana.png"))
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [x y contentimage minmaxval name]
                            (draw/image rahmen [x y])
                            (draw/image (graphics/sub-sprite contentimage
                                                             [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                        [x y])
                            (render-infostr-on-bar (str (utils/readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (proxy [Actor] []
      (draw [_batch _parent-alpha]
        (let [player-entity @ctx/player-eid
              x (- x (/ rahmenw 2))]
          (render-hpmana-bar x y-hp   hpcontent   (entity/hitpoints player-entity) "HP")
          (render-hpmana-bar x y-mana manacontent (entity/mana      player-entity) "MP"))))))

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
                      :on-click (fn [] ((requiring-resolve 'cdq.game.reset/do!) world-fn))})}
           {:label "Help"
            :items [{:label "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"}]}
           {:label "Objects"
            :items (for [property-type (sort (filter #(= "properties" (namespace %)) (keys ctx/schemas)))]
                     {:label (str/capitalize (name property-type))
                      :on-click (fn []
                                  ((requiring-resolve 'cdq.editor/open-editor-window!) property-type))})}]
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
                    :update-fn (fn [] (mapv int (viewport/mouse-position ctx/ui-viewport)))}
                   {:label "World"
                    :update-fn (fn [] (mapv int (viewport/mouse-position ctx/world-viewport)))}
                   {:label "Zoom"
                    :update-fn (fn [] (camera/zoom (:camera ctx/world-viewport)))
                    :icon (ctx/assets "images/zoom.png")}
                   {:label "FPS"
                    :update-fn (fn [] (gdl.graphics/frames-per-second))
                    :icon (ctx/assets "images/fps.png")}]})

(defn- player-state-actor []
  (proxy [Actor] []
    (draw [_batch _parent-alpha]
      (state/draw-gui-view (entity/state-obj @ctx/player-eid)))))

(defn- player-message []
  (doto (proxy [Actor] []
          (draw [_batch _parent-alpha]
            (let [state (Actor/.getUserObject this)]
              (when-let [text (:text @state)]
                (draw/text {:x (/ (:width     ctx/ui-viewport) 2)
                            :y (+ (/ (:height ctx/ui-viewport) 2) 200)
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
  (Actor/.setUserObject (Group/.findActor (stage/root stage) "player-message-actor")
                        (atom {:text text
                               :counter 0})))

(defn- check-escape-close-windows [windows]
  (when (input/key-just-pressed? :escape)
    (run! #(Actor/.setVisible % false) (Group/.getChildren windows))))

(def window-hotkeys {:inventory-window  :i
                     :entity-info-window :e})

(defn- check-window-hotkeys [windows]
  (doseq [[id input-key] window-hotkeys
          :when (input/key-just-pressed? input-key)]
    (actor/toggle-visible! (get windows id))))

(defn- create-actors []
  [(create-menu (dev-menu-config))
   (action-bar)
   (hp-mana-bar [(/ (:width ctx/ui-viewport) 2)
                 80 ; action-bar-icon-size
                 ])
   (ui/group {:id :windows
              :actors [(proxy [Actor] []
                         (act [_delta]
                           (check-window-hotkeys       (Actor/.getParent this))
                           (check-escape-close-windows (Actor/.getParent this))))
                       (entity-info-window [(:width ctx/ui-viewport) 0])
                       (inventory-window [(:width  ctx/ui-viewport)
                                          (:height ctx/ui-viewport)])]})
   (player-state-actor)
   (player-message)])

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
                          :center-position [(/ (:width  ctx/ui-viewport) 2)
                                            (* (:height ctx/ui-viewport) (/ 3 4))]
                          :pack? true})))

(defn inventory-visible? [stage]
  (-> stage :windows :inventory-window Actor/.isVisible))

(defn inventory-cell-with-item? [actor]
  {:pre [actor]}
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

(defn create! []
  (ui/load! ctx/ui-config)
  (let [stage (Stage. (:java-object ctx/ui-viewport)
                      (:java-object ctx/batch))]
    (run! #(.addActor stage %) (create-actors))
    (input/set-processor! stage)
    (reify
      ILookup
      (valAt [_ id]
        (ui/find-actor-with-id (.getRoot stage) id))

      (valAt [_ id not-found]
        (or (ui/find-actor-with-id (.getRoot stage) id)
            not-found))

      cdq.stage/Stage
      (add-actor! [_ actor]
        (.addActor stage actor))

      (draw! [_]
        (.draw stage))

      (act! [_]
        (.act stage))

      (mouse-on-actor? [_]
        (let [[x y] (viewport/mouse-position ctx/ui-viewport)]
          (.hit stage x y true)))

      (root [_]
        (.getRoot stage))

  (set-item! [stage cell item]
    (set-item! stage cell item))

  (remove-item! [stage cell]
    (remove-item! stage cell))

  (selected-skill [stage]
    (selected-skill stage))

  (add-skill! [stage skill]
    (add-skill! stage skill))

  (remove-skill! [stage skill]
    (remove-skill! stage skill))

  (show-message! [stage text]
    (show-message! stage text))

  (show-modal! [stage opts]
    (show-modal! stage opts))

  (inventory-visible? [stage]
    (inventory-visible? stage))

  (inventory-cell-with-item? [_ actor]
    (inventory-cell-with-item? actor))

  (window-title-bar? [_ actor]
    (window-title-bar? actor))

  (button? [_ actor]
    (button? actor)))))
