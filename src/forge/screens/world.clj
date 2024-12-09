(ns forge.screens.world
  (:require [anvil.app :refer [change-screen]]
            [anvil.graphics :as g :refer [set-cursor draw-on-world-view draw-image draw-text
                                          sub-image ->image draw-tiled-map gui-viewport-width
                                          gui-mouse-position world-mouse-position world-camera
                                          world-viewport-width world-viewport-height]]
            [anvil.screen :refer [Screen]]
            [anvil.ui :refer [ui-actor change-listener image->widget] :as ui]
            [clojure.gdx.graphics :refer [frames-per-second clear-screen]]
            [clojure.gdx.graphics.camera :as cam]
            [clojure.gdx.graphics.color :as color :refer [->color]]
            [clojure.gdx.math.shapes :refer [circle->outer-rectangle]]
            [clojure.gdx.scene2d.actor :refer [visible?  set-visible] :as actor]
            [clojure.gdx.scene2d.group :refer [add-actor! children]]
            [clojure.gdx.tiled :as tiled]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [clojure.vis-ui :as vis]
            [clojure.utils :refer [bind-root
                                   ->tile
                                   tile->middle
                                   sort-by-order
                                   readable-number
                                   dev-mode?
                                   pretty-pst]]
            [forge.app.db :as db]
            [forge.component :refer [info-text]]
            [forge.controls :as controls]
            [forge.entity :as component]
            [forge.entity.hp :refer [hitpoints]]
            [forge.entity.fsm :refer [e-state-obj]]
            [forge.entity.mana :refer [e-mana]]
            [forge.entity.state :refer [manual-tick pause-game? draw-gui-view]]
            [forge.level :refer [generate-level]]
            [forge.screens.stage :as stage :refer [screen-stage
                                                   reset-stage]]
            [forge.ui :refer [error-window!]]
            [forge.ui.action-bar :as action-bar]
            [forge.ui.inventory :as inventory]
            [forge.ui.player-message :as player-message]
            [forge.val-max :as val-max]
            [forge.world :refer [render-z-order
                                 remove-destroyed
                                 spawn-creature
                                 active-entities
                                 line-of-sight?]]
            [forge.world.content-grid]
            [forge.world.entity-ids]
            [forge.world.explored-tile-corners :refer [explored-tile-corners]]
            [forge.world.grid :refer [world-grid
                                      circle->cells]]
            [forge.world.mouseover-entity :refer [mouseover-entity]]
            [forge.world.raycaster :refer [ray-blocked?]]
            [forge.world.tiled-map :refer [world-tiled-map]]
            [forge.world.time :refer [elapsed-time]]
            [forge.world.player :refer [player-eid]]
            [forge.world.potential-fields :refer [update-potential-fields! factions-iterations]])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Table)))

(declare tick-error
         paused?)

(defn- render-infostr-on-bar [infostr x y h]
  (draw-text {:text infostr
              :x (+ x 75)
              :y (+ y 2)
              :up? true}))

