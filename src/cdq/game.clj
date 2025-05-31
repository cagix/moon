(ns cdq.game
  (:require [cdq.resizable]
            [cdq.g :as g]
            [cdq.g.info]
            [cdq.g.player-movement-vector]
            [cdq.g.interaction-state]
            [cdq.g.handle-txs]
            [cdq.ui.editor]
            [cdq.ui.editor.overview-table]
            [cdq.malli :as m]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [gdl.application :as application]
            [gdl.utils :as utils]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.utils Disposable)))

(q/defrecord Context [ctx/assets
                      ctx/batch
                      ctx/config
                      ctx/cursors
                      ctx/db
                      ctx/default-font
                      ctx/graphics
                      ctx/input
                      ctx/stage
                      ctx/ui-viewport
                      ctx/unit-scale
                      ctx/shape-drawer
                      ctx/shape-drawer-texture
                      ctx/tiled-map-renderer
                      ctx/world-unit-scale
                      ctx/world-viewport])

(def ^:private schema
  (m/schema [:map {:closed true}
             [:ctx/assets :some]
             [:ctx/batch :some]
             [:ctx/config :some]
             [:ctx/cursors :some]
             [:ctx/db :some]
             [:ctx/default-font :some]
             [:ctx/graphics :some]
             [:ctx/input :some]
             [:ctx/stage :some]
             [:ctx/ui-viewport :some]
             [:ctx/unit-scale :some]
             [:ctx/shape-drawer :some]
             [:ctx/shape-drawer-texture :some]
             [:ctx/tiled-map-renderer :some]
             [:ctx/world-unit-scale :some]
             [:ctx/world-viewport :some]
             [:ctx/elapsed-time :some]
             [:ctx/delta-time {:optional true} number?]
             [:ctx/paused? {:optional true} :boolean]
             [:ctx/tiled-map :some]
             [:ctx/grid :some]
             [:ctx/raycaster :some]
             [:ctx/content-grid :some]
             [:ctx/explored-tile-corners :some]
             [:ctx/id-counter :some]
             [:ctx/entity-ids :some]
             [:ctx/potential-field-cache :some]
             [:ctx/factions-iterations :some]
             [:ctx/z-orders :some]
             [:ctx/render-z-order :some]
             [:ctx/mouseover-eid {:optional true} :any]
             [:ctx/player-eid :some]
             [:ctx/active-entities {:optional true} :some]]))

; We _need to create stepwise because otherwise the file becomes too big ... _
(defn- create! [config]
  (let [ctx (map->Context {:config config})
        ctx (reduce (fn [ctx f]
                      (f ctx))
                    ctx
                    (map requiring-resolve
                         '[cdq.create.assets/do!
                           cdq.create.input/do!
                           cdq.create.db/do!
                           cdq.create.graphics/do!
                           cdq.create.stage/do!
                           cdq.create.game-state/do!]))]
    (m/validate-humanize schema ctx)
    ctx))

(defn- dispose! [{:keys [ctx/assets
                         ctx/batch
                         ctx/cursors
                         ctx/default-font
                         ctx/shape-drawer-texture]}]
  (Disposable/.dispose assets)
  (Disposable/.dispose batch)
  (run! Disposable/.dispose (vals cursors))
  (Disposable/.dispose default-font)
  (Disposable/.dispose shape-drawer-texture)
  ; TODO vis-ui dispose
  ; TODO dispose world tiled-map/level resources?
  )

(def render-fns
  '[
    cdq.render.assoc-active-entities/do!
    cdq.render.set-camera-on-player/do!
    cdq.render.clear-screen/do!
    cdq.render.draw-world-map/do!
    cdq.render.draw-on-world-viewport/do!
    cdq.g/render-stage!
    cdq.render.player-state-handle-click/do!
    cdq.render.update-mouseover-entity/do!
    cdq.render.assoc-paused/do!
    cdq.render.update-time/do!
    cdq.render.update-potential-fields/do!
    cdq.render.tick-entities/do!
    cdq.render.remove-destroyed-entities/do!
    cdq.render.camera-controls/do!
    ])

(defn- render! [ctx]
  (m/validate-humanize schema ctx)
  (let [render-fns (map requiring-resolve render-fns)
        ctx (reduce (fn [ctx render!]
                      (render! ctx))
                    ctx
                    render-fns)]
    (m/validate-humanize schema ctx)
    ctx))

(defn- resize! [{:keys [ctx/ui-viewport
                        ctx/world-viewport]}
                width
                height]
  (cdq.resizable/resize! ui-viewport    width height)
  (cdq.resizable/resize! world-viewport width height))

(defn -main [config-path]
  (let [config (utils/create-config config-path)]
    (lwjgl/application (:lwjgl-application config)
                       (proxy [ApplicationAdapter] []
                         (create []
                           (reset! application/state (create! config)))

                         (dispose []
                           (dispose! @application/state))

                         (render []
                           (swap! application/state render!))

                         (resize [width height]
                           (resize! @application/state width height))))))

(extend-type Context
  g/InfoText
  (info-text [ctx object]
    (cdq.g.info/text ctx object)))

(extend-type Context
  g/PlayerMovementInput
  (player-movement-vector [ctx]
    (cdq.g.player-movement-vector/WASD-movement-vector ctx)))

(extend-type Context
  g/InteractionState
  (interaction-state [ctx eid]
    (cdq.g.interaction-state/create ctx eid)))

(extend-type Context
  g/EffectHandler
  (handle-txs! [ctx transactions]
    (doseq [transaction transactions
            :when transaction
            :let [_ (assert (vector? transaction)
                            (pr-str transaction))
                  ; TODO also should be with namespace 'tx' the first is a keyword
                  ]]
      (try (cdq.g.handle-txs/handle-tx! transaction ctx)
           (catch Throwable t
             (throw (ex-info "" {:transaction transaction} t)))))))

(extend-type Context
  g/EditorWindow
  (open-editor-window! [ctx property-type]
    (cdq.ui.editor/open-editor-window! ctx property-type))

  (edit-property! [ctx property]
    (g/add-actor! ctx (cdq.ui.editor/editor-window property ctx)))

  (property-overview-table [ctx property-type clicked-id-fn]
    (cdq.ui.editor.overview-table/create ctx property-type clicked-id-fn)))
