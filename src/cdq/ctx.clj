(ns cdq.ctx
  (:require [cdq.malli :as m]
            [cdq.utils :as utils]))

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta 0.04)

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
; TODO assert at properties load
(def minimum-size 0.39) ; == spider smallest creature size.

; set max speed so small entities are not skipped by projectiles
; could set faster than max-speed if I just do multiple smaller movement steps in one frame
(def ^:private max-speed (/ minimum-size max-delta)) ; need to make var because s/schema would fail later if divide / is inside the schema-form

(def speed-schema (m/schema [:and number? [:>= 0] [:<= max-speed]]))

(def z-orders [:z-order/on-ground
               :z-order/ground
               :z-order/flying
               :z-order/effect])

(def render-z-order (utils/define-order z-orders))

(def factions-iterations {:good 15 :evil 5})

(def ^:dbg-flag show-tile-grid? false)
(def ^:dbg-flag show-potential-field-colors? false) ; :good, :evil
(def ^:dbg-flag show-cell-entities? false)
(def ^:dbg-flag show-cell-occupied? false)

(def ^:dbg-flag show-body-bounds? false)

(def player-entity-config {:creature-id :creatures/vampire
                           :free-skill-points 3
                           :click-distance-tiles 1.5})
