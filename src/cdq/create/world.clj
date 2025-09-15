(ns cdq.create.world
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.db :as db]
            [cdq.malli :as m]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.explored-tile-corners :as explored-tile-corners]
            [cdq.world.grid :as grid]
            [cdq.world.raycaster :as raycaster]
            [clojure.gdx.maps.tiled :as tiled]
            [clojure.utils :as utils]
            [reduce-fsm :as fsm]))

(def ^:private npc-fsm
  (fsm/fsm-inc
   [[:npc-sleeping
     :kill -> :npc-dead
     :stun -> :stunned
     :alert -> :npc-idle]
    [:npc-idle
     :kill -> :npc-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :movement-direction -> :npc-moving]
    [:npc-moving
     :kill -> :npc-dead
     :stun -> :stunned
     :timer-finished -> :npc-idle]
    [:active-skill
     :kill -> :npc-dead
     :stun -> :stunned
     :action-done -> :npc-idle]
    [:stunned
     :kill -> :npc-dead
     :effect-wears-off -> :npc-idle]
    [:npc-dead]]))

(def ^:private player-fsm
  (fsm/fsm-inc
   [[:player-idle
     :kill -> :player-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :pickup-item -> :player-item-on-cursor
     :movement-input -> :player-moving]
    [:player-moving
     :kill -> :player-dead
     :stun -> :stunned
     :no-movement-input -> :player-idle]
    [:active-skill
     :kill -> :player-dead
     :stun -> :stunned
     :action-done -> :player-idle]
    [:stunned
     :kill -> :player-dead
     :effect-wears-off -> :player-idle]
    [:player-item-on-cursor
     :kill -> :player-dead
     :stun -> :stunned
     :drop-item -> :player-idle
     :dropped-item -> :player-idle]
    [:player-dead]]))

(def ^:private components-schema
  (m/schema [:map {:closed true}
             [:entity/body :some]
             [:entity/image {:optional true} :some]
             [:entity/animation {:optional true} :some]
             [:entity/delete-after-animation-stopped? {:optional true} :some]
             [:entity/alert-friendlies-after-duration {:optional true} :some]
             [:entity/line-render {:optional true} :some]
             [:entity/delete-after-duration {:optional true} :some]
             [:entity/destroy-audiovisual {:optional true} :some]
             [:entity/fsm {:optional true} :some]
             [:entity/player? {:optional true} :some]
             [:entity/free-skill-points {:optional true} :some]
             [:entity/click-distance-tiles {:optional true} :some]
             [:entity/clickable {:optional true} :some]
             [:property/id {:optional true} :some]
             [:property/pretty-name {:optional true} :some]
             [:creature/level {:optional true} :some]
             [:entity/faction {:optional true} :some]
             [:entity/species {:optional true} :some]
             [:entity/movement {:optional true} :some]
             [:entity/skills {:optional true} :some]
             [:creature/stats {:optional true} :some]
             [:entity/inventory    {:optional true} :some]
             [:entity/item {:optional true} :some]
             [:entity/projectile-collision {:optional true} :some]]))

(defn- call-world-fn
  [[f params] creature-properties graphics]
  (f
   (assoc params
          :creature-properties creature-properties
          :graphics graphics)))

(defn- create-tiled-map
  [{:keys [ctx/db
           ctx/graphics]
    :as ctx}
   world-fn]
  (let [{:keys [tiled-map
                start-position]} (call-world-fn world-fn
                                                (db/all-raw db :properties/creatures)
                                                graphics)
        grid (grid/create (:tiled-map/width  tiled-map)
                          (:tiled-map/height tiled-map)
                          #(case (tiled/movement-property tiled-map %)
                             "none" :none
                             "air"  :air
                             "all"  :all))
        config {:content-grid-cell-size 16
                :potential-field-factions-iterations {:good 15
                                                      :evil 5}
                :world/max-delta 0.04
                :world/minimum-size 0.39
                :world/z-orders [:z-order/on-ground
                                 :z-order/ground
                                 :z-order/flying
                                 :z-order/effect]}]
    (assoc ctx :ctx/world {:world/tiled-map tiled-map
                           :world/start-position start-position
                           :world/grid grid
                           :world/content-grid (content-grid/create (:tiled-map/width  tiled-map)
                                                                    (:tiled-map/height tiled-map)
                                                                    (:content-grid-cell-size config))
                           :world/explored-tile-corners (explored-tile-corners/create (:tiled-map/width  tiled-map)
                                                                                      (:tiled-map/height tiled-map))
                           :world/raycaster (raycaster/create grid)
                           :world/elapsed-time 0
                           :world/max-delta    (:world/max-delta    config)
                           :world/minimum-size (:world/minimum-size config)
                           :world/z-orders     (:world/z-orders     config)
                           :world/max-speed (/ (:world/minimum-size config)
                                               (:world/max-delta    config))
                           :world/potential-field-cache (atom nil)
                           :world/factions-iterations (:potential-field-factions-iterations config)
                           :world/id-counter (atom 0)
                           :world/entity-ids (atom {})
                           :world/render-z-order (utils/define-order (:world/z-orders config))
                           :world/spawn-entity-schema components-schema
                           :world/fsms {:fsms/player player-fsm
                                        :fsms/npc npc-fsm}
                           :world/enemy-components {:entity/fsm {:fsm :fsms/npc
                                                                 :initial-state :npc-sleeping}
                                                    :entity/faction :evil}
                           :world/player-components {:creature-id :creatures/vampire
                                                     :components {:entity/fsm {:fsm :fsms/player
                                                                               :initial-state :player-idle}
                                                                  :entity/faction :good
                                                                  :entity/player? true
                                                                  :entity/free-skill-points 3
                                                                  :entity/clickable {:type :clickable/player}
                                                                  :entity/click-distance-tiles 1.5}}
                           :world/effect-body-props {:width 0.5
                                                     :height 0.5
                                                     :z-order :z-order/effect}
                           })))

(defn- spawn-player!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (ctx/handle-txs! ctx
                   [[:tx/spawn-creature (let [{:keys [creature-id
                                                      components]} (:world/player-components world)]
                                          {:position (utils/tile->middle (:world/start-position world))
                                           :creature-property (db/build db creature-id)
                                           :components components})]])
  (let [eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @eid))
    (assoc-in ctx [:ctx/world :world/player-eid] eid)))

(defn- spawn-enemies!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (doseq [[position creature-id] (tiled/positions-with-property (:world/tiled-map world) "creatures" "id")]
    (ctx/handle-txs! ctx
                     [[:tx/spawn-creature {:position (utils/tile->middle position)
                                           :creature-property (db/build db (keyword creature-id))
                                           :components (:world/enemy-components world)}]]))
  ctx)

(defn do!
  [ctx world-fn]
  (-> ctx
      (create-tiled-map world-fn)
      spawn-player!
      spawn-enemies!))