(defn- hp-mana-bar []
  (let [rahmen      (->image "images/rahmen.png")
        hpcontent   (->image "images/hp.png")
        manacontent (->image "images/mana.png")
        x (/ gui-viewport-width 2)
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-mana 80 ; action-bar-icon-size
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [x y contentimage minmaxval name]
                            (draw-image rahmen [x y])
                            (draw-image (sub-image contentimage [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                        [x y])
                            (render-infostr-on-bar (str (readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui-actor {:draw (fn []
                       (let [player-entity @player-eid
                             x (- x (/ rahmenw 2))]
                         (render-hpmana-bar x y-hp   hpcontent   (hitpoints   player-entity) "HP")
                         (render-hpmana-bar x y-mana manacontent (e-mana player-entity) "MP")))})))

(defn- menu-item [text on-clicked]
  (doto (vis/menu-item text)
    (.addListener (change-listener on-clicked))))

(defn- add-upd-label
  ([table text-fn icon]
   (let [icon (image->widget (->image icon) {})
         label (vis/label "")
         sub-table (ui/table {:rows [[icon label]]})]
     (add-actor! table (ui-actor {:act #(.setText label (str (text-fn)))}))
     (.expandX (.right (Table/.add table sub-table)))))
  ([table text-fn]
   (let [label (vis/label "")]
     (add-actor! table (ui-actor {:act #(.setText label (str (text-fn)))}))
     (.expandX (.right (Table/.add table label))))))

(defn- add-update-labels [menu-bar update-labels]
  (let [table (vis/menu-bar->table menu-bar)]
    (doseq [{:keys [label update-fn icon]} update-labels]
      (let [update-fn #(str label ": " (update-fn))]
        (if icon
          (add-upd-label table update-fn icon)
          (add-upd-label table update-fn))))))

(defn- add-menu [menu-bar {:keys [label items]}]
  (let [app-menu (vis/menu label)]
    (doseq [{:keys [label on-click]} items]
      (.addItem app-menu (menu-item label (or on-click (fn [])))))
    (vis/add-menu menu-bar app-menu)))

(defn- create-menu-bar [menus]
  (let [menu-bar (vis/menu-bar)]
    (run! #(add-menu menu-bar %) menus)
    menu-bar))

(defn- dev-menu* [{:keys [menus update-labels]}]
  (let [menu-bar (create-menu-bar menus)]
    (add-update-labels menu-bar update-labels)
    menu-bar))

(def ^:private disallowed-keys [:entity/skills
                                #_:entity/fsm
                                :entity/faction
                                :active-skill])

(defn- entity-info-window []
  (let [label (ui/label "")
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position [gui-viewport-width 0]
                           :rows [[{:actor label :expand? true}]]})]
    ; TODO do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (add-actor! window (ui-actor {:act (fn update-label-text []
                                         ; items then have 2x pretty-name
                                         #_(.setText (.getTitleLabel window)
                                                     (if-let [entity (mouseover-entity)]
                                                       (info-text [:property/pretty-name (:property/pretty-name entity)])
                                                       "Entity Info"))
                                         (.setText label
                                                   (str (when-let [entity (mouseover-entity)]
                                                          (info-text
                                                           ; don't use select-keys as it loses Entity record type
                                                           (apply dissoc entity disallowed-keys)))))
                                         (.pack window))}))
    window))

(defn- geom-test []
  (let [position (world-mouse-position)
        radius 0.8
        circle {:position position :radius radius}]
    (g/circle position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (circle->cells circle))]
      (g/rectangle x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (g/rectangle x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(defn- tile-debug []
  (let [cam (world-camera)
        [left-x right-x bottom-y top-y] (cam/frustum cam)]

    (when tile-grid?
      (g/grid (int left-x) (int bottom-y)
              (inc (int world-viewport-width))
              (+ 2 (int world-viewport-height))
              1 1 [1 1 1 0.8]))

    (doseq [[x y] (cam/visible-tiles cam)
            :let [cell (world-grid [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (g/filled-rectangle x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (g/filled-rectangle x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (g/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile []
  (when highlight-blocked-cell?
    (let [[x y] (->tile (world-mouse-position))
          cell (get world-grid [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (g/rectangle x y 1 1
                      (case (:movement @cell)
                        :air  [1 1 0 0.5]
                        :none [1 0 0 0.5]))))))

(defn- debug-render-before-entities []
  (tile-debug))

(defn- debug-render-after-entities []
  #_(geom-test)
  (highlight-mouseover-tile))

(def ^:private explored-tile-color (->color 0.5 0.5 0.5 1))

(def ^:private ^:dbg-flag see-all-tiles? false)

(comment
 (def ^:private count-rays? false)

 (def ray-positions (atom []))
 (def do-once (atom true))

 (count @ray-positions)
 2256
 (count (distinct @ray-positions))
 608
 (* 608 4)
 2432
 )

(defn- tile-color-setter* [light-cache light-position]
  #_(reset! do-once false)
  (fn tile-color-setter [_color x y]
    (let [position [(int x) (int y)]
          explored? (get @explored-tile-corners position) ; TODO needs int call ?
          base-color (if explored? explored-tile-color color/black)
          cache-entry (get @light-cache position :not-found)
          blocked? (if (= cache-entry :not-found)
                     (let [blocked? (ray-blocked? light-position position)]
                       (swap! light-cache assoc position blocked?)
                       blocked?)
                     cache-entry)]
      #_(when @do-once
          (swap! ray-positions conj position))
      (if blocked?
        (if see-all-tiles? color/white base-color)
        (do (when-not explored?
              (swap! explored-tile-corners assoc (->tile position) true))
            color/white)))))

(defn tile-color-setter [light-position]
  (tile-color-setter* (atom {}) light-position))

;"Mouseover-Actor: "
#_(when-let [actor (mouse-on-actor?)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))

(declare start-world)

(defn- dev-menu-bar []
  (dev-menu*
   {:menus [{:label "Screens"
             :items [{:label "Map-editor"
                      :on-click (partial change-screen :screens/map-editor)}
                     {:label "Editor"
                      :on-click (partial change-screen :screens/editor)}
                     {:label "Main-Menu"
                      :on-click (partial change-screen :screens/main-menu)}]}
            {:label "World"
             :items (for [world (db/build-all :properties/worlds)]
                      {:label (str "Start " (:property/id world))
                       :on-click #(start-world world)})}
            {:label "Help"
             :items [{:label controls/help-text}]}]
    :update-labels [{:label "Mouseover-entity id"
                     :update-fn #(when-let [entity (mouseover-entity)] (:entity/id entity))
                     :icon "images/mouseover.png"}
                    {:label "elapsed-time"
                     :update-fn #(str (readable-number elapsed-time) " seconds")
                     :icon "images/clock.png"}
                    {:label "paused?"
                     :update-fn (fn [] paused?)}
                    {:label "GUI"
                     :update-fn gui-mouse-position}
                    {:label "World"
                     :update-fn #(mapv int (world-mouse-position))}
                    {:label "Zoom"
                     :update-fn #(cam/zoom (world-camera))
                     :icon "images/zoom.png"}
                    {:label "FPS"
                     :update-fn frames-per-second
                     :icon "images/fps.png"}]}))

(defn- dev-menu []
  (ui/table {:rows [[{:actor (vis/menu-bar->table (dev-menu-bar))
                      :expand-x? true
                      :fill-x? true
                      :colspan 1}]
                    [{:actor (doto (ui/label "")
                               (.setTouchable Touchable/disabled))
                      :expand? true
                      :fill-x? true
                      :fill-y? true}]]
             :fill-parent? true}))

; FIXME camera/viewport used @ line of sight & raycaster explored tiles
; fixed player viewing range use & for opponents too

(defn- widgets []
  [(if dev-mode?
     (dev-menu)
     (ui-actor {}))
   (ui/table {:rows [[{:actor (action-bar/create)
                       :expand? true
                       :bottom? true}]]
              :id :action-bar-table
              :cell-defaults {:pad 2}
              :fill-parent? true})
   (hp-mana-bar)
   (ui/group {:id :windows
              :actors [(entity-info-window)
                       (inventory/create)]})
   (ui-actor {:draw #(draw-gui-view (e-state-obj @player-eid))})
   (player-message/actor)])

(defn- windows []
  (:windows (screen-stage)))

(defn- check-window-hotkeys []
  (doseq [window-id [:inventory-window :entity-info-window]
          :when (controls/toggle-visible? window-id)]
    (actor/toggle-visible! (get (windows) window-id))))

(defn- close-all-windows []
  (let [windows (children (windows))]
    (when (some visible? windows)
      (run! #(set-visible % false) windows))))

(def ^:private ^:dbg-flag spawn-enemies? true)

(defn- spawn-enemies [tiled-map]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (spawn-creature (update props :position tile->middle))))

; player-creature needs mana & inventory
; till then hardcode :creatures/vampire
(defn- player-entity-props [start-position]
  {:position (tile->middle start-position)
   :creature-id :creatures/vampire
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? true
                :entity/free-skill-points 3
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles 1.5}})

(defn- world-init [{:keys [tiled-map start-position]}]
  (forge.world.tiled-map/init             tiled-map)
  (forge.world.explored-tile-corners/init tiled-map)
  (forge.world.grid/init                  tiled-map)
  (forge.world.entity-ids/init            tiled-map)
  (forge.world.content-grid/init          tiled-map)
  (forge.world.raycaster/init             tiled-map)
  (forge.world.time/init                  tiled-map)
  (forge.world.player/init
   (spawn-creature
    (player-entity-props start-position)))
  (when spawn-enemies?
    (spawn-enemies tiled-map)))

(defn- world-clear [] ; responsibility of screen? we are not creating the tiled-map here ...
  (when (bound? #'world-tiled-map)
    (dispose world-tiled-map)))

; depends on world-widgets & world-init ....
; so widgets comes from world-props .... as components ...
; and this all goes to world-init?
(defn start-world [world-props]
  ; TODO assert is :screens/world
  (reset-stage (widgets))
  (world-clear)
  (bind-root tick-error nil)
  ; generate level -> creates actually the tiled-map and
  ; start-position?
  ; other stuff just depend on it?!
  (world-init (generate-level world-props)))

; FIXME config/changeable inside the app (dev-menu ?)
(def ^:private ^:dbg-flag pausing? true)

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [eid]
  (try
   (doseq [k (keys @eid)]
     (try (when-let [v (k @eid)]
            (component/tick [k v] eid))
          (catch Throwable t
            (throw (ex-info "entity-tick" {:k k} t)))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(defn- tick-entities [entities]
  (run! tick-entity entities))

(defn- update-world []
  (manual-tick (e-state-obj @player-eid))
  (forge.world.mouseover-entity/frame-tick) ; this do always so can get debug info even when game not running
  (bind-root paused? (or tick-error
                         (and pausing?
                              (pause-game? (e-state-obj @player-eid))
                              (not (controls/unpaused?)))))
  (when-not paused?
    (forge.world.time/frame-tick)
    (let [entities (active-entities)]
      (update-potential-fields! entities)
      (try (tick-entities entities)
           (catch Throwable t
             (error-window! t)
             (bind-root tick-error t)))))
  (remove-destroyed)) ; do not pause this as for example pickup item, should be destroyed.

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    (g/rectangle x y (:width entity) (:height entity) color)))

(defn- render-entity! [system entity]
  (try
   (when show-body-bounds
     (draw-body-rect entity (if (:collides? entity) color/white :gray)))
   (run! #(system % entity) entity)
   (catch Throwable t
     (draw-body-rect entity :red)
     (pretty-pst t))))

(defn- render-entities
  "Draws entities in the correct z-order and in the order of render-systems for each z-order."
  [entities]
  (let [player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              render-z-order)
            system [component/render-below
                    component/render-default
                    component/render-above
                    component/render-info]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? player entity))]
      (render-entity! system entity))))

(defn- render-world []
  ; FIXME position DRY
  (cam/set-position! (world-camera) (:position @player-eid))
  ; FIXME position DRY
  (draw-tiled-map world-tiled-map
                  (tile-color-setter (cam/position (world-camera))))
  (draw-on-world-view (fn []
                       (debug-render-before-entities)
                       ; FIXME position DRY (from player)
                       (render-entities (map deref (active-entities)))
                       (debug-render-after-entities))))

(deftype WorldScreen []
  Screen
  (enter [_]
    (cam/set-zoom! (world-camera) 0.8))

  (exit [_]
    (set-cursor :cursors/default))

  (render [_]
    (clear-screen color/black)
    (render-world)
    (update-world)
    (controls/world-camera-zoom)
    (check-window-hotkeys)
    (cond (controls/close-windows?)
          (close-all-windows)

          (controls/minimap?)
          (change-screen :screens/minimap)))

  (dispose [_]
    (world-clear)))

(defn create []
  (stage/create
   {:screen (->WorldScreen)}))
