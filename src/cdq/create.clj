(ns cdq.create
  (:require [gdl.utils :refer [dispose safe-merge tile->middle readable-number dev-mode?]]
            [gdl.context :as c]
            [gdl.graphics :as graphics]
            [gdl.graphics.camera :as cam]
            [gdl.grid2d :as g2d]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.group :as group]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui :refer [ui-actor]]
            [cdq.context :refer [spawn-creature mouseover-entity]]
            [cdq.context.info :as info]
            [cdq.content-grid :as content-grid]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.level :refer [generate-level]]
            [cdq.ui.dev-menu :as dev-menu]
            [cdq.val-max :as val-max]
            [cdq.widgets.inventory :as inventory]))

(defn- action-bar-button-group []
  (let [actor (ui-actor {})]
    (.setName actor "action-bar/button-group")
    (actor/set-user-object actor (ui/button-group {:max-check-count 1
                                                   :min-check-count 0}))
    actor))

(defn- action-bar []
  (let [group (ui/horizontal-group {:pad 2 :space 2})]
    (.setUserObject group :ui/action-bar)
    (group/add-actor! group (action-bar-button-group))
    group))

(defn- action-bar-table [_context]
  (ui/table {:rows [[{:actor (action-bar)
                      :expand? true
                      :bottom? true}]]
             :id :action-bar-table
             :cell-defaults {:pad 2}
             :fill-parent? true}))

(defn- draw-player-message [{:keys [gdl.graphics/ui-viewport
                                    cdq.context/player-message] :as c}]
  (when-let [text (:text @player-message)]
    (c/draw-text c
                 {:x (/ (:width ui-viewport) 2)
                  :y (+ (/ (:height ui-viewport) 2) 200)
                  :text text
                  :scale 2.5
                  :up? true})))

(defn- check-remove-message [{:keys [cdq.context/player-message
                                     gdl/graphics]}]
  (when (:text @player-message)
    (swap! player-message update :counter + (graphics/delta-time graphics))
    (when (>= (:counter @player-message)
              (:duration-seconds @player-message))
      (swap! player-message dissoc :counter :text))))

(defn- player-message []
  (ui-actor {:draw draw-player-message
             :act  check-remove-message}))

(defn- render-infostr-on-bar [c infostr x y h]
  (c/draw-text c
               {:text infostr
                :x (+ x 75)
                :y (+ y 2)
                :up? true}))

