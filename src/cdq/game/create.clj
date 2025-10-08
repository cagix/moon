(ns cdq.game.create
  (:require [cdq.game.create.tx-handler :as create-tx-handler]
            [clojure.gdx :as gdx]
            [cdq.audio :as audio]
            [cdq.db :as db]
            [cdq.graphics.textures :as textures]
            [cdq.ui :as ui]
            cdq.impl.db
            cdq.impl.graphics
            cdq.impl.ui
            cdq.impl.world
            [cdq.world-fns.creature-tiles]
            [cdq.ui :as ui]
            [clojure.gdx.maps.tiled :as tiled]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.txs :as txs]
            [qrecord.core :as q]))

(q/defrecord Context [])

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
   (for [[position creature-id] (tiled/positions-with-property
                                 (:world/tiled-map world)
                                 "creatures"
                                 "id")]
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
                                                                             #(textures/texture-region graphics %))
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

(defn create-world
  [{:keys [ctx/db
           ctx/graphics
           ctx/world]
    :as ctx}
   world-fn]
  (let [world-fn-result (call-world-fn world-fn
                                       (db/all-raw db :properties/creatures)
                                       graphics)]
    (-> ctx
        (assoc :ctx/world (cdq.impl.world/create world-params world-fn-result))
        spawn-player!
        spawn-enemies!)))

(def graphics-params
  {:tile-size 48
   :ui-viewport {:width 1440
                 :height 900}
   :world-viewport {:width 1440
                    :height 900}
   :texture-folder {:folder "resources/"
                    :extensions #{"png" "bmp"}}
   :default-font {:path "exocet/films.EXL_____.ttf"
                  :params {:size 16
                           :quality-scaling 2
                           :enable-markup? true
                           :use-integer-positions? false
                           ; :texture-filter/linear because scaling to world-units
                           :min-filter :linear
                           :mag-filter :linear}}
   :cursors {:path-format "cursors/%s.png"
             :data {:cursors/bag                   ["bag001"       [0   0]]
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
                    :cursors/walking               ["walking"      [16 16]]}}})

(defn create-input
  [{:keys [ctx/stage]
    :as ctx}
   gdx]
  (gdx/set-input-processor! gdx stage)
  ctx)

(def ^:private sound-names (->> "sounds.edn" io/resource slurp edn/read-string))
(def ^:private path-format "sounds/%s.wav")

(defn do! [gdx]
  (-> {:ctx/gdx gdx}
      map->Context
      create-tx-handler/do!
      (assoc :ctx/db (cdq.impl.db/create))
      (assoc :ctx/graphics (cdq.impl.graphics/create! gdx graphics-params))
      (ui/create! '[[cdq.ctx.create.ui.dev-menu/create cdq.game.create/create-world]
                    [cdq.ctx.create.ui.action-bar/create]
                    [cdq.ctx.create.ui.hp-mana-bar/create]
                    [cdq.ctx.create.ui.windows/create [[cdq.ctx.create.ui.windows.entity-info/create]
                                                       [cdq.ctx.create.ui.windows.inventory/create]]]
                    [cdq.ctx.create.ui.player-state-draw/create]
                    [cdq.ctx.create.ui.message/create]])
      (create-input gdx)
      (assoc :ctx/audio (audio/create gdx sound-names path-format))
      (create-world "world_fns/vampire.edn")))
