(ns cdq.game
  (:require [cdq.audio]
            [cdq.assets]
            [cdq.c]
            [cdq.create.db]
            [cdq.create.ui]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.dev.data-view :as data-view]
            [cdq.entity :as entity]
            cdq.entity.state.player-idle
            cdq.entity.state.player-item-on-cursor
            [cdq.graphics :as graphics]
            [cdq.graphics.camera :as camera]
            [cdq.grid :as grid]
            [cdq.input :as input]
            [cdq.malli :as m]
            [cdq.ui.stage :as stage]
            cdq.ui.dev-menu
            cdq.ui.action-bar
            cdq.ui.hp-mana-bar
            cdq.ui.windows.entity-info
            cdq.ui.windows.inventory
            cdq.ui.player-state-draw
            cdq.ui.message
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]
            [cdq.world :as world]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx.utils Disposable)))

(q/defrecord Context [ctx/config
                      ctx/input
                      ctx/db
                      ctx/audio
                      ctx/stage
                      ctx/graphics
                      ctx/world])

(def ^:private schema
  (m/schema [:map {:closed true}
             [:ctx/config :some]
             [:ctx/input :some]
             [:ctx/db :some]
             [:ctx/audio :some]
             [:ctx/stage :some]
             [:ctx/graphics :some]
             [:ctx/world :some]]))

(defn- validate [ctx]
  (m/validate-humanize schema ctx)
  ctx)

(declare ^:private reset-game-state!)

(defn- create-ui-actors [ctx]
  [(cdq.ui.dev-menu/create ctx ;graphics db
                           {:reset-game-state-fn reset-game-state!
                            :world-fns [[(requiring-resolve 'cdq.level.from-tmx/create)
                                         {:tmx-file "maps/vampire.tmx"
                                          :start-position [32 71]}]
                                        [(requiring-resolve 'cdq.level.uf-caves/create)
                                         {:tile-size 48
                                          :texture "maps/uf_terrain.png"
                                          :spawn-rate 0.02
                                          :scaling 3
                                          :cave-size 200
                                          :cave-style :wide}]
                                        [(requiring-resolve 'cdq.level.modules/create)
                                         {:world/map-size 5,
                                          :world/max-area-level 3,
                                          :world/spawn-rate 0.05}]]
                            ;icons, etc. , components ....
                            :info "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"})
    (cdq.ui.action-bar/create {:id :action-bar}) ; padding.... !, etc.

    ; graphics
    (cdq.ui.hp-mana-bar/create ctx
                               {:rahmen-file "images/rahmen.png"
                                :rahmenw 150
                                :rahmenh 26
                                :hpcontent-file "images/hp.png"
                                :manacontent-file "images/mana.png"
                                :y-mana 80}) ; action-bar-icon-size

    {:actor/type :actor.type/group
     :id :windows
     :actors [(cdq.ui.windows.entity-info/create ctx {:y 0}) ; graphics only
              (cdq.ui.windows.inventory/create ctx ; graphics only
               {:title "Inventory"
                :id :inventory-window
                :visible? false
                :state->clicked-inventory-cell
                {:player-idle           cdq.entity.state.player-idle/clicked-inventory-cell
                 :player-item-on-cursor cdq.entity.state.player-item-on-cursor/clicked-cell}})]}
    (cdq.ui.player-state-draw/create
     {:state->draw-gui-view
      {:player-item-on-cursor
       cdq.entity.state.player-item-on-cursor/draw-gui-view}})
    (cdq.ui.message/create {:duration-seconds 0.5
                            :name "player-message"})])

(defn- reset-stage!
  [{:keys [ctx/stage]
    :as ctx}]
  (stage/clear! stage)
  (doseq [actor (create-ui-actors ctx)]
    (stage/add! stage actor))
  ctx)

(defn- add-ctx-world
  [{:keys [ctx/config]
    :as ctx}
   world-fn]
  (assoc ctx :ctx/world (world/create (merge (:cdq.ctx.game/world config)
                                             (let [[f params] world-fn]
                                               (f ctx params))))))

(defn- spawn-player!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
    :as ctx}]
  (->> (let [{:keys [creature-id
                     components]} (:cdq.ctx.game/player-props config)]
         {:position (utils/tile->middle (:world/start-position world))
          :creature-property (db/build db creature-id)
          :components components})
       (world/spawn-creature! world)
       (ctx/handle-txs! ctx))
  (let [player-eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @player-eid))
    (assoc-in ctx [:ctx/world :world/player-eid] player-eid)))

(defn- spawn-enemies!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
    :as ctx}]
  (doseq [[position creature-id] (tiled/positions-with-property (:world/tiled-map world)
                                                                "creatures"
                                                                "id")]
    (->> {:position (utils/tile->middle position)
          :creature-property (db/build db (keyword creature-id))
          :components (:cdq.ctx.game/enemy-components config)}
         (world/spawn-creature! world)
         (ctx/handle-txs! ctx)))
  ctx)

(defn- reset-game-state! [ctx world-fn]
  (-> ctx
      reset-stage!
      (add-ctx-world world-fn)
      spawn-player!
      spawn-enemies!))

