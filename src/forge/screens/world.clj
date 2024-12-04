(ns forge.screens.world
  (:require [forge.base :refer :all]
            [forge.core :refer :all]
            [forge.controls :as controls]
            [forge.ui.inventory :as inventory]
            [forge.world.potential-fields :refer [update-potential-fields! factions-iterations]])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Table)
           (com.kotcrab.vis.ui.widget Menu MenuItem MenuBar)))

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
                            (draw-image (sub-image contentimage [0 0 (* rahmenw (val-max-ratio minmaxval)) rahmenh])
                                        [x y])
                            (render-infostr-on-bar (str (readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui-actor {:draw (fn []
                       (let [player-entity @player-eid
                             x (- x (/ rahmenw 2))]
                         (render-hpmana-bar x y-hp   hpcontent   (hitpoints   player-entity) "HP")
                         (render-hpmana-bar x y-mana manacontent (e-mana player-entity) "MP")))})))

(defn- menu-item [text on-clicked]
  (doto (MenuItem. text)
    (.addListener (change-listener on-clicked))))

(defn- add-upd-label
  ([table text-fn icon]
   (let [icon (image->widget (->image icon) {})
         label (label "")
         sub-table (ui-table {:rows [[icon label]]})]
     (add-actor! table (ui-actor {:act #(.setText label (str (text-fn)))}))
     (.expandX (.right (Table/.add table sub-table)))))
  ([table text-fn]
   (let [label (label "")]
     (add-actor! table (ui-actor {:act #(.setText label (str (text-fn)))}))
     (.expandX (.right (Table/.add table label))))))

(defn- add-update-labels [menu-bar update-labels]
  (let [table (MenuBar/.getTable menu-bar)]
    (doseq [{:keys [label update-fn icon]} update-labels]
      (let [update-fn #(str label ": " (update-fn))]
        (if icon
          (add-upd-label table update-fn icon)
          (add-upd-label table update-fn))))))

(defn- add-menu [menu-bar {:keys [label items]}]
  (let [app-menu (Menu. label)]
    (doseq [{:keys [label on-click]} items]
      (.addItem app-menu (menu-item label (or on-click (fn [])))))
    (MenuBar/.addMenu menu-bar app-menu)))

(defn- create-menu-bar [menus]
  (let [menu-bar (MenuBar.)]
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
  (let [label (label "")
        window (ui-window {:title "Info"
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
    (draw-circle position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (circle->cells circle))]
      (draw-rectangle x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (draw-rectangle x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(defn- tile-debug []
  (let [cam (world-camera)
        [left-x right-x bottom-y top-y] (frustum cam)]

    (when tile-grid?
      (draw-grid (int left-x) (int bottom-y)
                 (inc (int world-viewport-width))
                 (+ 2 (int world-viewport-height))
                 1 1 [1 1 1 0.8]))

    (doseq [[x y] (visible-tiles cam)
            :let [cell (world-grid [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (draw-filled-rectangle x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (draw-filled-rectangle x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (draw-filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile []
  (when highlight-blocked-cell?
    (let [[x y] (->tile (world-mouse-position))
          cell (get world-grid [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (draw-rectangle x y 1 1
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
          base-color (if explored? explored-tile-color black)
          cache-entry (get @light-cache position :not-found)
          blocked? (if (= cache-entry :not-found)
                     (let [blocked? (ray-blocked? light-position position)]
                       (swap! light-cache assoc position blocked?)
                       blocked?)
                     cache-entry)]
      #_(when @do-once
          (swap! ray-positions conj position))
      (if blocked?
        (if see-all-tiles? white base-color)
        (do (when-not explored?
              (swap! explored-tile-corners assoc (->tile position) true))
            white)))))

(defn tile-color-setter [light-position]
  (tile-color-setter* (atom {}) light-position))

;"Mouseover-Actor: "
#_(when-let [actor (mouse-on-actor?)]
    (str "TRUE - name:" (.getName actor)
         "id: " (.getUserObject actor)))

(defn- dev-menu-bar ^MenuBar []
  (dev-menu*
   {:menus [{:label "Screens"
             :items [{:label "Map-editor"
                      :on-click (partial change-screen :screens/map-editor)}
                     {:label "Editor"
                      :on-click (partial change-screen :screens/editor)}
                     {:label "Main-Menu"
                      :on-click (partial change-screen :screens/main-menu)}]}
            {:label "World"
             :items (for [world (build-all :properties/worlds)]
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
                     :update-fn #(zoom (world-camera))
                     :icon "images/zoom.png"}
                    {:label "FPS"
                     :update-fn frames-per-second
                     :icon "images/fps.png"}]}))

(defn- dev-menu []
  (ui-table {:rows [[{:actor (.getTable (dev-menu-bar))
                      :expand-x? true
                      :fill-x? true
                      :colspan 1}]
                    [{:actor (doto (label "")
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
   (ui-table {:rows [[{:actor (actionbar-create)
                       :expand? true
                       :bottom? true}]]
              :id :action-bar-table
              :cell-defaults {:pad 2}
              :fill-parent? true})
   (hp-mana-bar)
   (ui-group {:id :windows
              :actors [(entity-info-window)
                       (inventory/create)]})
   (ui-actor {:draw #(draw-gui-view (e-state-obj @player-eid))})
   (player-message-actor)])

(defn- windows []
  (:windows (screen-stage)))

(defn- check-window-hotkeys []
  (doseq [window-id [:inventory-window :entity-info-window]
          :when (controls/toggle-visible? window-id)]
    (toggle-visible! (get (windows) window-id))))

(defn- close-all-windows []
  (let [windows (children (windows))]
    (when (some visible? windows)
      (run! #(set-visible % false) windows))))

(bind-root #'start-world (fn start-world [world-props]
                           (change-screen :screens/world)
                           (reset-stage (widgets))
                           (world-clear)
                           (world-init (generate-level world-props))))

; FIXME config/changeable inside the app (dev-menu ?)
(def ^:private ^:dbg-flag pausing? true)

(defn- calculate-eid []
  (let [player @player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (point->entities
                      (world-mouse-position)))]
    (->> render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? player @%))
         first)))

(defn- update-mouseover-entity []
  (let [new-eid (if (mouse-on-actor?)
                  nil
                  (calculate-eid))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (bind-root #'mouseover-eid new-eid)))

(defn- update-world []
  (manual-tick (e-state-obj @player-eid))
  (update-mouseover-entity) ; this do always so can get debug info even when game not running
  (bind-root #'paused? (or tick-error
                           (and pausing?
                                (pause-game? (e-state-obj @player-eid))
                                (not (controls/unpaused?)))))
  (when-not paused?
    (let [delta-ms (min (delta-time) max-delta-time)]
      (alter-var-root #'elapsed-time + delta-ms)
      (bind-root #'world-delta delta-ms) )
    (let [entities (active-entities)]
      (update-potential-fields! entities)
      (try (tick-entities entities)
           (catch Throwable t
             (error-window! t)
             (bind-root #'tick-error t)))))
  (remove-destroyed)) ; do not pause this as for example pickup item, should be destroyed.

(defn- render-world []
  ; FIXME position DRY
  (set-position! (world-camera) (:position @player-eid))
  ; FIXME position DRY
  (draw-tiled-map world-tiled-map
                  (tile-color-setter (cam-position (world-camera))))
  (draw-on-world-view (fn []
                       (debug-render-before-entities)
                       ; FIXME position DRY (from player)
                       (render-entities (map deref (active-entities)))
                       (debug-render-after-entities))))

(deftype WorldScreen []
  Screen
  (screen-enter [_]
    (set-zoom! (world-camera) 0.8))

  (screen-exit [_]
    (set-cursor :cursors/default))

  (screen-render [_]
    (render-world)
    (update-world)
    (controls/world-camera-zoom)
    (check-window-hotkeys)
    (cond (controls/close-windows?)
          (close-all-windows)

          (controls/minimap?)
          (change-screen :screens/minimap)))

  (screen-destroy [_]
    (world-clear)))

(defn create []
  {:screen (->WorldScreen)})
