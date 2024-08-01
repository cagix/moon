(ns context.game
  (:require [utils.core :refer [safe-get]]
            [core.component :as component]
            [api.context :as ctx :refer [delta-time key-just-pressed? key-pressed? render-map render-entities! tick-entities! line-of-sight? content-grid remove-destroyed-entities! update-mouseover-entity! update-potential-fields! update-elapsed-game-time! debug-render-after-entities debug-render-before-entities transact-all! frame->txs windows]]
            [api.entity :as entity]
            [api.entity.state :as state]
            [api.graphics :as g]
            [api.graphics.camera :as camera]
            [api.input.keys :as input.keys]
            [api.scene2d.actor :refer [visible? set-visible! toggle-visible!]]
            [api.world.content-grid :refer [active-entities]]
            [app.state :refer [current-context change-screen!]]
            [context.world :as world]
            [entity.movement :as movement]))

(defn- fetch-player-entity [ctx]
  {:post [%]}
  (first (filter #(:entity/player? @%) (api.context/all-entities ctx))))

(defn- ->player-entity-context [ctx]
  {:context/player-entity (fetch-player-entity ctx)})

(defn- reset-common-game-context! [{:keys [context/game] :as ctx}]
  (let [components (map #(vector % nil) game)]
    (component/load! components)
    (reduce (fn [ctx [k v]]
              (assoc ctx k (ctx/create [k v] ctx)))
            ctx
            components)))

(defn- start-game! [ctx tiled-level]
  (let [ctx (merge (reset-common-game-context! ctx)
                   {:context/replay-mode? false}
                   (world/->context ctx tiled-level))]
    ;(ctx/clear-recorded-txs!)
    ;(ctx/set-record-txs! true) ; TODO set in config ? ignores option menu setting and sets true always.
    (world/transact-create-entities-from-tiledmap! ctx)
    ;(println "Initial entity txs:")
    ;(ctx/summarize-txs (ctx/frame->txs ctx 0))
    (merge ctx (->player-entity-context ctx))))

(defn- start-replay-mode! [ctx]
  (.setInputProcessor com.badlogic.gdx.Gdx/input nil)
  (ctx/set-record-txs! false)
  ; remove entity connections to world grid/content-grid,
  ; otherwise all entities removed with reset-common-game-context!
  (ctx/transact-all! ctx (for [e (api.context/all-entities ctx)] [:tx/destroy e]))
  (ctx/remove-destroyed-entities! ctx)
  (let [ctx (reset-common-game-context! ctx)] ; without replay-mode / world ... make it explicit we re-use this here ? assign ?
    ; world visibility is not reset ... ...
    (ctx/transact-all! ctx (ctx/frame->txs ctx 0))
    (reset! app.state/current-context
            (merge ctx
                   (->player-entity-context ctx)
                   {:context/replay-mode? true}))))

; for now a function, see context.libgdx.input reload bug
; otherwise keys in dev mode may be unbound because dependency order not reflected
; because bind-roots
(defn- hotkey->window [{:keys [context/config] :as ctx}]
  (merge
   {input.keys/i :inventory-window
    input.keys/e :entity-info-window}
   (when (safe-get config :debug-window?)
     {input.keys/z :debug-window})))

(defn- check-window-hotkeys [ctx]
  (doseq [[hotkey window] (hotkey->window ctx)
          :when (key-just-pressed? ctx hotkey)]
    (toggle-visible! (ctx/get-window ctx window))))

(defn- adjust-zoom [camera by] ; DRY map editor
  (camera/set-zoom! camera (max 0.1 (+ (camera/zoom camera) by))))

(def ^:private zoom-speed 0.05)

(defn- end-of-frame-checks! [context]
  (when (key-pressed? context input.keys/minus)
    (adjust-zoom (ctx/world-camera context) zoom-speed))
  (when (key-pressed? context input.keys/equals)
    (adjust-zoom (ctx/world-camera context) (- zoom-speed)))
  (check-window-hotkeys context)
  (when (key-just-pressed? context input.keys/escape)
    (let [windows (windows context)]
      (cond (some visible? windows) (run! #(set-visible! % false) windows)
            :else (change-screen! :screens/options-menu))))
  (when (key-just-pressed? context input.keys/tab)
    (change-screen! :screens/minimap)))

(defn- render-game [{:keys [context/player-entity] g :context.libgdx/graphics :as context}
                    active-entities*]
  (camera/set-position! (ctx/world-camera context)
                        (:entity/position @player-entity))
  (render-map context)
  (g/render-world-view g
                       (fn [g]
                         (debug-render-before-entities context g)
                         (render-entities! context
                                           g
                                           ; TODO lazy seqS everywhere!
                                           (->> active-entities*
                                                (filter :entity/z-order)
                                                (filter #(line-of-sight? context @player-entity %))))
                         (debug-render-after-entities context g))))

(def ^:private pausing? true)

(defn- player-unpaused? [ctx]
  (or (key-just-pressed? ctx input.keys/p)
      (key-pressed?      ctx input.keys/space)))

(defn- assoc-delta-time [ctx]
  (assoc ctx :context/delta-time (min (delta-time ctx) movement/max-delta-time)))

(defn- update-game [{:keys [context/player-entity
                            context/game-paused
                            context/thrown-error
                            context/game-logic-frame]
                     :as ctx}
                    active-entities]
  (let [state-obj (entity/state-obj @player-entity)
        _ (transact-all! ctx (state/manual-tick state-obj @player-entity ctx))
        paused? (reset! game-paused (or @thrown-error
                                        (and pausing?
                                             (state/pause-game? (entity/state-obj @player-entity))
                                             (not (player-unpaused? ctx)))))
        ctx (assoc-delta-time ctx)]
    (update-mouseover-entity! ctx) ; this do always so can get debug info even when game not running
    (when-not paused?
      (swap! game-logic-frame inc)
      (update-elapsed-game-time! ctx)
      (update-potential-fields! ctx active-entities)
      (tick-entities! ctx (map deref active-entities))) ; TODO lazy seqs everywhere!
    (remove-destroyed-entities! ctx) ; do not pause this as for example pickup item, should be destroyed.
    (end-of-frame-checks! ctx)))

(defn- replay-frame! [ctx frame-number]
  (update-mouseover-entity! ctx)
  (update-elapsed-game-time! (assoc-delta-time ctx))
  (let [txs (frame->txs ctx frame-number)]
    ;(println frame-number ". " (count txs))
    (transact-all! ctx txs))
  (end-of-frame-checks! ctx))

; TODO adjust sound speed also equally ? pitch ?
(def ^:private replay-speed 2)

(defn- replay-game! [{:keys [context/game-logic-frame] :as ctx}]
  (dotimes [_ replay-speed]
    (replay-frame! ctx (swap! game-logic-frame inc))))

(component/def :context/game {}
  components
  (ctx/create [_ _ctx] components))

(extend-type api.context.Context
  api.context/Game
  (start-new-game [ctx tiled-level]
    (start-game! ctx tiled-level))

  (render-game [{:keys [context/player-entity
                        context/replay-mode?] :as context}]
    (let [active-entities (active-entities (content-grid context) player-entity)]
      ; TODO lazy seqS everywhere!
      (render-game context (map deref active-entities))
      (if replay-mode?
        (replay-game! context)
        (update-game context active-entities)))))

(comment

 ; explored-tiles? (TODO)
 ; player message, player modals, etc. all game related state handle ....
 ; game timer is not reset  - continues as if
 ; entities all disappearing, just stop when end reached ....
 ; check other atoms , try to remove atoms ...... !?

 ; replay mode no window hotkeys working
 ; buttons working
 ; can remove items from inventory ! changes cursor but does not change back ..
 ; => deactivate all input somehow (set input processor nil ?)
 ; works but ESC is separate from main input processor and on re-entry
 ; again stage is input-processor
 ; also cursor is from previous game replay
 ; => all hotkeys etc part of stage input processor make.
 ; set nil for non idle/item in hand states .

 ; for some reason he calls end of frame checks but cannot open windows with hotkeys

 (.postRunnable com.badlogic.gdx.Gdx/app
                (fn []
                  (start-replay-mode!
                   @app.state/current-context)))

 )