(defn create! [{:keys [audio files graphics input]}]
  (let [graphics (graphics/create
                  graphics
                  files
                  {:colors [["PRETTY_NAME" [0.84 0.8 0.52 1]]]
                   :textures (cdq.assets/search files
                                                {:folder "resources/"
                                                 :extensions #{"png" "bmp"}})
                   :tile-size 48
                   :ui-viewport    {:width 1440 :height 900}
                   :world-viewport {:width 1440 :height 900}
                   :cursor-path-format "cursors/%s.png"
                   :cursors {:cursors/bag                   ["bag001"       [0   0]]
                             :cursors/black-x               ["black_x"      [0   0]]
                             :cursors/default               ["default"      [0   0]]
                             :cursors/denied                ["denied"       [16 16]]
                             :cursors/hand-before-grab      ["hand004"      [4  16]]
                             :cursors/hand-before-grab-gray ["hand004_gray" [4  16]]
                             :cursors/hand-grab             ["hand003"      [4  16]]
                             :cursors/move-window           ["move002"      [16 16]]
                             :cursors/no-skill-selected     ["denied003"    [0   0]]
                             :cursors/over-button           ["hand002"      [0   0]]
                             :cursors/sandclock             ["sandclock"    [16 16]]
                             :cursors/skill-not-usable      ["x007"         [0   0]]
                             :cursors/use-skill             ["pointer004"   [0   0]]
                             :cursors/walking               ["walking"      [16 16]]}
                   :default-font {:file "exocet/films.EXL_____.ttf"
                                  :params {:size 16
                                           :quality-scaling 2
                                           :enable-markup? true
                                           ; false, otherwise scaling to world-units not visible
                                           :use-integer-positions? false}}})
        ctx (map->Context {:audio (cdq.audio/create audio files {:sounds "sounds.edn"})
                           :config {:cdq.ctx.game/enemy-components {:entity/fsm {:fsm :fsms/npc
                                                                                 :initial-state :npc-sleeping}
                                                                    :entity/faction :evil}
                                    :cdq.ctx.game/player-props {:creature-id :creatures/vampire
                                                                :components {:entity/fsm {:fsm :fsms/player
                                                                                          :initial-state :player-idle}
                                                                             :entity/faction :good
                                                                             :entity/player? true
                                                                             :entity/free-skill-points 3
                                                                             :entity/clickable {:type :clickable/player}
                                                                             :entity/click-distance-tiles 1.5}}
                                    :cdq.ctx.game/world {:content-grid-cell-size 16
                                                         :potential-field-factions-iterations {:good 15
                                                                                               :evil 5}}
                                    :effect-body-props {:width 0.5
                                                        :height 0.5
                                                        :z-order :z-order/effect}

                                    :controls {:zoom-in :minus
                                               :zoom-out :equals
                                               :unpause-once :p
                                               :unpause-continously :space}}
                           :db (cdq.create.db/do! {:schemas "schema.edn"
                                                   :properties "properties.edn"})
                           :graphics graphics
                           :input input
                           :stage (cdq.create.ui/do! graphics input {:skin-scale :x1})})]
    (-> ctx
        (reset-game-state! [(requiring-resolve 'cdq.level.from-tmx/create)
                            {:tmx-file "maps/vampire.tmx"
                             :start-position [32 71]}])
        validate)))

; TODO call dispose! on all components
(defn dispose! [{:keys [ctx/audio
                        ctx/graphics
                        ctx/world]}]
  (Disposable/.dispose audio)
  (Disposable/.dispose graphics)
  (Disposable/.dispose (:world/tiled-map world))
  ; TODO vis-ui dispose
  ; TODO what else disposable?
  ; => :ctx/tiled-map definitely and also dispose when re-creting gamestate.
  )

; TODO call resize! on all components
(defn resize! [{:keys [ctx/graphics]} width height]
  (graphics/resize-viewports! graphics width height))

(defn- check-open-debug-data-view!
  [{:keys [ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (when (input/button-just-pressed? input :right)
    (let [mouseover-eid (:world/mouseover-eid world)
          data (or (and mouseover-eid @mouseover-eid)
                   @(grid/cell (:world/grid world)
                               (mapv int (cdq.c/world-mouse-position ctx))))]
      (stage/add! stage (data-view/table-view-window {:title "Data View"
                                                      :data data
                                                      :width 500
                                                      :height 500}))))
  ctx)

(defn- assoc-active-entities [ctx]
  (update ctx :ctx/world world/cache-active-entities))

(defn- set-camera-on-player!
  [{:keys [ctx/world
           ctx/graphics]
    :as ctx}]
  (camera/set-position! (:viewport/camera (:world-viewport graphics))
                        (entity/position @(:world/player-eid world)))
  ctx)

(defn- clear-screen!
  [{:keys [ctx/graphics] :as ctx}]
  (graphics/clear-screen! graphics :black)
  ctx)

(defn render! [ctx]
  (reduce (fn [ctx f] (f ctx))
          (-> ctx
              validate
              check-open-debug-data-view! ; TODO FIXME its not documented I forgot rightclick can open debug data view!
              assoc-active-entities
              set-camera-on-player!
              clear-screen!
              )
          (map requiring-resolve
               '[
                 cdq.render.draw-world-map/do!
                 cdq.render.draw-on-world-viewport/do!
                 cdq.render.stage/do!
                 cdq.render.set-cursor/do!
                 cdq.render.player-state-handle-input/do!
                 cdq.render.update-mouseover-entity/do!
                 cdq.render.assoc-paused/do!
                 cdq.render.tick-world/do!
                 cdq.render.remove-destroyed-entities/do! ; do not pause as pickup item should be destroyed
                 cdq.render.camera-controls/do!
                 cdq.render.check-window-hotkeys/do!
                 cdq.game/validate])))
