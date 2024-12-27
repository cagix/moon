(ns anvil.world.create
  (:require [anvil.level :refer [generate-level]]
            [cdq.context :as world]
            [cdq.grid :as grid]
            [gdl.context :as c]
            [gdl.tiled :as tiled]))

(defn- spawn-enemies [c tiled-map]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (world/creature c (update props :position tile->middle))))

(defn- world-components [c world-id]
  (let [{:keys [tiled-map start-position]} (generate-level c
                                                           (c/build c world-id))]
    [[:cdq.context/tiled-map tiled-map]
     [:cdq.context/start-position start-position]
     [:cdq.context/grid nil]
     [:cdq.context/explored-tile-corners nil]
     [:cdq.context/content-grid {:cell-size 16}]
     [:cdq.context/entity-ids nil]
     [:cdq.context/raycaster nil]
     [:cdq.context/factions-iterations {:good 15 :evil 5}]
     ; "The elapsed in-game-time in seconds (not counting when game is paused)."
     [:cdq.context/elapsed-time nil] ; game speed config!?
     [:cdq.context/player-eid nil] ; pass props
     ;:mouseover-eid nil ; ?
     ;:delta-time "The game logic update delta-time in ms."
     ;(bind-root world/delta-time nil) ?
     [:cdq.context/error nil]]))

(def ^:private ^:dbg-flag spawn-enemies? true)

(defn-impl world/create [c world-id]
  (world/dispose c)
  (c/reset-stage c (world/widgets c))
  (let [c (c/create-into c (world-components c world-id))]
    (when spawn-enemies?
      (spawn-enemies c (:cdq.context/tiled-map c)))
    c))
