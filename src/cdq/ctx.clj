(ns cdq.ctx
  (:require [cdq.malli :as m]
            [cdq.utils :as utils]))

(comment
 (spit "asset_diagnostics" (.getDiagnostics assets))

 (count (seq (.getAssetNames assets)))

 (spit "assets"
       (with-out-str
        (clojure.pprint/pprint
         (for [asset-name (.getAssetNames assets)]
           [asset-name (.get assets asset-name)]))))
 )

(declare config)

(def pausing? true)

(def zoom-speed 0.025)

(def controls {:zoom-in :minus
               :zoom-out :equals
               :unpause-once :p
               :unpause-continously :space
               })

(def sound-path-format "sounds/%s.wav")

(declare world-unit-scale)

(def unit-scale (atom 1))

(declare db
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
         tiled-map
         start-position
         grid
         raycaster
         content-grid
         explored-tile-corners
         id-counter
         entity-ids
         potential-field-cache
         active-entities
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

; set max speed so small entities are not skipped by projectiles
; could set faster than max-speed if I just do multiple smaller movement steps in one frame
(def ^:private max-speed (/ minimum-size max-delta)) ; need to make var because s/schema would fail later if divide / is inside the schema-form

(def speed-schema (m/schema [:and number? [:>= 0] [:<= max-speed]]))

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

(defn make-map []
  {:ctx/get-tiled-map-renderer get-tiled-map-renderer
   :ctx/tiled-map tiled-map
   :ctx/explored-tile-corners explored-tile-corners
   :ctx/pausing? pausing?
   :ctx/controls controls
   :ctx/zoom-speed zoom-speed
   :ctx/active-entities active-entities
   :ctx/default-font default-font
   :ctx/batch        batch
   :ctx/unit-scale   unit-scale
   :ctx/shape-drawer shape-drawer
   :ctx/shape-drawer-texture shape-drawer-texture
   :ctx/world-unit-scale world-unit-scale
   :ctx/world-viewport world-viewport
   :ctx/elapsed-time elapsed-time
   :ctx/effect-body-props effect-body-props
   :ctx/content-grid content-grid
   :ctx/player-eid player-eid
   :ctx/grid grid
   :ctx/cursors cursors
   :ctx/stage stage
   :ctx/ui-viewport ui-viewport
   :ctx/assets assets
   :ctx/sound-path-format sound-path-format
   :ctx/db db
   :ctx/minimum-size minimum-size
   :ctx/z-orders z-orders
   :ctx/id-counter id-counter
   :ctx/entity-ids entity-ids
   :ctx/delta-time delta-time
   :ctx/raycaster raycaster
   :ctx/paused? paused?
   :ctx/mouseover-eid mouseover-eid
   })

(defn handle-txs! [transactions]
  (let [ctx (make-map)]
    (doseq [transaction transactions
            :when transaction
            :let [_ (assert (vector? transaction)
                            (pr-str transaction))
                  ; TODO also should be with namespace 'tx' the first is a keyword
                  sym (symbol (str "cdq.tx." (name (first transaction)) "/do!"))
                  do! (requiring-resolve sym)]] ; TODO throw error if requiring failes ! compiler errors ... compile all tx/game first ?
      (try (apply do! (cons ctx (rest transaction)))
           (catch Throwable t
             (throw (ex-info ""
                             {:transaction transaction
                              :sym sym}
                             t)))))))
