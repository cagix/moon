(ns cdq.ctx
  (:require [cdq.utils :as utils]))

(def world-unit-scale (float (/ 48)))

(def unit-scale (atom 1))

(declare schemas
         db
         assets
         batch
         shape-drawer-texture
         shape-drawer
         cursors
         default-font
         world-viewport
         ui-viewport
         get-tiled-map-renderer
         stage
         world
         elapsed-time
         delta-time
         player-eid
         paused?)

(def mouseover-eid nil)

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta 0.04)

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
; TODO assert at properties load
(def minimum-size 0.39) ; == spider smallest creature size.

(def z-orders [:z-order/on-ground
               :z-order/ground
               :z-order/flying
               :z-order/effect])

(def render-z-order (utils/define-order z-orders))

(def ^{:doc "For effects just to have a mouseover body size for debugging purposes."}
  effect-body-props
  {:width 0.5
   :height 0.5
   :z-order :z-order/effect})
