(ns cdq.ctx)

(def world-unit-scale (float (/ 48)))

(declare schemas
         db
         assets
         batch
         shape-drawer-texture
         shape-drawer
         cursors
         default-font
         graphics
         stage
         world
         elapsed-time
         delta-time
         player-eid
         paused?)

(def mouseover-eid nil)
