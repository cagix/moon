(ns cdq.ctx
  (:require [cdq.utils :as utils]))

(def pausing? true)

(def zoom-speed 0.025)

(def controls {:zoom-in :minus
               :zoom-out :equals
               :unpause-once :p
               :unpause-continously :space
               })

(def sound-path-format "sounds/%s.wav")

(def cursor-path-format "cursors/%s.png")

(def cursor-config
  {:cursors/bag                   ["bag001"       [0   0]]
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
   :cursors/walking               ["walking"      [16 16]]})

(def font-config {:file "fonts/exocet/films.EXL_____.ttf"
                  :size 16
                  :quality-scaling 2})

(def world-unit-scale (float (/ 48)))

(def unit-scale (atom 1))

(def ui-viewport-config {:width 1440
                         :height 900})

(def world-viewport-config {:width 1440
                            :height 900})

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

(def factions-iterations {:good 15 :evil 5})

(def ^:dbg-flag show-tile-grid? false)
(def ^:dbg-flag show-potential-field-colors? false) ; :good, :evil
(def ^:dbg-flag show-cell-entities? false)
(def ^:dbg-flag show-cell-occupied? false)

(def ^:dbg-flag show-body-bounds? false)

(def player-entity-config {:creature-id :creatures/vampire
                           :free-skill-points 3
                           :click-distance-tiles 1.5})
