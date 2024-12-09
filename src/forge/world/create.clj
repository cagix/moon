(ns forge.world.create
  (:require [anvil.app :as app]
            [anvil.content-grid :as content-grid]
            [anvil.controls :as controls]
            [anvil.db :as db]
            [anvil.fsm :as fsm]
            [anvil.graphics :as g]
            [anvil.hitpoints :as hp]
            [anvil.info :as info]
            [anvil.inventory :as inventory]
            [anvil.level :refer [generate-level]]
            [anvil.mana :as mana]
            [anvil.skills :as skills]
            [anvil.stage :as stage]
            [anvil.ui :refer [ui-actor] :as ui]
            [anvil.val-max :as val-max]
            [anvil.world :as world :refer [mouseover-entity]]
            [clojure.component :as component]
            [clojure.gdx.graphics :refer [frames-per-second]]
            [clojure.gdx.graphics.camera :as cam]
            [clojure.gdx.scene2d.group :refer [add-actor!]]
            [clojure.gdx.tiled :as tiled]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [clojure.utils :refer [dev-mode? tile->middle bind-root
                                   readable-number]]
            [clojure.vis-ui :as vis]
            [data.grid2d :as g2d]
            [forge.ui.player-message :as player-message])
  (:import (com.badlogic.gdx.scenes.scene2d Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Table)))

(defn- render-infostr-on-bar [infostr x y h]
  (g/draw-text {:text infostr
                :x (+ x 75)
                :y (+ y 2)
                :up? true}))

