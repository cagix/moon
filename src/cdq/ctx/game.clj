(ns cdq.ctx.game
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]
            [cdq.world :as w]
            cdq.ui.dev-menu
            cdq.ui.action-bar
            cdq.ui.hp-mana-bar
            cdq.ui.windows.entity-info
            cdq.ui.windows.inventory
            cdq.entity.state.player-idle
            cdq.entity.state.player-item-on-cursor
            cdq.ui.player-state-draw
            cdq.ui.message
            [cdq.ui.stage :as stage]))

(defn- create-ui-actors [ctx]
  [(cdq.ui.dev-menu/create ctx ;graphics db
                           {:world-fns [[(requiring-resolve 'cdq.level.from-tmx/create)
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
  (assoc ctx :ctx/world (w/create (merge (:cdq.ctx.game/world config)
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
       (w/spawn-creature! world)
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
         (w/spawn-creature! world)
         (ctx/handle-txs! ctx)))
  ctx)

(defn reset-game-state! [ctx world-fn]
  (-> ctx
      reset-stage!
      (add-ctx-world world-fn)
      spawn-player!
      spawn-enemies!))
