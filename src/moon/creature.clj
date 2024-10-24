(ns moon.creature
  (:require [clojure.string :as str]
            [gdl.graphics.color :as color]
            [gdl.tiled :as tiled]
            [gdl.utils :refer [safe-merge tile->middle]]
            [moon.component :refer [defc] :as component]
            [moon.creature.fsms :as fsms]
            [moon.db :as db]
            [moon.graphics :as g]
            [moon.effect :as effect]
            [moon.entity :as entity]
            [moon.world :as world]))

(color/put "ITEM_GOLD" [0.84 0.8 0.52])

(defc :property/pretty-name
  {:schema :string
   :let value}
  (component/info [_]
    (str "[ITEM_GOLD]"value"[]")))

(defc :body/width   {:schema pos?})
(defc :body/height  {:schema pos?})
(defc :body/flying? {:schema :boolean})

; player doesn;t need aggro-range/reaction-time
; stats armor-pierce wrong place
; assert min body size from entity

(defc :entity/body
  {:schema [:s/map [:body/width
                :body/height
                :body/flying?]]})

(defc :creature/species
  {:schema [:qualified-keyword {:namespace :species}]}
  (entity/->v [[_ species]]
    (str/capitalize (name species)))
  (component/info [[_ species]]
    (str "[LIGHT_GRAY]Creature - " species "[]")))

(defc :creature/level
  {:schema pos-int?}
  (component/info [[_ lvl]]
    (str "[GRAY]Level " lvl "[]")))

; # :z-order/flying has no effect for now
; * entities with :z-order/flying are not flying over water,etc. (movement/air)
; because using potential-field for z-order/ground
; -> would have to add one more potential-field for each faction for z-order/flying
; * they would also (maybe) need a separate occupied-cells if they don't collide with other
; * they could also go over ground units and not collide with them
; ( a test showed then flying OVER player entity )
; -> so no flying units for now
(defn- ->body [{:keys [body/width body/height #_body/flying?]}]
  {:width  width
   :height height
   :collides? true
   :z-order :z-order/ground #_(if flying? :z-order/flying :z-order/ground)})

(defc :tx/creature
  {:let {:keys [position creature-id components]}}
  (component/handle [_]
    (let [props (db/get creature-id)]
      [[:e/create
        position
        (->body (:entity/body props))
        (-> props
            (dissoc :entity/body)
            (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
            (safe-merge components))]])))

(def ^:private ^:dbg-flag spawn-enemies? true)

; player-creature needs mana & inventory
; till then hardcode :creatures/vampire
(defn- spawn-all [{:keys [tiled-map start-position]}]
  (component/->handle
   (for [creature (cons {:position start-position
                         :creature-id :creatures/vampire
                         :components {:entity/state {:fsm fsms/player
                                                     :initial-state :player-idle}
                                      :entity/faction :good
                                      :entity/player? true
                                      :entity/free-skill-points 3
                                      :entity/clickable {:type :clickable/player}
                                      :entity/click-distance-tiles 1.5}}
                        (when spawn-enemies?
                          (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                            {:position position
                             :creature-id (keyword creature-id)
                             :components {:entity/state {:fsm fsms/npc
                                                         :initial-state :npc-sleeping}
                                          :entity/faction :evil}})))]
     [:tx/creature (update creature :position tile->middle)])))

(.bindRoot #'world/spawn-entities spawn-all)

; https://github.com/damn/core/issues/29
(defc :effect/spawn
  {:schema [:s/one-to-one :properties/creatures]
   :let {:keys [property/id]}}
  (effect/applicable? [_]
    (and (:entity/faction @effect/source)
         effect/target-position))

  (component/handle [_]
    [[:tx/sound "sounds/bfxr_shield_consume.wav"]
     [:tx/creature {:position effect/target-position
                    :creature-id id ; already properties/get called through one-to-one, now called again.
                    :components {:entity/state {:fsm fsms/npc
                                                :initial-state :npc-idle}
                                 :entity/faction (:entity/faction @effect/source)}}]]))