(defn- hp-mana-bar []
  (let [rahmen      (g/->image "images/rahmen.png")
        hpcontent   (g/->image "images/hp.png")
        manacontent (g/->image "images/mana.png")
        x (/ g/gui-viewport-width 2)
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-mana 80 ; action-bar-icon-size
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [x y contentimage minmaxval name]
                            (g/draw-image rahmen [x y])
                            (g/draw-image (g/sub-image contentimage [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                          [x y])
                            (render-infostr-on-bar (str (readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui-actor {:draw (fn []
                       (let [player-entity @world/player-eid
                             x (- x (/ rahmenw 2))]
                         (render-hpmana-bar x y-hp   hpcontent   (hp/->value   player-entity) "HP")
                         (render-hpmana-bar x y-mana manacontent (mana/->value player-entity) "MP")))})))

(defn- menu-item [text on-clicked]
  (doto (vis/menu-item text)
    (.addListener (ui/change-listener on-clicked))))

(defn- add-upd-label
  ([table text-fn icon]
   (let [icon (ui/image->widget (g/->image icon) {})
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
                           :position [g/gui-viewport-width 0]
                           :rows [[{:actor label :expand? true}]]})]
    ; TODO do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (add-actor! window (ui-actor {:act (fn update-label-text []
                                         ; items then have 2x pretty-name
                                         #_(.setText (.getTitleLabel window)
                                                     (if-let [entity (mouseover-entity)]
                                                       (info/text [:property/pretty-name (:property/pretty-name entity)])
                                                       "Entity Info"))
                                         (.setText label
                                                   (str (when-let [entity (mouseover-entity)]
                                                          (info/text
                                                           ; don't use select-keys as it loses Entity record type
                                                           (apply dissoc entity disallowed-keys)))))
                                         (.pack window))}))
    window))


(declare create-world)

;"Mouseover-Actor: "
#_(when-let [actor (stage/mouse-on-actor?)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))

(defn- dev-menu-bar []
  (dev-menu*
   {:menus [{:label "Screens"
             :items [{:label "Map-editor"
                      :on-click (partial app/change-screen :screens/map-editor)}
                     {:label "Editor"
                      :on-click (partial app/change-screen :screens/editor)}
                     {:label "Main-Menu"
                      :on-click (partial app/change-screen :screens/main-menu)}]}
            {:label "World"
             :items (for [world (db/build-all :properties/worlds)]
                      {:label (str "Start " (:property/id world))
                       :on-click #(create-world world)})}
            {:label "Help"
             :items [{:label controls/help-text}]}]
    :update-labels [{:label "Mouseover-entity id"
                     :update-fn #(when-let [entity (mouseover-entity)] (:entity/id entity))
                     :icon "images/mouseover.png"}
                    {:label "elapsed-time"
                     :update-fn #(str (readable-number world/elapsed-time) " seconds")
                     :icon "images/clock.png"}
                    {:label "paused?"
                     :update-fn (fn [] world/paused?)}
                    {:label "GUI"
                     :update-fn g/gui-mouse-position}
                    {:label "World"
                     :update-fn #(mapv int (g/world-mouse-position))}
                    {:label "Zoom"
                     :update-fn #(cam/zoom (g/world-camera))
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

(defn- widgets []
  [(if dev-mode?
     (dev-menu)
     (ui-actor {}))
   (ui/table {:rows [[{:actor (skills/action-bar)
                       :expand? true
                       :bottom? true}]]
              :id :action-bar-table
              :cell-defaults {:pad 2}
              :fill-parent? true})
   (hp-mana-bar)
   (ui/group {:id :windows
              :actors [(entity-info-window)
                       (inventory/create)]})
   (ui-actor {:draw #(component/draw-gui-view (fsm/state-obj @world/player-eid))})
   (player-message/actor)])

(defn dispose-world []
  (when (bound? #'world/tiled-map)
    (dispose world/tiled-map)))

(def ^:private ^:dbg-flag spawn-enemies? true)

(defn- spawn-enemies [tiled-map]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (world/spawn-creature (update props :position tile->middle))))

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

(defn- time-init []
  (bind-root world/elapsed-time 0)
  (bind-root world/world-delta nil))

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn- init-raycaster* [grid position->blocked?]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell position->blocked?))
    (bind-root world/raycaster [arr width height])))

(defn init-raycaster [tiled-map]
  (init-raycaster* world/grid world/blocks-vision?))

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  world/Cell
  (cell-blocked? [_ z-order]
    (case movement
      :none true ; wall
      :air (case z-order ; water/doodads
             :z-order/flying false
             :z-order/ground true)
      :all false)) ; ground/floor

  (blocks-vision? [_]
    (= movement :none))

  (occupied-by-other? [_ eid]
    (some #(not= % eid) occupied)) ; contains? faster?

  (nearest-entity [this faction]
    (-> this faction :eid))

  (nearest-entity-distance [this faction]
    (-> this faction :distance)))

(defn- ->cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(defn- init-world-grid [tiled-map]
  (bind-root world/grid (g2d/create-grid
                         (tiled/tm-width tiled-map)
                         (tiled/tm-height tiled-map)
                         (fn [position]
                           (atom (->cell position
                                         (case (tiled/movement-property tiled-map position)
                                           "none" :none
                                           "air"  :air
                                           "all"  :all)))))))

(defn- world-init [{:keys [tiled-map start-position]}]
  (bind-root world/tiled-map tiled-map)
  (bind-root world/explored-tile-corners (atom (g2d/create-grid
                                                (tiled/tm-width  tiled-map)
                                                (tiled/tm-height tiled-map)
                                                (constantly false))))
  (init-world-grid tiled-map)
  (bind-root world/entity-ids {})
  (bind-root world/content-grid
             (content-grid/create {:cell-size 16  ; FIXME global config
                                   :width  (tiled/tm-width  tiled-map)
                                   :height (tiled/tm-height tiled-map)}))
  (init-raycaster tiled-map)
  (time-init)
  (bind-root world/player-eid
   (world/spawn-creature
    (player-entity-props start-position)))
  (when spawn-enemies?
    (spawn-enemies tiled-map)))

(defn create-world [world-props]
  ; TODO assert is :screens/world
  (stage/reset (widgets))
  (dispose-world)
  (bind-root world/tick-error nil)
  ; generate level -> creates actually the tiled-map and
  ; start-position?
  ; other stuff just depend on it?!
  (world-init (generate-level world-props)))