(defn- hp-mana-bar [{:keys [gdl.graphics/ui-viewport] :as c}]
  (let [rahmen      (c/sprite c "images/rahmen.png")
        hpcontent   (c/sprite c "images/hp.png")
        manacontent (c/sprite c "images/mana.png")
        x (/ (:width ui-viewport) 2)
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-mana 80 ; action-bar-icon-size
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [c x y contentimage minmaxval name]
                            (c/draw-image c rahmen [x y])
                            (c/draw-image c
                                          (c/sub-sprite c
                                                        contentimage
                                                        [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                          [x y])
                            (render-infostr-on-bar c (str (readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui-actor {:draw (fn [{:keys [cdq.context/player-eid] :as c}]
                       (let [player-entity @player-eid
                             x (- x (/ rahmenw 2))]
                         (render-hpmana-bar c x y-hp   hpcontent   (entity/hitpoints   player-entity) "HP")
                         (render-hpmana-bar c x y-mana manacontent (entity/mana        player-entity) "MP")))})))

(def ^:private help-text
  "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause")

(defn- dev-menu-config [c]
  {:menus [{:label "World"
            :items (for [world (c/build-all c :properties/worlds)]
                     {:label (str "Start " (:property/id world))
                      :on-click
                      (fn [_context])
                      ;#(world/create % (:property/id world))

                      })}
           ; TODO fixme does not work because create world uses create-into which checks key is not preseent
           ; => look at cleanup-world/reset-state/ (camera not reset - mutable state be careful ! -> create new cameras?!)
           ; => also world-change should be supported, use component systems
           {:label "Help"
            :items [{:label help-text}]}]
   :update-labels [{:label "Mouseover-entity id"
                    :update-fn (fn [c]
                                 (when-let [entity (mouseover-entity c)]
                                   (:entity/id entity)))
                    :icon "images/mouseover.png"}
                   {:label "elapsed-time"
                    :update-fn (fn [{:keys [gdl.context/elapsed-time]}]
                                 (str (readable-number elapsed-time) " seconds"))
                    :icon "images/clock.png"}
                   {:label "paused?"
                    :update-fn :cdq.context/paused?} ; TODO (def paused ::paused) @ cdq.context
                   {:label "GUI"
                    :update-fn c/mouse-position}
                   {:label "World"
                    :update-fn #(mapv int (c/world-mouse-position %))}
                   {:label "Zoom"
                    :update-fn #(cam/zoom (:camera (:gdl.graphics/world-viewport %)))
                    :icon "images/zoom.png"}
                   {:label "FPS"
                    :update-fn (fn [{:keys [gdl/graphics]}]
                                 (graphics/frames-per-second graphics))
                    :icon "images/fps.png"}]})

(defn- dev-menu [c]
  (if dev-mode?
    (dev-menu/table c (dev-menu-config c))
    (ui-actor {})))

(def ^:private disallowed-keys [:entity/skills
                                #_:entity/fsm
                                :entity/faction
                                :active-skill])

(defn- ->label-text [c]
  ; items then have 2x pretty-name
  #_(.setText (.getTitleLabel window)
              (if-let [entity (mouseover-entity c)]
                (info/text c [:property/pretty-name (:property/pretty-name entity)])
                "Entity Info"))
  (when-let [entity (mouseover-entity c)]
    (info/text c ; don't use select-keys as it loses Entity record type
               (apply dissoc entity disallowed-keys))))

(defn- entity-info-window [{:keys [gdl.graphics/ui-viewport] :as c}]
  (let [label (ui/label "")
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position [(:width ui-viewport) 0]
                           :rows [[{:actor label :expand? true}]]})]
    ; do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (group/add-actor! window (ui-actor {:act (fn [context]
                                               (.setText label (str (->label-text context)))
                                               (.pack window))}))
    window))

(defn- widgets-windows [c]
  (ui/group {:id :windows
             :actors [(entity-info-window c)
                      (inventory/create c)]}))

(defn- widgets-player-state-draw-component [_context]
  (ui-actor {:draw #(entity/draw-gui-view (entity/state-obj @(:cdq.context/player-eid %))
                                          %)}))

(defn- stage-actors [c]
  [(dev-menu c)
   (action-bar-table c)
   (hp-mana-bar c)
   (widgets-windows c)
   (widgets-player-state-draw-component c)
   (player-message)])

(defn- create-content-grid [tiled-map {:keys [cell-size]}]
  (content-grid/create {:cell-size cell-size
                        :width  (tiled/tm-width  tiled-map)
                        :height (tiled/tm-height tiled-map)}))

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  grid/Cell
  (blocked? [_ z-order]
    (case movement
      :none true ; wall
      :air (case z-order ; water/doodads
             :z-order/flying false
             :z-order/ground true)
      :all false)) ; ground/floor

  (blocks-vision? [_]
    (= movement :none))

  (occupied-by-other? [_ eid]
    (some #(not= % eid) occupied))

  (nearest-entity [this faction]
    (-> this faction :eid))

  (nearest-entity-distance [this faction]
    (-> this faction :distance)))

(defn- ->grid-cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(defn- create-grid [tiled-map]
  (g2d/create-grid
   (tiled/tm-width tiled-map)
   (tiled/tm-height tiled-map)
   (fn [position]
     (atom (->grid-cell position
                        (case (tiled/movement-property tiled-map position)
                          "none" :none
                          "air"  :air
                          "all"  :all))))))

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

(defn- spawn-player-entity [context start-position]
  (spawn-creature context (player-entity-props start-position)))

(defn- spawn-enemies! [c tiled-map]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (spawn-creature c (update props :position tile->middle))))

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn- create-raycaster [grid]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell grid/blocks-vision?))
    [arr width height]))

(defn- explored-tile-corners [tiled-map]
  (atom (g2d/create-grid
         (tiled/tm-width  tiled-map)
         (tiled/tm-height tiled-map)
         (constantly false))))

(defn add-new-game-context [context config]
  (let [context (safe-merge context
                            {:gdl.context/elapsed-time 0
                             :cdq.context/player-message (atom (:player-message config))})]
    (c/reset-stage context (stage-actors context))
    (let [level (generate-level context
                                (c/build context (:world-id config)))
          tiled-map (:tiled-map level)
          grid (create-grid tiled-map)
          context (safe-merge context
                              {:cdq.context/error nil
                               :cdq.context/level level
                               :cdq.context/tiled-map tiled-map
                               :cdq.context/grid grid
                               :cdq.context/explored-tile-corners (explored-tile-corners tiled-map)
                               :cdq.context/content-grid (create-content-grid tiled-map (:content-grid config))
                               :cdq.context/entity-ids (atom {})
                               :cdq.context/raycaster (create-raycaster grid)
                               :cdq.context/factions-iterations (:factions-iterations config)})
          context (assoc context :cdq.context/player-eid (spawn-player-entity context (:start-position level)))]
      (spawn-enemies! context tiled-map)
      context)))
