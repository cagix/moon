(ns cdq.game.create
  (:require [cdq.game.create.dev-menu-config :as dev-menu-config]
            [cdq.game.create.hp-mana-bar-config :as hp-mana-bar-config]
            [cdq.game.create.entity-info-window-config :as entity-info-window-config]
            [cdq.game.create.inventory-window :as inventory-window]
            [cdq.game.create.txs]
            [cdq.audio :as audio]
            [cdq.db :as db]
            [cdq.graphics :as graphics]
            [cdq.graphics.impl]
            [cdq.input :as input]
            [cdq.ui :as ui]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.group :as build-group]
            [cdq.ui.dev-menu :as dev-menu]
            [cdq.ui.message :as message]
            [cdq.ui.stage :as stage]
            [cdq.world :as world]
            [cdq.world.info :as info]
            [cdq.world.tiled-map :as tiled-map]
            [cdq.world-fns.creature-tiles]
            [cdq.entity.state.player-item-on-cursor :as player-item-on-cursor]
            [clojure.gdx.scene2d.actor :as actor]
            [cdq.ui.info-window :as info-window]
            [cdq.ui.window :as window]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.txs :as txs]
            [clojure.utils :as utils]
            [qrecord.core :as q]))

(q/defrecord Context [])

(defn- create-hp-mana-bar* [create-draws]
  (actor/create
   {:act (fn [_this _delta])
    :draw (fn [actor _batch _parent-alpha]
            (when-let [stage (actor/stage actor)]
              (graphics/draw! (:ctx/graphics (stage/ctx stage))
                              (create-draws (stage/ctx stage)))))}))

(def state->draw-ui-view
  {:player-item-on-cursor (fn
                            [eid
                             {:keys [ctx/graphics
                                     ctx/input
                                     ctx/stage]}]
                            ; TODO see player-item-on-cursor at render layers
                            ; always draw it here at right position, then render layers does not need input/stage
                            ; can pass world to graphics, not handle here at application
                            (when (not (player-item-on-cursor/world-item? (ui/mouseover-actor stage (input/mouse-position input))))
                              [[:draw/texture-region
                                (graphics/texture-region graphics (:entity/image (:entity/item-on-cursor @eid)))
                                (:graphics/ui-mouse-position graphics)
                                {:center? true}]]))})

(defn- player-state-handle-draws
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [player-eid (:world/player-eid world)
        entity @player-eid
        state-k (:state (:entity/fsm entity))]
    (when-let [f (state->draw-ui-view state-k)]
      (graphics/draw! graphics (f player-eid ctx)))))

(def message-duration-seconds 0.5)

(declare rebuild-actors!
         create-world)

(defn- add-actors! [stage ctx]
  ; => stage-config passed once saved in the cdq.ui.Stage
  ; => can rebuild actors
  (doseq [actor [(dev-menu/create (dev-menu-config/create ctx rebuild-actors! create-world))
                 (action-bar/create)
                 (create-hp-mana-bar* (hp-mana-bar-config/create ctx))
                 (build-group/create
                  {:actor/name "cdq.ui.windows"
                   :group/actors [(info-window/create (entity-info-window-config/create ctx))
                                  (inventory-window/create ctx)]})
                 (actor/create
                  {:draw (fn [this _batch _parent-alpha]
                           (player-state-handle-draws (stage/ctx (actor/stage this))))
                   :act (fn [this _delta])})
                 (message/create message-duration-seconds)]]
    (stage/add-actor! stage actor)))

(defn rebuild-actors! [stage ctx]
  (stage/clear! stage)
  (add-actors! stage ctx))

(defn- spawn-player!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (txs/handle! ctx
               [[:tx/spawn-creature (let [{:keys [creature-id
                                                  components]} (:world/player-components world)]
                                      {:position (mapv (partial + 0.5) (:world/start-position world))
                                       :creature-property (db/build db creature-id)
                                       :components components})]])
  (let [eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @eid))
    (assoc-in ctx [:ctx/world :world/player-eid] eid)))

(defn- spawn-enemies!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (txs/handle!
   ctx
   (for [[position creature-id] (tiled-map/spawn-positions (:world/tiled-map world))]
     [:tx/spawn-creature {:position (mapv (partial + 0.5) position)
                          :creature-property (db/build db (keyword creature-id))
                          :components (:world/enemy-components world)}]))
  ctx)

(defn- call-world-fn
  [world-fn creature-properties graphics]
  (let [[f params] (-> world-fn io/resource slurp edn/read-string)]
    ((requiring-resolve f)
     (assoc params
            :level/creature-properties (cdq.world-fns.creature-tiles/prepare creature-properties
                                                                             #(graphics/texture-region graphics %))
            :textures (:graphics/textures graphics)))))

(def ^:private world-params
  {:content-grid-cell-size 16
   :world/factions-iterations {:good 15 :evil 5}
   :world/max-delta 0.04
   :world/minimum-size 0.39
   :world/z-orders [:z-order/on-ground
                    :z-order/ground
                    :z-order/flying
                    :z-order/effect]
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
                             :z-order :z-order/effect}})

(defn- create-world
  [{:keys [ctx/db
           ctx/graphics
           ctx/world]
    :as ctx}
   world-fn]
  (let [world-fn-result (call-world-fn world-fn
                                       (db/all-raw db :properties/creatures)
                                       graphics)]
    (-> ctx
        (assoc :ctx/world (world/create world-params world-fn-result))
        spawn-player!
        spawn-enemies!)))

(defn do!
  [{:keys [audio
           files
           graphics
           input]}
   config]
  (let [graphics (cdq.graphics.impl/create! graphics files (:graphics config))
        stage (ui/create! graphics)
        ctx (map->Context {})
        ctx (cdq.game.create.txs/do! ctx)
        ctx (merge ctx
                   {:ctx/audio (audio/create audio files (:audio config))
                    :ctx/db (db/create)
                    :ctx/graphics graphics
                    :ctx/input input
                    :ctx/stage stage})]
    (input/set-processor! input stage)
    (add-actors! stage ctx)
    (create-world ctx (:world config))))
